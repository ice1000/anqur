package org.aya.anqur.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.aya.anqur.util.Param;
import org.jetbrains.annotations.NotNull;

/**
 * Concrete syntax of global definitions.
 */
public sealed interface Decl {
  record Tele(@NotNull ImmutableSeq<Param<Expr>> scope) {
  }
  @NotNull DefVar<? extends Def> name();
  @NotNull Tele tele();
  record Fn(
    @Override @NotNull DefVar<Def.Fn> name,
    @Override @NotNull Tele tele,
    @NotNull Expr result,
    @NotNull Either<Expr, Either<Pat.ClauseSet<Expr>, ImmutableSeq<Pat.UnresolvedClause>>> body
  ) implements Decl {}
  record Print(
    @Override @NotNull Tele tele,
    @NotNull Expr result,
    @NotNull Expr body
  ) implements Decl {
    @Override public @NotNull DefVar<? extends Def> name() {
      throw new UnsupportedOperationException();
    }
  }
  record Data(
    @Override @NotNull DefVar<Def.Data> name,
    @Override @NotNull Tele tele,
    @NotNull ImmutableSeq<Cons> cons
  ) implements Decl {}
  record Cons(
    @Override @NotNull DefVar<Def.Cons> name,
    @Override @NotNull Tele tele
  ) implements Decl {}
}
