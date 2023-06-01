# Anqur

[![maven]](https://repo1.maven.org/maven2/org/aya-prover/anqur/base/)
[![test](https://github.com/ice1000/anqur/actions/workflows/gradle-check.yml/badge.svg)](https://github.com/ice1000/anqur/actions/workflows/gradle-check.yml)

![image](https://user-images.githubusercontent.com/16398479/187799888-e873abef-d459-41a7-85ed-77977cb0da01.png)

[maven]: https://img.shields.io/maven-central/v/org.aya-prover.anqur/cli
[Guest0x0]: https://github.com/ice1000/Guest0x0

This project is evolved from [Guest0x0] with cubical features removed. It serves as a demo for elaboration of inductive types, pattern matching, and indexed types.

I will also be testing ideas on induction-recursion and induction-induction of simpler indexed types in this project.

## v0.5

![image](https://github.com/ice1000/anqur/assets/16398479/8ee97d3b-b622-4925-b3a6-92223f7e0b99)

Ported latest code from Aya with a new coverage checking algorithm which deals
with nested pattern matching better.

## v0.4

![image](https://user-images.githubusercontent.com/16398479/190531305-e6eadceb-b402-4544-8404-7de292c78e36.png)

Implemented a coverage checker. The following code:

```
def plus-bad (a : Nat) (b : Nat) : Nat
| (succ a) b => succ (plus a b)
```

Will cause:

```
Missing pattern >:)
```

It is too difficult to implement proper error reporting, so I'll leave it to readers :) in case you are really curious, you may check out the coverage checker in Aya.

## v0.3

![image](https://user-images.githubusercontent.com/16398479/189470948-626d6669-bfb5-4da5-a079-a15ea605213d.png)

Implements unfold properly. The following code (with the old code):

```
print : Nat => plus two two
```

Prints

```
succ (succ (succ (succ (zero))))
```

Awesome. Total lines of Java code (excluding blank/comments): 864

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
