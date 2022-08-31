package org.aya.anqur.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.anqur.syntax.Def;
import org.aya.anqur.syntax.Expr;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.anqur.util.SPE;
import org.aya.pretty.doc.Doc;
import org.jetbrains.annotations.NotNull;

public record Resolver(@NotNull MutableMap<String, LocalVar> env) {
  private @NotNull TeleCache mkCache(int initialCapacity) {
    return new TeleCache(this, MutableArrayList.create(initialCapacity), MutableArrayList.create(initialCapacity));
  }

  private record TeleCache(Resolver ctx, MutableArrayList<LocalVar> recover, MutableArrayList<LocalVar> remove) {
    private void add(@NotNull LocalVar var) {
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

  public @NotNull Def<Expr> def(@NotNull Def<Expr> def) {
    var tele = tele(def);
    var result = expr(def.result());
    return switch (def) {
      case Def.Fn<Expr> fn -> {
        put(fn.name());
        var body = expr(fn.body());
        tele._2.purge();
        yield new Def.Fn<>(fn.name(), tele._1, result, body);
      }
      case Def.Print<Expr> print -> {
        var body = expr(print.body());
        tele._2.purge();
        yield new Def.Print<>(tele._1, result, body);
      }
    };
  }

  @NotNull private Tuple2<ImmutableSeq<Param<Expr>>, TeleCache> tele(Def<Expr> def) {
    var telescope = MutableArrayList.<Param<Expr>>create(def.telescope().size());
    var cache = mkCache(def.telescope().size());
    for (var param : def.telescope()) {
      telescope.append(new Param<>(param.x(), expr(param.type())));
      cache.add(param.x());
    }
    return Tuple.of(telescope.toImmutableArray(), cache);
  }

  public @NotNull Expr expr(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.DT dt -> new Expr.DT(dt.isPi(), dt.pos(), param(dt.param()), bodied(param(dt.param()).x(), dt.cod()));
      case Expr.Two two -> new Expr.Two(two.isApp(), two.pos(), expr(two.f()), expr(two.a()));
      case Expr.Lam lam -> new Expr.Lam(lam.pos(), lam.x(), bodied(lam.x(), lam.a()));
      case Expr.PrimTy primTy -> primTy;
      case Expr.Hole hole -> new Expr.Hole(hole.pos(), env.valuesView().toImmutableSeq());
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

  private @NotNull Option<LocalVar> put(LocalVar x) {
    return env.put(x.name(), x);
  }
}
