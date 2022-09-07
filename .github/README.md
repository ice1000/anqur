# Anqur

[![maven]](https://repo1.maven.org/maven2/org/aya-prover/anqur/base/)
[![test](https://github.com/ice1000/anqur/actions/workflows/gradle-check.yml/badge.svg)](https://github.com/ice1000/anqur/actions/workflows/gradle-check.yml)

![image](https://user-images.githubusercontent.com/16398479/187799888-e873abef-d459-41a7-85ed-77977cb0da01.png)

[maven]: https://img.shields.io/maven-central/v/org.aya-prover/anqur/base
[Guest0x0]: https://github.com/ice1000/Guest0x0

This is a project evolved from [Guest0x0] with cubical features removed. It serves as a demo for elaboration of inductive types and indexed types.

## v0.1

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
