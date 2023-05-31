package org.aya.anqur.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.syntax.Term;
import org.aya.anqur.util.LocalVar;
import org.jetbrains.annotations.NotNull;

public class Unifier {
  public record FailureData(@NotNull Term l, @NotNull Term r) {}
  public FailureData data;

  boolean untyped(@NotNull Term l, @NotNull Term r) {
    if (l == r) return true;
    var happy = switch (l) {
      case Term.Lam lam && r instanceof Term.Lam ram -> untyped(lam.body(), rhs(ram.body(), ram.x(), lam.x()));
      case Term.Lam lam -> eta(r, lam);
      case Term ll && r instanceof Term.Lam ram -> eta(ll, ram);
      case Term.Ref lref && r instanceof Term.Ref rref -> lref.var() == rref.var();
      case Term.Two lapp && r instanceof Term.Two rapp ->
        lapp.isApp() == rapp.isApp() && untyped(lapp.f(), rapp.f()) && untyped(lapp.a(), rapp.a());
      case Term.DT ldt && r instanceof Term.DT rdt -> ldt.isPi() == rdt.isPi()
        && untyped(ldt.param().type(), rdt.param().type())
        && untyped(ldt.cod(), rhs(rdt.cod(), rdt.param().x(), ldt.param().x()));
      case Term.Proj lproj && r instanceof Term.Proj rproj ->
        lproj.isOne() == rproj.isOne() && untyped(lproj.t(), rproj.t());
      case Term.UI lu && r instanceof Term.UI ru -> lu.keyword() == ru.keyword();
      case Term.FnCall lcall && r instanceof Term.FnCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      case Term.DataCall lcall && r instanceof Term.DataCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      // We probably won't need to compare dataArgs cus the two sides of conversion should be of the same type
      case Term.ConCall lcall && r instanceof Term.ConCall rcall -> lcall.fn() == rcall.fn()
        && unifySeq(lcall.args(), rcall.args());
      default -> false;
    };
    if (!happy && data == null)
      data = new FailureData(l, r);
    return happy;
  }

  private boolean unifySeq(@NotNull ImmutableSeq<Term> l, @NotNull ImmutableSeq<Term> r) {
    return l.allMatchWith(r, this::untyped);
  }

  private boolean eta(@NotNull Term r, Term.Lam lam) {
    return untyped(lam.body(), r.app(new Term.Ref(lam.x())));
  }

  private static @NotNull Term rhs(Term rhs, LocalVar rb, LocalVar lb) {
    return rhs.subst(rb, new Term.Ref(lb));
  }
}
