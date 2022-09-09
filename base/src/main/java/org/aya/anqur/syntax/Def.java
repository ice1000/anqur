package org.aya.anqur.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.util.Param;
import org.jetbrains.annotations.NotNull;

public sealed interface Def extends FnLike {
  @NotNull DefVar<? extends Def> name();

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

  /**
   * For (maybe mutually) recursive definitions, like types and functions
   *
   * @param isData it will be a function if false
   */
  record Signature(
    boolean isData,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result
  ) implements FnLike {
  }

  record Data(
    @Override @NotNull DefVar<Data> name,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull ImmutableSeq<Cons> cons
  ) implements Def {
    public Data {
      name.core = this;
    }

    @Override public @NotNull Term result() {
      return Term.U;
    }
  }

  record Cons(
    @Override @NotNull DefVar<Cons> name,
    @NotNull DefVar<Data> owner,
    @Override @NotNull ImmutableSeq<Param<Term>> tele
  ) implements Def {
    public Cons {
      name.core = this;
    }

    @Override public @NotNull ImmutableSeq<Param<Term>> telescope() {
      return owner.core.telescope().view().concat(tele.view()).toImmutableSeq();
    }

    /** Invoke only when T = Term */
    @Override public @NotNull Term result() {
      // TODO: indexed types
      return new Term.DataCall(owner, owner.core.teleRefs().toImmutableSeq());
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
