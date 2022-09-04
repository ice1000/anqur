package org.aya.anqur.cli;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import org.antlr.v4.runtime.*;
import org.aya.anqur.parser.AnqurLexer;
import org.aya.anqur.parser.AnqurParser;
import org.aya.anqur.prelude.GeneratedVersion;
import org.aya.anqur.syntax.Decl;
import org.aya.anqur.syntax.Def;
import org.aya.anqur.tyck.Elaborator;
import org.aya.anqur.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "anqur",
  mixinStandardHelpOptions = true,
  version = "Anqur v" + GeneratedVersion.VERSION_STRING,
  showDefaultValues = true)
public class CliMain implements Callable<Integer> {
  @CommandLine.Parameters(paramLabel = "<input-file>", description = "File to compile")
  public String inputFile;
  @CommandLine.Option(names = {"--st"}, description = "Show the stack trace")
  public boolean stackTrace = false;

  public static void main(String @NotNull ... args) {
    System.exit(new CommandLine(new CliMain()).execute(args));
  }

  @Override public Integer call() throws Exception {
    if (inputFile == null) {
      System.err.println("Anqur " + GeneratedVersion.COMMIT_HASH);
      System.err.println("Use -h for help");
      return 1;
    }
    try {
      var ak = tyck(Files.readString(Paths.get(inputFile)), true);
      System.out.println("Tycked " + ak.sigma().size() + " definitions, phew.");
      return 0;
    } catch (RuntimeException re) {
      System.err.println(re.getMessage());
      if (stackTrace) re.printStackTrace();
      return 2;
    }
  }

  public static @NotNull Elaborator andrasKovacs() {
    return new Elaborator(MutableMap.create(), MutableMap.create());
  }

  public static @NotNull AnqurParser parser(String s) {
    var p = new AnqurParser(new CommonTokenStream(new AnqurLexer(CharStreams.fromString(s))));
    p.removeErrorListeners();
    p.addErrorListener(new Listener());
    return p;
  }

  private static class Listener extends BaseErrorListener {
    @Override public void syntaxError(
      Recognizer<?, ?> r, Object os, int line, int col, String msg,
      RecognitionException e
    ) {
      throw new RuntimeException("line " + line + ":" + col + " " + msg);
    }
  }

  public static @NotNull ImmutableSeq<Decl> def(String s) {
    var decls = ImmutableSeq.from(parser(s).program().decl());
    var edj = new Resolver(MutableMap.create());
    return decls.map(d -> edj.def(new Parser(new SourceFile("<input>", Option.none(), s)).def(d)));
  }

  public static @NotNull Elaborator tyck(String code, boolean verbose) {
    var artifact = def(code);
    var akJr = andrasKovacs();
    for (var def : artifact) {
      var tycked = akJr.def(def);
      if (tycked instanceof Def.Print print) {
        System.out.println(print.body().toDoc().commonRender());
      } else {
        akJr.sigma().put(tycked.name(), tycked);
        if (verbose) System.out.println(tycked.name());
      }
    }
    return akJr;
  }
}
