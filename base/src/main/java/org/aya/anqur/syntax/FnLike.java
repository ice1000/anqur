package org.aya.anqur.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.jetbrains.annotations.NotNull;

public interface FnLike {
  @NotNull ImmutableSeq<Param<Term>> telescope();
  @NotNull Term result();
  default @NotNull SeqView<LocalVar> teleVars() {
    return telescope().view().map(Param::x);
  }
  default @NotNull SeqView<Term> teleRefs() {
    return teleVars().map(Term.Ref::new);
  }
}
