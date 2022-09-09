package org.aya.anqur.tyck;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
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
        if (!core.tele().sizeEquals(con.pats().size()))
          throw new RuntimeException("Wrong number of arguments: " + core.name().name);
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

  public static Option<Normalizer> buildSubst(SeqView<Pat> pat, SeqView<Term> term) {
    var subst = new Normalizer(MutableMap.create());
    if (buildSubst(pat, term, subst)) return Option.some(subst);
    else return Option.none();
  }

  private static boolean buildSubst(Pat pat, Term term, Normalizer subst) {
    return switch (pat) {
      case Pat.Bind bind -> {
        subst.rho().put(bind.bind(), term);
        yield true;
      }
      case Pat.Con con && term instanceof Term.ConCall call -> {
        if (con.ref() != con.ref()) yield false;
        yield buildSubst(con.pats().view(), call.args().view(), subst);
      }
      case default -> false;
    };
  }

  private static boolean buildSubst(SeqView<Pat> pats, SeqView<Term> args, Normalizer subst) {
    return pats.zipView(args).allMatch(pair -> buildSubst(pair._1, pair._2, subst));
  }

  public static Option<Term> unfold(Pat.ClauseSet<Term> clauses, ImmutableSeq<Term> args) {
    assert clauses.clauses().size() == args.size();
    for (var cls : clauses.clauses()) {
      var subst = buildSubst(cls.pats().view(), args.view());
      if (subst.isDefined()) return Option.some(subst.get().term(cls.body()));
    }
    return Option.none();
  }
}
