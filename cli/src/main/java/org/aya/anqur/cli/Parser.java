package org.aya.anqur.cli;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.control.Either;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.aya.anqur.parser.AnqurParser;
import org.aya.anqur.syntax.*;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
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
        .map(id -> new WithPos<>(sourcePosOf(id, source), new LocalVar(id.getText()))), expr(lam.expr()));
      case AnqurParser.RefContext ref -> new Expr.Unresolved(sourcePosOf(ref), ref.ID().getText());
      case AnqurParser.PiContext pi -> buildDT(true, sourcePosOf(pi), param(pi.param()), expr(pi.expr()));
      case AnqurParser.SigContext si -> buildDT(false, sourcePosOf(si), param(si.param()), expr(si.expr()));
      case AnqurParser.SimpFunContext pi -> new Expr.DT(true, sourcePosOf(pi), param(pi.expr(0)), expr(pi.expr(1)));
      case AnqurParser.SimpTupContext si -> new Expr.DT(false, sourcePosOf(si), param(si.expr(0)), expr(si.expr(1)));
      default -> throw new IllegalArgumentException("Unknown expr: " + expr.getClass().getName());
    };
  }

  /*package*/
  @NotNull Decl def(@NotNull AnqurParser.DeclContext decl) {
    return switch (decl) {
      case AnqurParser.PrintDeclContext def -> new Decl.Print(
        new Decl.Tele(Seq.wrapJava(def.param()).flatMap(this::param)),
        expr(def.expr(0)),
        expr(def.expr(1)));
      case AnqurParser.FnDeclContext def -> new Decl.Fn(
        new DefVar<>(def.ID().getText()),
        new Decl.Tele(Seq.wrapJava(def.param()).flatMap(this::param)),
        expr(def.expr()),
        fnBody(def.fnBody()));
      case AnqurParser.DataDeclContext def -> new Decl.Data(
        new DefVar<>(def.ID().getText()),
        new Decl.Tele(Seq.wrapJava(def.param()).flatMap(this::param)),
        Seq.wrapJava(def.consDecl()).map(this::cons));
      default -> throw new IllegalArgumentException("Unknown def: " + decl.getClass().getName());
    };
  }

  private <U> Either<Expr, Either<U, ImmutableSeq<Pat.UnresolvedClause>>> fnBody(AnqurParser.FnBodyContext fnBody) {
    var expr = fnBody.expr();
    if (expr != null) return Either.left(expr(expr));
    return Either.right(Either.right(
      Seq.wrapJava(fnBody.clause()).map(this::clause)));
  }

  private Pat.UnresolvedClause clause(AnqurParser.ClauseContext cls) {
    return new Pat.UnresolvedClause(pats(cls.pattern()), expr(cls.expr()));
  }

  private ImmutableSeq<Pat.Unresolved> pats(List<AnqurParser.PatternContext> patterns) {
    return Seq.wrapJava(patterns).map(this::pat);
  }

  private Pat.Unresolved pat(AnqurParser.PatternContext pat) {
    return new Pat.Unresolved(sourcePosOf(pat), pat.ID().getText(), pats(pat.pattern()));
  }

  private Decl.Cons cons(AnqurParser.ConsDeclContext consDeclContext) {
    return new Decl.Cons(
      new DefVar<>(consDeclContext.ID().getText()),
      new Decl.Tele(Seq.wrapJava(consDeclContext.param()).flatMap(this::param)));
  }

  private Expr buildDT(boolean isPi, SourcePos pos, SeqView<Param<Expr>> params, Expr body) {
    if (params.isEmpty()) return body;
    var drop = params.drop(1);
    return new Expr.DT(isPi, pos, params.first(), buildDT(isPi,
      sourcePosForSubExpr(source, drop.map(x -> x.type().pos()), body.pos()), drop, body));
  }

  private Expr buildLam(SourcePos pos, SeqView<WithPos<LocalVar>> params, Expr body) {
    if (params.isEmpty()) return body;
    var drop = params.drop(1);
    return new Expr.Lam(pos, params.first().data(), buildLam(
      sourcePosForSubExpr(source, drop.map(WithPos::sourcePos), body.pos()), drop, body));
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
    return sourcePosOf(ctx, source);
  }

  static @NotNull SourcePos sourcePosOf(TerminalNode node, SourceFile sourceFile) {
    var token = node.getSymbol();
    var line = token.getLine();
    return new SourcePos(
      sourceFile,
      token.getStartIndex(),
      token.getStopIndex(),
      line,
      token.getCharPositionInLine(),
      line,
      token.getCharPositionInLine() + token.getText().length() - 1
    );
  }

  static @NotNull SourcePos sourcePosOf(ParserRuleContext ctx, SourceFile sourceFile) {
    var start = ctx.getStart();
    var end = ctx.getStop();
    return new SourcePos(
      sourceFile,
      start.getStartIndex(),
      end.getStopIndex(),
      start.getLine(),
      start.getCharPositionInLine(),
      end.getLine(),
      end.getCharPositionInLine() + end.getText().length() - 1
    );
  }

  static @NotNull SourcePos sourcePosForSubExpr(
    @NotNull SourceFile sourceFile,
    @NotNull SeqView<SourcePos> params,
    @NotNull SourcePos bodyPos
  ) {
    var restParamSourcePos = params.fold(SourcePos.NONE, (acc, it) -> {
      if (acc == SourcePos.NONE) return it;
      return new SourcePos(sourceFile, acc.tokenStartIndex(), it.tokenEndIndex(),
        acc.startLine(), acc.startColumn(), it.endLine(), it.endColumn());
    });
    return new SourcePos(
      sourceFile,
      restParamSourcePos.tokenStartIndex(),
      bodyPos.tokenEndIndex(),
      restParamSourcePos.startLine(),
      restParamSourcePos.startColumn(),
      bodyPos.endLine(),
      bodyPos.endColumn()
    );
  }
}
