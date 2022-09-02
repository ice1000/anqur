package org.aya.anqur.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.util.AnyVar;
import org.aya.anqur.util.Distiller;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr extends Docile {
  @NotNull SourcePos pos();
  @Override default @NotNull Doc toDoc() {
    return Distiller.expr(this, Distiller.Prec.Free);
  }
  record Unresolved(@Override @NotNull SourcePos pos, String name) implements Expr {}
  record Resolved(@Override @NotNull SourcePos pos, AnyVar ref) implements Expr {}
  /** @param isApp it's a tuple if false */
  record Two(boolean isApp, @Override @NotNull SourcePos pos, Expr f, Expr a) implements Expr {}
  record Lam(@Override @NotNull SourcePos pos, LocalVar x, Expr a) implements Expr {}

  /** @param isOne it's a second projection if false */
  record Proj(@Override @NotNull SourcePos pos, @NotNull Expr t, boolean isOne) implements Expr {}
  record PrimTy(@Override @NotNull SourcePos pos, @NotNull Keyword keyword) implements Expr {}
  record Hole(@Override @NotNull SourcePos pos, ImmutableSeq<LocalVar> accessible) implements Expr {}
  /** @param isPi it's a sigma if false */
  record DT(boolean isPi, @Override @NotNull SourcePos pos, Param<Expr> param, Expr cod) implements Expr {}
}
