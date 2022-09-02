package org.aya.anqur.cli;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.aya.anqur.parser.AnqurParser;
import org.aya.anqur.syntax.Def;
import org.aya.anqur.syntax.DefVar;
import org.aya.anqur.syntax.Expr;
import org.aya.anqur.syntax.Keyword;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.repl.antlr.AntlrUtil;
import org.aya.util.error.SourceFile;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Parser(@NotNull SourceFile source) {
  public @NotNull Expr expr(@NotNull AnqurParser.ExprContext expr) {
    return switch (expr) {
      case AnqurParser.ParenContext paren -> expr(paren.expr());
      case AnqurParser.TwoContext app -> new Expr.Two(true, sourcePosOf(app), expr(app.expr(0)), expr(app.expr(1)));
      case AnqurParser.PairContext p -> new Expr.Two(false, sourcePosOf(p), expr(p.expr(0)), expr(p.expr(1)));
      case AnqurParser.FstContext fst -> new Expr.Proj(sourcePosOf(fst), expr(fst.expr()), true);
      case AnqurParser.SndContext snd -> new Expr.Proj(sourcePosOf(snd), expr(snd.expr()), false);
      case AnqurParser.KeywordContext trebor -> {
        var pos = sourcePosOf(trebor);
        yield new Expr.PrimTy(pos, Keyword.U);
      }
      case AnqurParser.LamContext lam -> buildLam(sourcePosOf(lam), Seq.wrapJava(lam.ID()).view()
        .map(id -> new WithPos<>(AntlrUtil.sourcePosOf(id, source), new LocalVar(id.getText()))), expr(lam.expr()));
      case AnqurParser.RefContext ref -> new Expr.Unresolved(sourcePosOf(ref), ref.ID().getText());
      case AnqurParser.PiContext pi -> buildDT(true, sourcePosOf(pi), param(pi.param()), expr(pi.expr()));
      case AnqurParser.SigContext si -> buildDT(false, sourcePosOf(si), param(si.param()), expr(si.expr()));
      case AnqurParser.SimpFunContext pi -> new Expr.DT(true, sourcePosOf(pi), param(pi.expr(0)), expr(pi.expr(1)));
      case AnqurParser.SimpTupContext si -> new Expr.DT(false, sourcePosOf(si), param(si.expr(0)), expr(si.expr(1)));
      default -> throw new IllegalArgumentException("Unknown expr: " + expr.getClass().getName());
    };
  }

  @NotNull private ImmutableSeq<LocalVar> localVars(List<TerminalNode> ids) {
    return Seq.wrapJava(ids).map(id -> new LocalVar(id.getText()));
  }

  /*package*/
  @NotNull Def<Expr> def(@NotNull AnqurParser.DeclContext decl) {
    return switch (decl) {
      case AnqurParser.PrintDeclContext def -> new Def.Print<>(
        Seq.wrapJava(def.param()).flatMap(this::param),
        expr(def.expr(0)),
        expr(def.expr(1)));
      case AnqurParser.FnDeclContext def -> new Def.Fn<>(
        new DefVar<>(def.ID().getText()),
        Seq.wrapJava(def.param()).flatMap(this::param),
        expr(def.expr(0)),
        expr(def.expr(1)));
      default -> throw new IllegalArgumentException("Unknown def: " + decl.getClass().getName());
    };
  }

  private Expr buildDT(boolean isPi, SourcePos pos, SeqView<Param<Expr>> params, Expr body) {
    if (params.isEmpty()) return body;
    var drop = params.drop(1);
    return new Expr.DT(isPi, pos, params.first(), buildDT(isPi,
      AntlrUtil.sourcePosForSubExpr(source, drop.map(x -> x.type().pos()), body.pos()), drop, body));
  }

  private Expr buildLam(SourcePos pos, SeqView<WithPos<LocalVar>> params, Expr body) {
    if (params.isEmpty()) return body;
    var drop = params.drop(1);
    return new Expr.Lam(pos, params.first().data(), buildLam(
      AntlrUtil.sourcePosForSubExpr(source, drop.map(WithPos::sourcePos), body.pos()), drop, body));
  }

  private @NotNull Param<Expr> param(AnqurParser.ExprContext paramExpr) {
    return new Param<>(new LocalVar("_"), expr(paramExpr));
  }

  private SeqView<Param<Expr>> param(AnqurParser.ParamContext param) {
    var e = expr(param.expr());
    return ImmutableSeq.from(param.ID()).view()
      .map(id -> new Param<>(new LocalVar(id.getText()), e));
  }

  private @NotNull SourcePos sourcePosOf(ParserRuleContext ctx) {
    return AntlrUtil.sourcePosOf(ctx, source);
  }
}
