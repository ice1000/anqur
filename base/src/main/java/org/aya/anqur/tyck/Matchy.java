package org.aya.anqur.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.syntax.Expr;
import org.aya.anqur.syntax.Pat;
import org.aya.anqur.syntax.Term;
import org.aya.anqur.util.Param;
import org.jetbrains.annotations.NotNull;

public record Matchy(@NotNull Elaborator elaborator) {
  public @NotNull Pat.Clause<Term> clause(
    @NotNull ImmutableSeq<Param<Term>> params,
    @NotNull Term result,
    @NotNull Pat.Clause<Expr> clause
  ) {
    pats(params, clause.pats());
    return new Pat.Clause<>(clause.pats(), elaborator.inherit(clause.body(), result));
  }
  public void pat(@NotNull Pat pat, @NotNull Term type) {
    switch (pat) {
      case Pat.Bind bind -> elaborator.gamma().put(bind.bind(), type);
      case Pat.Con con && type instanceof Term.DataCall data -> {
        var core = con.ref().core;
        var expected = core.owner();
        if (data.fn() != expected) throw new RuntimeException("Expected " + data.fn().name + ", got " + expected.name);
        pats(core.tele(), con.pats());
      }
      case Pat.Con con -> throw new RuntimeException(
        "So " + con.ref().name + " is not a constructor for type " + type.toDoc().commonRender());
    }
  }

  private void pats(ImmutableSeq<Param<Term>> tele, ImmutableSeq<Pat> pats) {
    tele.zipView(pats).forEach(pair -> pat(pair._2, pair._1.type()));
  }
}
