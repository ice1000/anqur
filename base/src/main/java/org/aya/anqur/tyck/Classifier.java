package org.aya.anqur.tyck;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.immutable.primitive.ImmutableIntSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.anqur.syntax.Def;
import org.aya.anqur.syntax.Pat;
import org.aya.anqur.syntax.Term;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.aya.util.Arg;
import org.aya.util.tyck.pat.ClassifierUtil;
import org.aya.util.tyck.pat.Indexed;
import org.aya.util.tyck.pat.PatClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Classifier implements ClassifierUtil<Normalizer, Term, Param<Term>, Pat, LocalVar> {
  static void classify(
    @NotNull Pat.ClauseSet<Term> clauses,
    @NotNull ImmutableSeq<Param<Term>> telescope
  ) {
    var classifier = new Classifier();
    var classification = classifier.classifyN(new Normalizer(MutableMap.create()),
      telescope.view(), clauses.view()
        .mapIndexed((i, clause) -> new Indexed<>(clause.pats().view(), i))
        .toImmutableSeq(), 2);
    var p = classification.partition(c -> c.cls().isEmpty());
    var missing = p.component1();
    if (missing.isNotEmpty()) throw new RuntimeException("Missing: " + missing);
    // return p.component2();
  }

  @Override public Param<Term> subst(Normalizer normalizer, Param<Term> termParam) {
    return new Param<>(termParam.x(), normalizer.term(termParam.type()));
  }

  @Override public Pat normalize(Pat pat) {
    return pat;
  }

  @Override public Normalizer add(Normalizer normalizer, LocalVar v, Term term) {
    normalizer.rho().put(v, term);
    return normalizer;
  }

  @Override public LocalVar ref(Param<Term> termParam) {
    return termParam.x();
  }

  /**
   * @param param   type signature, assumed to be well-typed
   *                (so like, no data constructors can possibly be here)
   * @param fuel    for inference of absurd patterns
   *                (Agda sets one, maybe two)
   * @param clauses every element can be checked against the telescope
   *                (assumed to be well-typed)
   * @return the multi-case tree
   */
  @Override public @NotNull ImmutableSeq<PatClass<Arg<Term>>> classify1(
    @NotNull Normalizer normalizer, @NotNull Param<Term> param,
    @NotNull ImmutableSeq<Indexed<Pat>> clauses, int fuel
  ) {
    var whnfTy = normalizer.term(param.type());
    switch (whnfTy) {
      default -> {
      }
      // Note that we cannot have ill-typed patterns such as constructor patterns,
      // since patterns here are already well-typed
      case Term.DT sigma when !sigma.isPi() -> {
        throw new UnsupportedOperationException("unimplemented");
      }
      // THE BIG GAME
      case Term.DataCall dataCall -> {
        // In case clauses are empty, we're just making sure that the type is uninhabited,
        // so proceed as if we have valid patterns
        if (clauses.isNotEmpty() &&
          // there are no clauses starting with a constructor pattern -- we don't need a split!
          clauses.noneMatch(subPat -> subPat.pat() instanceof Pat.Con)
        ) break;
        var data = dataCall.fn();
        if (data.core == null) throw new RuntimeException("Not yet tycked: " + data.name);
        var body = data.core.cons();
        var buffer = MutableList.<PatClass<Arg<Term>>>create();
        // For all constructors,
        for (var ctor : body) {
          var fuel1 = fuel;
          var conTele = new Normalizer.Renamer(MutableMap.create()).params(ctor.tele());
          // Find all patterns that are either catchall or splitting on this constructor,
          // e.g. for `suc`, `suc (suc a)` will be picked
          var matches = clauses.mapIndexedNotNull((ix, subPat) ->
            // Convert to constructor form
            matches(conTele, ctor, ix, subPat));
          var conHead = dataCall.args();
          // The only matching cases are catch-all cases, and we skip these
          if (matches.isEmpty()) {
            fuel1--;
            // In this case we give up and do not split on this constructor
            if (conTele.isEmpty() || fuel1 <= 0) {
              var err = new Term.Error("...");
              buffer.append(new PatClass<>(new Arg<>(
                new Term.ConCall(ctor.name(),
                  conTele.isEmpty() ? ImmutableSeq.empty() : ImmutableSeq.of(err),
                  conHead), true
              ), ImmutableIntSeq.empty()));
              continue;
            }
          }
          var classes = classifyN(normalizer.derive(), conTele.view(), matches, fuel1);
          buffer.appendAll(classes.map(args -> new PatClass<>(
            new Arg<>(new Term.ConCall(ctor.name(),
              args.term().map(Arg::term), conHead), true),
            args.cls())));
        }
        return buffer.toImmutableSeq();
      }
    }
    var bind = new Term.Ref(param.x());
    return ImmutableSeq.of(new PatClass<>(new Arg<>(bind, true), Indexed.indices(clauses)));
  }

  private static @Nullable Indexed<SeqView<Pat>> matches(
    ImmutableSeq<Param<Term>> conTele, Def.Cons ctor, int ix, Indexed<Pat> subPat
  ) {
    return switch (subPat.pat()) {
      case Pat.Con c when c.ref() == ctor.name() -> new Indexed<>(c.pats().view(), ix);
      case Pat.Bind b -> new Indexed<>(conTele.view().map(p -> new Pat.Bind(p.x())), ix);
      default -> null;
    };
  }
}
