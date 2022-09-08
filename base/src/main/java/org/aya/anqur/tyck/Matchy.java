package org.aya.anqur.tyck;

import org.aya.anqur.syntax.Pat;
import org.aya.anqur.syntax.Term;
import org.jetbrains.annotations.NotNull;

public record Matchy(@NotNull Elaborator elaborator) {
  public void pat(@NotNull Pat pat, @NotNull Term type) {
    switch (pat) {
      case Pat.Bind bind -> elaborator.gamma().put(bind.bind(), type);
      case Pat.Con con && type instanceof Term.DataCall data -> {
        var core = con.ref().core;
        var expected = core.owner();
        if (data.fn() != expected) throw new RuntimeException("Expected " + data.fn().name + ", got " + expected.name);
        core.tele().zipView(con.pats())
          .forEach(pair -> pat(pair._2, pair._1.type()));
      }
      case Pat.Con con -> throw new RuntimeException(
        "So " + con.ref().name + " is not a constructor for type " + type.toDoc().commonRender());
    }
  }
}
