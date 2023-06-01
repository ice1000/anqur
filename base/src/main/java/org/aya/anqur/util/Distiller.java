package org.aya.anqur.util;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.anqur.syntax.DefVar;
import org.aya.anqur.syntax.Expr;
import org.aya.anqur.syntax.Term;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.Arg;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

import static org.aya.anqur.util.Distiller.Prec.*;

public interface Distiller {
  @FunctionalInterface
  interface PP<E> extends BiFunction<E, Prec, Doc> {}
  enum Prec {
    Free, Cod, AppHead, AppSpine, ProjHead
  }
  static @NotNull Doc expr(@NotNull Expr expr, Prec envPrec) {
    return switch (expr) {
      case Expr.PrimTy u -> Doc.plain(u.keyword().name());
      case Expr.Two two when two.isApp() -> {
        var inner = Doc.sep(expr(two.f(), AppHead), expr(two.a(), AppSpine));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(inner) : inner;
      }
      case Expr.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(expr(two.f(), Free), expr(two.a(), Free))));
      case Expr.Lam lam -> {
        var doc = Doc.sep(Doc.cat(Doc.plain("\\"),
          Doc.symbol(lam.x().name()), Doc.plain(".")), expr(lam.a(), Free));
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.Resolved resolved -> Doc.plain(resolved.ref().name());
      case Expr.Unresolved unresolved -> Doc.plain(unresolved.name());
      case Expr.Proj proj -> Doc.cat(expr(proj.t(), ProjHead), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Expr.DT dt -> {
        var doc = dependentType(dt.isPi(), dt.param(), expr(dt.cod(), Cod));
        yield envPrec.ordinal() > Cod.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.Hole ignored -> Doc.symbol("_");
    };
  }
  private static @NotNull Doc dependentType(boolean isPi, Param<?> param, Docile cod) {
    return Doc.sep(Doc.plain(isPi ? "Pi" : "Sig"),
      param.toDoc(), Doc.symbol(isPi ? "->" : "**"), cod.toDoc());
  }

  static @NotNull Doc term(@NotNull Term term, Prec envPrec) {
    return switch (term) {
      case Term.DT dt -> {
        var doc = dependentType(dt.isPi(), dt.param(), term(dt.cod(), Cod));
        yield envPrec.ordinal() > Cod.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.UI ui -> Doc.plain(ui.keyword().name());
      case Term.Ref ref -> Doc.plain(ref.var().name());
      case Term.Lam lam -> {
        var doc = Doc.sep(Doc.cat(Doc.plain("\\"),
          Doc.symbol(lam.x().name()), Doc.plain(".")), term(lam.body(), Free));
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Proj proj -> Doc.cat(term(proj.t(), ProjHead), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Term.Two two when two.isApp() -> {
        var inner = Doc.sep(term(two.f(), AppHead), term(two.a(), AppSpine));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(inner) : inner;
      }
      case Term.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(term(two.f(), Free), term(two.a(), Free))));
      case Term.FnCall fnCall -> call(envPrec, fnCall.args().view(), fnCall.fn());
      case Term.ConCall conCall -> call(envPrec, conCall.args().view(), conCall.fn());
      case Term.DataCall dataCall -> call(envPrec, dataCall.args().view(), dataCall.fn());
      case Term.Error(var msg) -> Doc.plain(msg);
    };
  }
  private static @NotNull Doc call(Prec envPrec, SeqView<Term> args, DefVar<?> name) {
    var doc = Doc.sep(args
      .map(t -> term(t, AppSpine)).prepended(Doc.plain(name.name)));
    if (args.isEmpty()) return doc;
    return envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
  }

  static Doc args(ImmutableSeq<Arg<Term>> args) {
    return Doc.wrap("(", ")", Doc.commaList(args.map(
      arg -> term(arg.term(), Free))));
  }
}
