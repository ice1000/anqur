import org.aya.anqur.cli.CliMain;
import org.aya.anqur.syntax.Def;
import org.aya.anqur.syntax.Term;
import org.aya.anqur.tyck.Elaborator;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeclsTest {
  @Test public void dontSayLazy() {
    var akJr = tyck("""
      def uncurry (A B C : U)
        (t : A ** B) (f : A -> B -> C) : C => f (t.1) (t.2)
      def uncurry' (A : U) (t : A ** A) (f : A -> A -> A) : A => uncurry A A A t f
      """);
    akJr.sigma().valuesView().forEach(tycked -> {
      var body = ((Def.Fn) tycked).body();
      assertTrue(akJr.normalize(body.getLeftValue()) instanceof Term.Two two && two.isApp());
    });
  }

  @Test public void leibniz() {
    tyck("""
      def Eq (A : U) (a b : A) : U => Pi (P : A -> U) -> P a -> P b
      def refl (A : U) (a : A) : Eq A a a => \\P pa. pa
      def sym (A : U) (a b : A) (e : Eq A a b) : Eq A b a =>
          e (\\b. Eq A b a) (refl A a)
      """);
  }

  @Test public void unitNatList() {
    tyck("""
      data Unit | unit
      def unnit : Unit => unit
      data Nat
      | zero
      | succ (n : Nat)

      def two : Nat => succ (succ zero)
      print : Nat => two

      data List (A : U)
      | nil
      | cons (x : A) (xs : List A)

      def lengthTwo (A : U) (a : A) : List A => cons A a (cons A a (nil A))
      print : List Nat => lengthTwo Nat two
      """);
  }

  @Test public void patternMatchingNat() {
    tyck("""
      data Nat
      | zero
      | succ (n : Nat)

      def plus (a : Nat) (b : Nat) : Nat
      | zero b => b
      | (succ a) b => succ (plus a b)

      def two : Nat => succ (succ zero)
      def four : Nat => plus two two
      def six : Nat => plus four two
      print : Nat => six
      """);
  }

  private static @NotNull Elaborator tyck(@Language("TEXT") String s) {
    return CliMain.tyck(s, false);
  }
}
