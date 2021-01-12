(ns delexical.core-test
  (:require [clojure.test :refer :all]
            [delexical.core :refer :all]))

;; A custom assertion type, to let me use `are` and have the tests
;; play nicely with the test.check infrastructure
(defmethod assert-expr 'check-delexical-result [msg assertion]
  (let [[_ form type result] assertion]
    (cond
      (= type '=throws>) (assert-expr nil (list 'thrown-with-msg? Exception result form))
      (= type '=>)       (assert-expr nil (list '= result form))
      :else              (throw (ex-info "Unknown assertion type"
                                         {:type   type
                                          :form   form
                                          :result result})))))


(defdelexical no-closure [a]
  (+ a b c))

(let [b 2]
  (defdelexical yes-closure [a]
    (+ a b c d)))

(defdelexical varargs [& xs]
  (apply + a b xs))

(deftest no-closure-and-no-lexical-environment
  (binding [*ns* (find-ns 'delexical.core-test)]
    (are [form type result] (check-delexical-result (eval 'form) type result)
      (no-closure)     =throws> #"Wrong number of args"
      (no-closure 1)   =throws> #"Unable to resolve symbol: (b|c) in this context"
      (no-closure 1 2) =throws> #"Wrong number of args")))

(deftest no-closure-and-relevant-lexical-environment
  (binding [*ns* (find-ns 'delexical.core-test)]
    (are [form type result] (check-delexical-result (eval 'form) type result)
      (let [b 2] (no-closure))          =throws> #"Wrong number of args"
      (let [b 2] (no-closure 1))        =throws> #"Unable to resolve symbol: c in this context"
      (let [b 2] (no-closure 1 2))      =throws> #"Wrong number of args"
      (let [c 3] (no-closure))          =throws> #"Wrong number of args"
      (let [c 3] (no-closure 1))        =throws> #"Unable to resolve symbol: b in this context"
      (let [c 3] (no-closure 1 2))      =throws> #"Wrong number of args"
      (let [b 2, c 3] (no-closure))     =throws> #"Wrong number of args"
      (let [b 2, c 3] (no-closure 1))   =>       6
      (let [b 2, c 3] (no-closure 1 2)) =throws> #"Wrong number of args")))


(deftest yes-closure-and-no-lexical-environment
  (binding [*ns* (find-ns 'delexical.core-test)]
    (are [form type result] (check-delexical-result (eval 'form) type result)
      (yes-closure)     =throws> #"Wrong number of args"
      (yes-closure 1)   =throws> #"Unable to resolve symbol: (c|d) in this context"
      (yes-closure 1 2) =throws> #"Wrong number of args")))

(deftest yes-closure-and-relevant-lexical-environment
  (binding [*ns* (find-ns 'delexical.core-test)]
    (are [form type result] (check-delexical-result (eval 'form) type result)
      (let [c 3] (yes-closure))          =throws> #"Wrong number of args"
      (let [c 3] (yes-closure 1))        =throws> #"Unable to resolve symbol: d in this context"
      (let [c 3] (yes-closure 1 2))      =throws> #"Wrong number of args"
      (let [d 4] (yes-closure))          =throws> #"Wrong number of args"
      (let [d 4] (yes-closure 1))        =throws> #"Unable to resolve symbol: c in this context"
      (let [d 4] (yes-closure 1 2))      =throws> #"Wrong number of args"
      (let [c 3, d 4] (yes-closure))     =throws> #"Wrong number of args"
      (let [c 3, d 4] (yes-closure 1))   =>       10
      (let [c 3, d 4] (yes-closure 1 2)) =throws> #"Wrong number of args")))

(deftest varargs-and-no-lexical-environment
  (binding [*ns* (find-ns 'delexical.core-test)]
    (are [form type result] (check-delexical-result (eval 'form) type result)
      (varargs)   =throws> #"Unable to resolve symbol: (a|b) in this context"
      (varargs 1) =throws> #"Unable to resolve symbol: (a|b) in this context")))

(deftest varargs-and-relevant-lexical-environment
  (binding [*ns* (find-ns 'delexical.core-test)]
    (are [form type result] (check-delexical-result (eval 'form) type result)
      (let [a 1] (varargs))        =throws> #"Unable to resolve symbol: b in this context"
      (let [a 1] (varargs 1))      =throws> #"Unable to resolve symbol: b in this context"
      (let [b 2] (varargs))        =throws> #"Unable to resolve symbol: a in this context"
      (let [b 2] (varargs 1))      =throws> #"Unable to resolve symbol: a in this context"
      (let [a 1, b 2] (varargs))   =>       3
      (let [a 1, b 2] (varargs 1)) =>       4)))


(def ^:dynamic *expansions*)
(defmacro count-expansion []
  (set! *expansions* (inc *expansions*))
  nil)
(deftest definition-only-macroexpands-once
  (binding [*ns* (find-ns 'delexical.core-test)
            *expansions* 0]
    (eval '(defdelexical expansion-counting-delexical []
             (count-expansion)))
    (is (= 1 *expansions*))))
