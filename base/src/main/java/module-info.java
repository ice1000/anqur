module aya.anqur.base {
  requires static org.jetbrains.annotations;

  requires transitive aya.pretty;
  requires transitive aya.util;
  requires transitive kala.base;
  requires transitive kala.collection;

  exports org.aya.anqur.syntax;
  exports org.aya.anqur.tyck;
  exports org.aya.anqur.util;
}
