# delexical

A Clojure library that provides closures whose lexical context can be bound at call-time rather than construction-time.

*Yo where my niggas at ?*\
*In the range of my voice*

[Smoked Out Productions – Bok Bok](https://www.youtube.com/watch?v=U_qoRNHJ_JY)

## Usage

```clojure
[delexical "0.1.0-SNAPSHOT"]
```

```clojure
(ns my-ns
  (:require [delexical.core :refer [defdelexical]]))
```

## [API doc](https://tristefigure.github.io/delexical/index.html)

## How ?

```clojure
(let [d 1000]
  (defdelexical f [a b]
    (+ a b c d)))
```

Here we created a delexical pretty much like we would have defined a normal function. The only difference so far is that the code did not raise an `"Unable to resolve symbol: c"` `CompilerException`. Indeed the whole point of a delexical is to allow symbols that are bound to nothing at the time the closure is created to be bound at call-time instead.

To do so requires that these free symbols are bound in the lexical context of the call site.

```clojure
(let [c 100]
  (f 1 10)) ;; => 1111
```

When calling a delexical, you cannot rebind symbols that were bound when the delexical was defined.

```clojure
(let [c 100 d 0]
  (f 1 10)) ;; => 1111 (rather than 111)
```

But you can bind symbols that were free at construction-time by explictly passing them at call-time as additional arguments, in the order they appear in code (pre-order).

```clojure
(let [c 100]
  (f 1 10 0)) ;; => 1011
```

### Caveats
- Delexicals cannot accept variadic arguments.
- Although delexicals look like functions, under the hood they are macros. This implies you will have to wrap them in a function if you want to use them as higher-order functions. E.g:  `(map #(f 1 %) [1 2 3])`.

## Why ?

Let's suppose you have been experimenting with something and ended up with a several hundred lines long function (hereafter called the **B**ig **F**ucking **F**unction) mostly consisting in one monstruous `let`.
This `let` is used to store the result of various computations that are then fed to various closure via the shared lexical context of that `let` in such a way that these closures have some of these variables in common.

Something akin to:
```clojure
(defn bff [object]
  (let [a (compute-a object)
        b (compute-b object)
        ... ...
        z (compute-z object)

        func1 (fn [leaf]  (+ leaf a b c))
        func2 (fn [leaf]  (+ leaf a c d))
        ... ...
        funcn (fn [leaf]  (+ leaf a b z))]

    (if (bottom? object)
      [(-> object func1 func2 ... funcn)]
      (mapcat bff (subobjects object)))))
```

So far, your code works OK with most of the obvious cases, but you need to straighten it out, either because you would like to test these `func1`, `func2` ... `funcn` closures, wish to reuse them elsewhere or because you find this big `let` ugly.

In other words, whichever the reason is, you have to move these subfunctions out of the BFF and `def` them in the current namespace.

#### Solution #1: pass as an argument what was passed via the lexical context
```clojure
(defn func1 [a b c leaf]  (+ leaf a b c))
...

(defn bff [object]
  (let [a (compute-a object)
        ... ...
        z (compute-z object)]
    (if (bottom? object)
      [(->> object (func1 a b c) (func2 a c d) ... (funcn a b z))]
      (mapcat bff (subobjects object)))))
```

**Caveats**:
- Those additionnals arguments make the extracted functions less easy to reuse.
- Changes to the BFF are necessary.

#### Solution #2: reduce the number of arguments to the bare minimum
```clojure
(defn func1 [parent leaf]
  (+ leaf (compute-a parent) (compute-b parent) (compute-c parent)))
...

(defn bff
  ([object]
    (bff nil object))
  ([parent object]
    (if (bottom? object)
      [(->> object (func1 parent) (func2 parent) ... (funcn parent))]
      (mapcat (partial bff object) (subobjects object)))))
```

**Caveats**:
- Some computations are unecessarily performed multiple times.
- Changes to the BFF are necessary.

#### Solution #3: reduce the number of arguments to the bare minimum and memoize
```clojure
(def compute-a
  (memoize (fn [object] ...)))
...
```

**Caveats**:
- Some computations are performed once: consider the case where the BFF, which is just a function that collects and transforms a tree's leafs, is run on a tree where certain leafs are identical => the `compute-*` functions will be run only once even when they contain side effects that are should be performed multiple times. This is due to the fact the memoization atom is global, whereas the variables "memoized" in the `let` were scoped to the tree-node (`object`) being processed.
- Changes to the BFF are necessary.

#### Solution #4: reduce the number of arguments to the bare minimum and structure the memoization
This can be done by storing the memo in a tree-node's `meta`, or via dynamic bindings or via equivalently convoluted ways such as maintaining a stack of memos in a global atom or a dynamic var.

#### Using delexicals

Just extract those godamned `funcs`, define them as delexicals and you're done.
```clojure
(defdelexical func1 [leaf]  (+ leaf a b c))
(defdelexical func2 [leaf]  (+ leaf a c d))
...
(defdelexical funcn [leaf]  (+ leaf a b z))

(defn bff [object]
  (let [a (compute-a object)
        b (compute-b object)
        ... ...
        z (compute-z object)]
    (if (bottom? object)
      [(-> object func1 func2 ... funcn)]
      (mapcat bff (subobjects object)))))
```

## TODO
- Support delexicals with multiple bodies.

## License

Copyright © 2018 TristeFigure

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
