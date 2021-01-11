(ns delexical.core-test
  (:require [clojure.test :refer :all]
            [shuriken.core :refer [thrown? with-ns]]
            [delexical.core :refer :all]
            [lexikon.core :refer [lexical-eval]]))

(defdelexical f [a b]
  (+ a b c d))

(let [d 4]
  (defdelexical ff [a b]
    (+ a b c d)))

(defdelexical fff [a b]
    (str a b
         (str c (str d))
         (str e)))

(defmacro miss? [sym expr]
  `(thrown?
     #(->> % .getMessage
           (re-find (re-pattern
                      (format "Unable to resolve symbol: %s in this context"
                              ~sym))))
     ~expr))

(deftest test-call-delexical
  (with-ns 'delexical.core-test
    (let [m #"Missing explicit args when calling delexical"
          w #"Too many args when calling delexical ff"]
      (testing "when the construction site has no lexical context"
        (testing "when the call site has no lexical context"
          (is (true? (thrown? m (eval '(macroexpand '(f))))))
          (is (true? (thrown? m (eval '(macroexpand '(f 1))))))
          (is (true? (miss?  'c (eval '(f 1 2)))))
          (is (true? (miss?  'd (eval '(f 1 2 3)))))
          (is (= 10 (eval '(f 1 2 3 4)))))
        (testing "when the call site has a pertinent lexical context"
          (let [c 3]
            (is (true? (thrown? m (lexical-eval '(macroexpand '(f))))))
            (is (true? (thrown? m (lexical-eval '(macroexpand '(f 1))))))
            (is (true? (miss? 'd (lexical-eval '(f 1 2)))))
            (is (true? (miss? 'd (lexical-eval '(f 1 2 3)))))
            (is (= 10 (lexical-eval '(f 1 2 3 4)))))
          (let [d 4]
            (is (true? (thrown? m (lexical-eval '(macroexpand '(f))))))
            (is (true? (thrown? m (lexical-eval '(macroexpand '(f 1))))))
            (is (true? (miss? 'c (lexical-eval '(f 1 2)))))
            (is (= 10 (lexical-eval '(f 1 2 3))))
            (is (= 10 (lexical-eval '(f 1 2 3 4)))))
          (let [c 3 d 4]
            (is (true? (thrown? m (lexical-eval '(macroexpand '(f))))))
            (is (true? (thrown? m (lexical-eval '(macroexpand '(f 1))))))
            (is (= 10 (lexical-eval '(f 1 2))))
            (is (= 10 (lexical-eval '(f 1 2 3))))
            (is (= 10 (lexical-eval '(f 1 2 3 4)))))))
      (testing "when the construction site has a pertinent lexical context"
        (testing "when the call site has no lexical context"
          (is (true? (thrown? m (eval '(ff)))))
          (is (true? (thrown? m (eval '(ff 1)))))
          (is (true? (miss?  'c (eval '(ff 1 2)))))
          (is (= 107 (eval '(ff 1 2 100))))
          (is (true? (thrown? w (eval '(ff 1 2 100 1000))))))
        (testing "when the call site has a pertinent lexical context"
          (let [c 3]
            (is (true? (thrown? m (lexical-eval '(macroexpand '(ff))))))
            (is (true? (thrown? m (lexical-eval '(macroexpand '(ff 1))))))
            (is (= 10  (lexical-eval '(ff 1 2))))
            (is (= 107 (lexical-eval '(ff 1 2 100))))
            (is (true? (thrown? w (lexical-eval '(ff 1 2 100 1000)))))
            (testing "creation-time bound syms cannot be rebound at call-time"
              (let [d 1000]
                (is (= 10 (lexical-eval '(ff 1 2)))))))))
      (testing "with multiple free symbols"
        (testing "can bind them at call-time by passing them in pre-order."
          (let [e "e"]
            (is (= "abcde" (fff "a" "b" "c" "d")))))))))
