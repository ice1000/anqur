package org.aya.anqur.util;

import org.aya.anqur.syntax.DefVar;
import org.jetbrains.annotations.NotNull;

public sealed interface AnyVar permits DefVar, LocalVar {
  @NotNull String name();
}
