module aya.anqur.cli {
  requires static org.jetbrains.annotations;
  requires org.antlr.antlr4.runtime;
  requires aya.anqur.base;
  requires info.picocli;
  requires transitive kala.base;
  requires transitive kala.collection;
  requires transitive aya.pretty;
  requires transitive aya.repl;
  requires transitive aya.util;

  exports org.aya.anqur.cli;
  exports org.aya.anqur.parser;
}
