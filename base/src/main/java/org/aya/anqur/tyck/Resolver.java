package org.aya.anqur.tyck;

import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableMap;
import kala.control.Either;
import kala.control.Option;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.anqur.syntax.*;
import org.aya.anqur.util.AnyVar;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.anqur.util.SPE;
import org.aya.pretty.doc.Doc;
import org.jetbrains.annotations.NotNull;

public record Resolver(@NotNull MutableMap<String, AnyVar> env) {
  private @NotNull TeleCache mkCache(int initialCapacity) {
    return new TeleCache(this, MutableArrayList.create(initialCapacity), MutableArrayList.create(initialCapacity));
  }

  private record TeleCache(Resolver ctx, MutableArrayList<AnyVar> recover, MutableArrayList<AnyVar> remove) {
    private void add(@NotNull AnyVar var) {
      var put = ctx.put(var);
      if (put.isDefined()) recover.append(put.get());
      else remove.append(var);
    }

    private void purge() {
      remove.forEach(key -> ctx.env.remove(key.name()));
      recover.forEach(ctx::put);
    }
  }

  public @NotNull Param<Expr> param(@NotNull Param<Expr> param) {
    return new Param<>(param.x(), expr(param.type()));
  }

  public @NotNull Decl def(@NotNull Decl def) {
    return switch (def) {
      case Decl.Fn fn -> {
        var tele = tele(def);
        put(fn.name());
        var result = expr(fn.result());
        // For javac type inference
        var resolved = new Decl.Fn(fn.name(), tele._1, result, fn.body().map(
          this::expr, clauses ->
            Either.left(new Pat.ClauseSet<>(clauses.getRightValue().map(this::clause)))));
        tele._2.purge();
        yield resolved;
      }
      case Decl.Print print -> {
        var tele = tele(def);
        var body = expr(print.body());
        var result = expr(print.result());
        tele._2.purge();
        yield new Decl.Print(tele._1, result, body);
      }
      case Decl.Cons cons -> cons(cons);
      case Decl.Data data -> {
        var tele = tele(def);
        put(def.name());
        var cons = data.cons().map(this::cons);
        tele._2.purge();
        yield new Decl.Data(data.name(), tele._1, cons);
      }
    };
  }

  private @NotNull Decl.Cons cons(Decl.Cons cons) {
    var tele = tele(cons);
    tele._2.purge();
    put(cons.name());
    return new Decl.Cons(cons.name(), tele._1);
  }

  @NotNull private Tuple2<Decl.Tele, TeleCache> tele(Decl def) {
    var size = def.tele().scope().size();
    var telescope = MutableArrayList.<Param<Expr>>create(size);
    var cache = mkCache(size);
    for (var param : def.tele().scope()) {
      telescope.append(new Param<>(param.x(), expr(param.type())));
      cache.add(param.x());
    }
    return Tuple.of(new Decl.Tele(telescope.toImmutableArray()), cache);
  }

  public @NotNull Expr expr(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.DT dt -> new Expr.DT(dt.isPi(), dt.pos(), param(dt.param()), bodied(param(dt.param()).x(), dt.cod()));
      case Expr.Two two -> new Expr.Two(two.isApp(), two.pos(), expr(two.f()), expr(two.a()));
      case Expr.Lam lam -> new Expr.Lam(lam.pos(), lam.x(), bodied(lam.x(), lam.a()));
      case Expr.PrimTy primTy -> primTy;
      case Expr.Hole hole -> new Expr.Hole(hole.pos(),
        env.valuesView().filterIsInstance(LocalVar.class).toImmutableSeq());
      case Expr.Unresolved unresolved -> env.getOption(unresolved.name())
        .map(x -> new Expr.Resolved(unresolved.pos(), x))
        .getOrThrow(() -> new SPE(unresolved.pos(), Doc.english("Unresolved: " + unresolved.name())));
      case Expr.Resolved resolved -> resolved;
      case Expr.Proj proj -> new Expr.Proj(proj.pos(), expr(proj.t()), proj.isOne());
    };
  }

  private @NotNull Expr bodied(LocalVar x, Expr expr) {
    var old = put(x);
    var e = expr(expr);
    old.map(this::put).getOrElse(() -> env.remove(x.name()));
    return e;
  }

  private @NotNull Option<AnyVar> put(AnyVar x) {
    return env.put(x.name(), x);
  }

  public @NotNull Pat.Clause<Expr> clause(@NotNull Pat.UnresolvedClause c) {
    var tele = mkCache(16);
    var pats = c.unsols().map(pat -> pattern(pat, tele));
    var body = expr(c.body());
    tele.purge();
    return new Pat.Clause<>(pats, body);
  }

  @SuppressWarnings("unchecked") private static @NotNull Pat pattern(@NotNull Pat.Unresolved u, @NotNull TeleCache cache) {
    var var = cache.ctx.env.getOrNull(u.name());
    if (var == null && u.pats().isEmpty()) {
      var v = new LocalVar(u.name());
      cache.add(v);
      return new Pat.Bind(v);
    }
    if (!(var instanceof DefVar<?> def)) throw new SPE(u.pos(), Doc.english("Not a def: " + u.name()));
    if (!(def.core instanceof Def.Cons cons)) throw new SPE(u.pos(), Doc.english("Not a cons: " + def.name));
    if (!cons.tele().sizeEquals(u.pats().size()))
      throw new SPE(u.pos(), Doc.english("Wrong number of arguments: " + u.name()));
    var pats = u.pats().map(p -> pattern(p, cache));
    return new Pat.Con((DefVar<Def.Cons>) def, pats);
  }
}
