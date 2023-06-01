package org.aya.anqur.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.anqur.syntax.*;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.anqur.util.SPE;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public record Elaborator(
  @NotNull MutableMap<DefVar<?>, Def> sigma,
  @NotNull MutableMap<LocalVar, Term> gamma
) {
  @NotNull public Term normalize(@NotNull Term term) {
    return term.subst(MutableMap.create());
  }

  public record Synth(@NotNull Term wellTyped, @NotNull Term type) {}

  public Term inherit(Expr expr, Term type) {
    return switch (expr) {
      case Expr.Lam lam -> {
        if (normalize(type) instanceof Term.DT dt && dt.isPi())
          yield new Term.Lam(lam.x(), hof(lam.x(), dt.param().type(), () ->
            inherit(lam.a(), dt.codomain(new Term.Ref(lam.x())))));
        else throw new SPE(lam.pos(),
          Doc.english("Expects a right adjoint for"), expr, Doc.plain("got"), type);
      }
      case Expr.Two two when !two.isApp() -> {
        if (!(normalize(type) instanceof Term.DT dt) || dt.isPi()) throw new SPE(two.pos(),
          Doc.english("Expects a left adjoint for"), expr, Doc.plain("got"), type);
        var lhs = inherit(two.f(), dt.param().type());
        yield new Term.Two(false, lhs, inherit(two.a(), dt.codomain(lhs)));
      }
      case Expr.Hole hole -> {
        var docs = MutableList.<Doc>create();
        gamma.forEach((k, v) -> {
          var list = MutableList.of(Doc.plain(k.name()));
          if (!hole.accessible().contains(k)) list.append(Doc.english("(out of scope)"));
          list.appendAll(new Doc[]{Doc.symbol(":"), normalize(v).toDoc()});
          docs.append(Doc.sep(list));
        });
        docs.append(Doc.plain("----------------------------------"));
        var tyDoc = type.toDoc();
        docs.append(tyDoc);
        var normDoc = normalize(type).toDoc();
        if (!tyDoc.equals(normDoc)) {
          docs.append(Doc.symbol("|->"));
          docs.append(normDoc);
        }
        throw new SPE(hole.pos(), Doc.vcat(docs));
      }
      default -> {
        var synth = synth(expr);
        unify(normalize(type), synth.wellTyped, synth.type, expr.pos());
        yield synth.wellTyped;
      }
    };
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos) {
    unify(ty, actual, pos, u -> unifyDoc(ty, on, actual, u));
  }

  private static void unify(Term ty, Term actual, SourcePos pos, Function<Unifier, Doc> message) {
    var unifier = new Unifier();
    if (!unifier.untyped(actual, ty))
      throw new SPE(pos, message.apply(unifier));
  }

  private static @NotNull Doc unifyDoc(Docile ty, Docile on, Docile actual, Unifier unifier) {
    var line1 = Doc.sep(Doc.plain("Umm,"), ty.toDoc(), Doc.plain("!="),
      actual.toDoc(), Doc.english("on"), on.toDoc());
    if (unifier.data != null) {
      var line2 = Doc.sep(Doc.english("In particular,"),
        unifier.data.l().toDoc(), Doc.symbol("!="), unifier.data.r().toDoc());
      line1 = Doc.vcat(line1, line2);
    }
    return line1;
  }

  public Synth synth(Expr expr) {
    var synth = switch (expr) {
      case Expr.PrimTy u -> new Synth(new Term.UI(u.keyword()), Term.U);
      case Expr.Resolved resolved -> switch (resolved.ref()) {
        case DefVar<?> defv -> {
          var def = defv.core;
          if (def == null) {
            var sig = defv.signature;
            var pi = Term.mkPi(sig.telescope(), sig.result());
            var call = mkCall(defv, sig);
            yield new Synth(Normalizer.rename(Term.mkLam(
              sig.teleVars(), call)), pi);
          }
          var pi = Term.mkPi(def.telescope(), def.result());
          yield switch (def) {
            case Def.Fn fn -> new Synth(Normalizer.rename(Term.mkLam(
              fn.teleVars(), new Term.FnCall(fn.name(), fn.teleRefs().toImmutableSeq()))), pi);
            case Def.Print print -> throw new AssertionError("unreachable: " + print);
            case Def.Cons cons -> new Synth(Normalizer.rename(Term.mkLam(
              cons.teleVars(), new Term.ConCall(cons.name(),
                cons.tele().map(x -> new Term.Ref(x.x())),
                cons.owner().core.teleRefs().toImmutableSeq()))), pi);
            case Def.Data data -> new Synth(Normalizer.rename(Term.mkLam(
              data.teleVars(), new Term.DataCall(data.name(), data.teleRefs().toImmutableSeq()))), pi);
          };
        }
        case LocalVar loc -> new Synth(new Term.Ref(loc), gamma.get(loc));
      };
      case Expr.Proj proj -> {
        var t = synth(proj.t());
        if (!(t.type instanceof Term.DT dt) || dt.isPi())
          throw new SPE(proj.pos(), Doc.english("Expects a left adjoint, got"), t.type);
        var fst = t.wellTyped.proj(true);
        if (proj.isOne()) yield new Synth(fst, dt.param().type());
        yield new Synth(t.wellTyped.proj(false), dt.codomain(fst));
      }
      case Expr.Two two -> {
        var f = synth(two.f());
        if (two.isApp()) {
          if (!(f.type instanceof Term.DT dt) || !dt.isPi())
            throw new SPE(two.pos(), Doc.english("Expects pi, got"), f.type, Doc.plain("when checking"), two);
          var a = hof(dt.param().x(), dt.param().type(), () -> inherit(two.a(), dt.param().type()));
          yield new Synth(f.wellTyped.app(a), dt.codomain(a));
        } else {
          var a = synth(two.a());
          yield new Synth(new Term.Two(false, f.wellTyped, a.wellTyped),
            new Term.DT(false, new Param<>(new LocalVar("_"), f.type), a.type));
        }
      }
      case Expr.DT dt -> {
        var param = synth(dt.param().type());
        var x = dt.param().x();
        var cod = hof(x, param.wellTyped, () -> synth(dt.cod()));
        yield new Synth(new Term.DT(dt.isPi(), new Param<>(x, param.wellTyped), cod.wellTyped), cod.type);
      }
      default -> throw new SPE(expr.pos(), Doc.english("Synthesis failed for"), expr);
    };
    var type = normalize(synth.type);
    return new Synth(synth.wellTyped, type);
  }

  @SuppressWarnings("unchecked") private static Term mkCall(DefVar<?> defv, Def.Signature sig) {
    return sig.isData() ? new Term.DataCall((DefVar<Def.Data>) defv,
      sig.teleRefs().toImmutableSeq()) : new Term.FnCall((DefVar<Def.Fn>) defv,
      sig.teleRefs().toImmutableSeq());
  }

  private <T> T hof(@NotNull LocalVar x, @NotNull Term type, @NotNull Supplier<T> t) {
    gamma.put(x, type);
    var ok = t.get();
    gamma.remove(x);
    return ok;
  }

  public @NotNull Def def(@NotNull Decl def) {
    var telescope = telescope(def.tele());
    return switch (def) {
      case Decl.Fn fn -> {
        var result = inherit(fn.result(), Term.U);
        fn.name().signature = new Def.Signature(false, telescope, result);
        var body = fn.body().map(
          expr -> inherit(expr, result),
          clauses -> tyckFunBody(telescope, result, clauses.getLeftValue())
        );
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Fn(fn.name(), telescope, result, body);
      }
      case Decl.Print print -> {
        var result = inherit(print.result(), Term.U);
        var body = inherit(print.body(), result);
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Print(telescope, result, body);
      }
      case Decl.Cons ignored -> throw new IllegalArgumentException("unreachable");
      case Decl.Data data -> {
        var ref = data.name();
        ref.signature = new Def.Signature(true, telescope, Term.U);
        yield new Def.Data(ref, telescope, data.cons().map(c -> cons(ref, c)));
      }
    };
  }

  private Pat.ClauseSet<Term> tyckFunBody(
    ImmutableSeq<Param<Term>> telescope, Term result,
    Pat.ClauseSet<Expr> clauseSet
  ) {
    var clauses = new Pat.ClauseSet<>(clauseSet.clauses().map(c ->
      new Matchy(this).clause(telescope, result, c)));
    Classifier.classify(clauses, telescope);
    return clauses;
  }

  private Def.Cons cons(DefVar<Def.Data> ref, Decl.Cons c) {
    return new Def.Cons(c.name(), ref, telescope(c.tele()));
  }

  private @NotNull ImmutableSeq<Param<Term>> telescope(Decl.Tele tele) {
    var telescope = MutableArrayList.<Param<Term>>create(tele.scope().size());
    for (var param : tele.scope()) {
      var ty = inherit(param.type(), Term.U);
      telescope.append(new Param<>(param.x(), ty));
      gamma.put(param.x(), ty);
    }
    return telescope.toImmutableArray();
  }
}
