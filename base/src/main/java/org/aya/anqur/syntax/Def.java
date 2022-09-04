package org.aya.anqur.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.jetbrains.annotations.NotNull;

public sealed interface Def {
  @NotNull ImmutableSeq<Param<Term>> telescope();
  @NotNull DefVar<? extends Def> name();
  @NotNull Term result();
  default @NotNull SeqView<LocalVar> teleVars() {
    return telescope().view().map(Param::x);
  }
  default @NotNull SeqView<Term> teleRefs() {
    return teleVars().map(Term.Ref::new);
  }

  record Fn(
    @Override @NotNull DefVar<Fn> name,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def {
    public Fn {
      name.core = this;
    }
  }

  record Data(
    @Override @NotNull DefVar<Data> name,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull ImmutableSeq<Cons> cons
  ) implements Def {
    public Data {
      name.core = this;
    }
  }

  record Cons(
    @Override @NotNull DefVar<Cons> name,
    @NotNull Data owner,
    @Override @NotNull ImmutableSeq<Param<Term>> tele
  ) implements Def {
    public Cons {
      name.core = this;
    }

    @Override public @NotNull ImmutableSeq<Param<Term>> telescope() {
      return tele.view().concat(owner.telescope().view()).toImmutableSeq();
    }

    /** Invoke only when T = Term */
    @Override public @NotNull Term result() {
      // TODO: indexed types
      return new Term.DataCall(owner.name, owner.teleRefs().toImmutableSeq());
    }
  }

  record Print(
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def {
    @Override public @NotNull DefVar<? extends Print> name() {
      throw new UnsupportedOperationException();
    }
  }
}
