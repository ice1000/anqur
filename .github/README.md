# Anqur

[![maven]](https://repo1.maven.org/maven2/org/aya-prover/anqur/base/)
[![test](https://github.com/ice1000/anqur/actions/workflows/gradle-check.yml/badge.svg)](https://github.com/ice1000/anqur/actions/workflows/gradle-check.yml)

![image](https://user-images.githubusercontent.com/16398479/187799888-e873abef-d459-41a7-85ed-77977cb0da01.png)

[maven]: https://img.shields.io/maven-central/v/org.aya-prover.anqur/cli
[Guest0x0]: https://github.com/ice1000/Guest0x0

This project is evolved from [Guest0x0] with cubical features removed. It serves as a demo for elaboration of inductive types, pattern matching, and indexed types.

I will also be testing ideas on induction-recursion and induction-induction of simpler indexed types in this project.

## v0.3

## v0.2

![image](https://user-images.githubusercontent.com/16398479/188980798-baebf9ad-ebb0-444d-9f48-080920b3182a.png)

Implement pattern matching functions and recursive functions. No termination and coverage check yet. Reduction of pattern matching is still a work-in-progress. The syntax is kinda Lean-flavored:

```
def plus (a : Nat) (b : Nat) : Nat
| zero b => b
| (succ a) b => succ (plus a b)
```

## v0.1

![image](https://user-images.githubusercontent.com/16398479/188972426-80e0f1de-1da8-426e-b221-88cbd8ef16c7.png)

Initial version with simplest parameterized inductive types:

```
data Unit | unit
def myYolife : Unit => unit

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
```

Total lines of Java code: 745. Please wait, patterns are on the way.
