package org.aya.anqur.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.anqur.tyck.Normalizer;
import org.aya.anqur.util.Distiller;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface Term extends Docile {
  @Override default @NotNull Doc toDoc() {
    return Distiller.term(this, Distiller.Prec.Free);
  }
  default @NotNull Term subst(@NotNull LocalVar x, @NotNull Term t) {
    return subst(MutableMap.of(x, t));
  }
  default @NotNull Term subst(@NotNull MutableMap<LocalVar, Term> map) {
    return new Normalizer(MutableMap.create(), map).term(this);
  }

  record Ref(@NotNull LocalVar var) implements Term {}
  record Call(@NotNull DefVar<Def.Fn<Term>> fn, @NotNull ImmutableSeq<Term> args) implements Term {}
  record Two(boolean isApp, @NotNull Term f, @NotNull Term a) implements Term {
    @Override public @NotNull Term proj(boolean isOne) {return isOne ? f : a;}
  }
  record Proj(@NotNull Term t, boolean isOne) implements Term {}
  record Lam(@NotNull LocalVar x, @NotNull Term body) implements Term {}
  static @Nullable Term unlam(MutableList<LocalVar> binds, Term t, int n) {
    if (n == 0) return t;
    if (t instanceof Lam lam) {
      binds.append(lam.x);
      return unlam(binds, lam.body, n - 1);
    } else return null;
  }

  static @NotNull Term mkLam(@NotNull SeqView<LocalVar> telescope, @NotNull Term body) {
    return telescope.foldRight(body, Lam::new);
  }
  default @NotNull Term app(@NotNull Term... args) {
    var f = this;
    for (var a : args) f = f instanceof Lam lam ? lam.body.subst(lam.x, a) : new Two(true, f, a);
    return f;
  }
  default @NotNull Term proj(boolean isOne) {return new Proj(this, isOne);}

  record DT(boolean isPi, @NotNull Param<Term> param, @NotNull Term cod) implements Term {
    public @NotNull Term codomain(@NotNull Term term) {
      return cod.subst(param.x(), term);
    }
  }

  static @NotNull Term mkPi(@NotNull ImmutableSeq<Param<Term>> telescope, @NotNull Term body) {
    return telescope.view().foldRight(body, (param, term) -> new DT(true, param, term));
  }
  static @NotNull Term mkPi(@NotNull Term dom, @NotNull Term cod) {
    return new Term.DT(true, new Param<>(new LocalVar("_"), dom), cod);
  }
  @NotNull Term U = new UI(Keyword.U);
  record UI(@NotNull Keyword keyword) implements Term {}
}
