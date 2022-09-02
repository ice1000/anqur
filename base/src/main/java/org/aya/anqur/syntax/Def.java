package org.aya.anqur.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.util.Param;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

public sealed interface Def<T extends Docile> {
  @NotNull ImmutableSeq<Param<T>> telescope();
  @NotNull DefVar<? extends Def<Term>> name();
  @NotNull T result();

  record Fn<T extends Docile>(
    @Override @NotNull DefVar<Fn<Term>> name,
    @Override @NotNull ImmutableSeq<Param<T>> telescope,
    @NotNull T result,
    @NotNull T body
  ) implements Def<T> {}

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
