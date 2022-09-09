package org.aya.anqur.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.util.LocalVar;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Pat {
  record Unresolved(@NotNull SourcePos pos, @NotNull String name, @NotNull ImmutableSeq<Unresolved> pats) {}

  record Bind(@NotNull LocalVar bind) implements Pat {}
  record Con(@NotNull DefVar<Def.Cons> ref, @NotNull ImmutableSeq<Pat> pats) implements Pat {}

  record Clause<T>(@NotNull ImmutableSeq<Pat> pats, @NotNull T body) {}
  record ClauseSet<Term>(@NotNull ImmutableSeq<Clause<Term>> clauses) {}
  record UnresolvedClause(@NotNull ImmutableSeq<Pat.Unresolved> unsols, @NotNull Expr body) {}
}
