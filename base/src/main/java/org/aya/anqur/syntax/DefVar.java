package org.aya.anqur.syntax;

import org.aya.anqur.util.AnyVar;
import org.jetbrains.annotations.NotNull;

public final class DefVar<D extends Def<Term>> implements AnyVar {
  public D core;
  public final @NotNull String name;

  public DefVar(@NotNull String name) {
    this.name = name;
  }

  @Override public @NotNull String name() {
    return name;
  }
}
