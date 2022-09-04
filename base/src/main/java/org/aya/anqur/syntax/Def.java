package org.aya.anqur.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

public sealed interface Def<T extends Docile> {
  @NotNull ImmutableSeq<Param<T>> telescope();
  @NotNull DefVar<? extends Def<Term>> name();
  @NotNull T result();
  default @NotNull SeqView<LocalVar> teleVars() {
    return telescope().view().map(Param::x);
  }
  default @NotNull SeqView<Term> teleRefs() {
    return teleVars().map(Term.Ref::new);
  }

  record Fn<T extends Docile>(
    @Override @NotNull DefVar<Fn<Term>> name,
    @Override @NotNull ImmutableSeq<Param<T>> telescope,
    @NotNull T result,
    @NotNull T body
  ) implements Def<T> {}

  record Data<T extends Docile>(
    @Override @NotNull DefVar<Data<Term>> name,
    @Override @NotNull ImmutableSeq<Param<T>> telescope,
    @NotNull T result,
    @NotNull ImmutableSeq<Cons<T>> cons
  ) implements Def<T> {}

  record Cons<T extends Docile>(
    @Override @NotNull DefVar<Cons<Term>> name,
    @NotNull Data<T> owner,
    @Override @NotNull ImmutableSeq<Param<T>> tele
  ) implements Def<T> {
    @Override
    public @NotNull ImmutableSeq<Param<T>> telescope() {
      return tele.view().concat(owner.telescope().view()).toImmutableSeq();
    }

    /** Invoke only when T = Term */
    @SuppressWarnings("unchecked") @Override public @NotNull T result() {
      // TODO: indexed types
      return (T) new Term.DataCall(owner.name, owner.teleRefs().toImmutableSeq());
    }
  }

  record Print<T extends Docile>(
    @Override @NotNull ImmutableSeq<Param<T>> telescope,
    @NotNull T result,
    @NotNull T body
  ) implements Def<T> {
    @Override public @NotNull DefVar<? extends Print<Term>> name() {
      throw new UnsupportedOperationException();
    }
  }
}
