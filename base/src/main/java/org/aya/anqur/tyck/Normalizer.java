package org.aya.anqur.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.anqur.syntax.Term;
import org.aya.anqur.util.LocalVar;
import org.aya.anqur.util.Param;
import org.jetbrains.annotations.NotNull;

public record Normalizer(@NotNull MutableMap<LocalVar, Term> rho) {
  public static @NotNull Term rename(@NotNull Term term) {
    return new Renamer(MutableMap.create()).term(term);
  }

  public Param<Term> param(Param<Term> param) {
    return new Param<>(param.x(), term(param.type()));
  }

  public Term term(Term term) {
    return switch (term) {
      case Term.Ref ref -> rho.getOption(ref.var())
        .map(Normalizer::rename)
        .map(this::term).getOrDefault(ref);
      case Term.UI u -> u;
      case Term.Lam lam -> new Term.Lam(lam.x(), term(lam.body()));
      case Term.DT dt -> new Term.DT(dt.isPi(), param(dt.param()), term(dt.cod()));
      case Term.Two two -> {
        var f = term(two.f());
        var a = term(two.a());
        // Either a tuple or a stuck term is preserved
        if (!two.isApp() || !(f instanceof Term.Lam lam)) yield new Term.Two(two.isApp(), f, a);
        rho.put(lam.x(), a);
        var body = term(lam.body());
        rho.remove(lam.x());
        yield body;
      }
      case Term.Proj proj -> {
        var t = term(proj.t());
        if (!(t instanceof Term.Two tup)) yield new Term.Proj(t, proj.isOne());
        assert !tup.isApp();
        yield proj.isOne() ? tup.f() : tup.a();
      }
      case Term.FnCall call -> {
        var fn = call.fn().core;
        var args = call.args().map(this::term);
        if (fn == null) yield new Term.FnCall(call.fn(), args);
        fn.teleVars().zip(args).forEach(rho::put);
        var bud = fn.body().fold(this::term, cls ->
          Matchy.unfold(cls, args).map(this::term).getOrElse(() -> new Term.FnCall(call.fn(), args)));
        fn.teleVars().forEach(rho::remove);
        yield bud;
      }
      case Term.ConCall conCall -> new Term.ConCall(conCall.fn(),
        conCall.args().map(this::term), conCall.dataArgs().map(this::term));
      case Term.DataCall dataCall -> new Term.DataCall(dataCall.fn(), dataCall.args().map(this::term));
    };
  }

  record Renamer(MutableMap<LocalVar, LocalVar> map) {
    /** @implNote Make sure to rename param before bodying */
    public Term term(Term term) {
      return switch (term) {
        case Term.Lam lam -> {
          var param = param(lam.x());
          yield new Term.Lam(param, term(lam.body()));
        }
        case Term.UI u -> u;
        case Term.Ref ref -> new Term.Ref(vv(ref.var()));
        case Term.DT dt -> {
          var param = param(dt.param());
          yield new Term.DT(dt.isPi(), param, term(dt.cod()));
        }
        case Term.Two two -> new Term.Two(two.isApp(), term(two.f()), term(two.a()));
        case Term.Proj proj -> new Term.Proj(term(proj.t()), proj.isOne());
        case Term.FnCall fnCall -> new Term.FnCall(fnCall.fn(), fnCall.args().map(this::term));
        case Term.ConCall conCall ->
          new Term.ConCall(conCall.fn(), conCall.args().map(this::term), conCall.dataArgs().map(this::term));
        case Term.DataCall dataCall -> new Term.DataCall(dataCall.fn(), dataCall.args().map(this::term));
      };
    }

    private @NotNull LocalVar vv(@NotNull LocalVar var) {
      return map.getOrDefault(var, var);
    }

    private Param<Term> param(Param<Term> param) {
      return new Param<>(param(param.x()), term(param.type()));
    }

    private LocalVar param(LocalVar param) {
      var var = new LocalVar(param.name());
      map.put(param, var);
      return var;
    }
  }
}
