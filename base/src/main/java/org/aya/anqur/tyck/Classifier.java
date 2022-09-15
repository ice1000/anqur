package org.aya.anqur.tyck;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.anqur.syntax.Pat;
import org.aya.anqur.syntax.Term;
import org.aya.anqur.util.AnyVar;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.pretty.doc.Doc;
import org.aya.util.tyck.MCT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public interface Classifier {
  /**
   * @param telescope type signature, assumed to be well-typed
   *                  (so like, no data constructors can possibly be here)
   * @param fuel      for inference of absurd patterns
   *                  (Agda sets one, maybe two)
   * @param clauses   every element can be checked against the telescope
   *                  (assumed to be well-typed)
   * @return the multi-case tree, null if proceed (see {@link MCT#classify(SeqView, ImmutableSeq, BiFunction)}
   */
  private static @Nullable MCT<Term, Doc> classifyImpl(SeqView<Param<Term>> telescope, int fuel, ImmutableSeq<MCT.SubPats<Pat>> clauses) {
    var ty = telescope.first();
    // Make sure this is NF!!
    switch (ty.type()) {
      case Term.DataCall dataCall -> {
        // If there are no remaining clauses, probably it's due to a previous `impossible` clause,
        // but since we're gonna remove this keyword, this check may not be needed in the future? LOL
        if (clauses.anyMatch(subPats -> subPats.pats().isNotEmpty()) &&
          // there are no clauses starting with a constructor pattern -- we don't need a split!
          clauses.noneMatch(subPats -> head(subPats) instanceof Pat.Con)
        ) break;
        var buffer = MutableList.<MCT<Term, Doc>>create();
        var data = dataCall.fn();
        if (data.core == null) throw new RuntimeException("Not yet tycked: " + data.name);
        var body = data.core.cons();
        // For all constructors,
        for (var ctor : body) {
          var conTele = ctor.tele().view();
          var conTele2 = conTele.toImmutableSeq();
          // Find all patterns that are either catchall or splitting on this constructor,
          // e.g. for `suc`, `suc (suc a)` will be picked
          var matches = clauses
            .mapIndexedNotNull((ix, subPats) -> matches(subPats, ix, conTele2, ctor.name()));
          // In case no pattern matches this constructor,
          var matchesEmpty = matches.isEmpty();
          // we consume one unit of fuel and,
          if (matchesEmpty) fuel--;
          // if the pattern has no arguments and no clause matches,
          var definitely = matchesEmpty && conTele2.isEmpty() && telescope.sizeEquals(1);
          // we report an error.
          // If we're running out of fuel, we also report an error.
          if (definitely || fuel <= 0) {
            buffer.append(new MCT.Error<>(ImmutableSeq.empty(),
              Doc.english("Missing pattern >:)")));
            continue;
          }
          MCT<Term, Doc> classified;
          // The base case of classifying literals together with other patterns:
          // variable `nonEmpty` only has two kinds of patterns: bind and literal.
          // We should put all bind patterns altogether and check overlapping of literals, which avoids
          // converting them to constructor forms and preventing possible stack overflow
          // (because literal overlapping check is simple).
          var nonEmpty = matches.filter(subPats -> subPats.pats().isNotEmpty());
          classified = classify(conTele2.view(), fuel, matches);
          var conCall = new Term.ConCall(ctor.name(), conTele2.map(Param::type), dataCall.args());
          var newTele = telescope.drop(1)
            .map(param -> subst(param, ty.x(), conCall))
            .toImmutableSeq().view();
          var fuelCopy = fuel;
          var rest = classified.flatMap(pat -> pat.propagate(
            classify(newTele, fuelCopy, MCT.extract(pat, clauses).map(MCT.SubPats::drop))));
          buffer.append(rest);
        }
        return new MCT.Node<>(dataCall, buffer.toImmutableSeq());
      }
      case Term.DT sigma && !sigma.isPi() -> {
        throw new UnsupportedOperationException("unimplemented");
      }
      case Term rest -> {
        // Patterns are well-typed, so these patterns can only be bind patterns!!
        if (clauses.isEmpty()) throw new RuntimeException("Missing clauses for type " + rest.toDoc().commonRender());
      }
    }
    return null;
  }
  static @NotNull MCT<Term, Doc> classify(SeqView<Param<Term>> view, int fuel, ImmutableSeq<MCT.SubPats<Pat>> matches) {
    return MCT.classify(view, matches, (tele, clauses) -> classifyImpl(tele, fuel, clauses));
  }

  private static Param<Term> subst(Param<Term> param, LocalVar x, Term term) {
    return new Param<>(param.x(), param.type().subst(x, term));
  }

  private static Pat head(MCT.SubPats<Pat> subPats) {
    return subPats.head();
  }

  private static @Nullable MCT.SubPats<Pat> matches(MCT.SubPats<Pat> subPats, int ix, ImmutableSeq<Param<Term>> conTele, AnyVar ctorRef) {
    var head = head(subPats);
    if (head instanceof Pat.Con ctorPat && ctorPat.ref() == ctorRef)
      return new MCT.SubPats<>(ctorPat.pats().view(), ix);
    if (head instanceof Pat.Bind)
      return new MCT.SubPats<>(conTele.view().map(x -> new Pat.Bind(x.x())), ix);
    return null;
  }
}
