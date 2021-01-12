(ns delexical.core
  (:require [clojure.set :as set]
            [shuriken.core :refer [deconstruct disentangle unwrap-form]]
            [dance.core :refer [dance free-symbols-collecting-dance]]))

;; TODO: does not work for multiple-arities fns
(defmacro defdelexical
  "Defines a closure-like entity whose lexical context can be set at
  both construction-time and call-time.

  Any free variables in the definition of a delexical will be looked
  up in the lexical context of the call-site and transparently bound
  in the delexical body.

  Symbols captured by the definition of the delexical at definition
  time will not be rebound at call-time.

  ```clojure
  (let [b 10]
    (defdelexical f [a]
      (+ a b c)))

  (let [a 1, b 2, c 3]
    (f 1)) ;=> 1 + 10 + 3 = 14

  (macroexpand '(f 1)) ;=> (f* c 1)
  ```

  Note that a delexical is a macro under the hood. To use it as a
  parameter to a higher order function you will have to wrap it
  inside a function, e.g. `(map #(f 1 %) [1 2 3])`."
  [nme args & body]
  (let [declared-args  (set (deconstruct args))
        parsed-args    (disentangle args)
        arg-vector     (concat (map (fn [_] (gensym 'arg)) (:items parsed-args))
                               (when (:more parsed-args)
                                 ['& (gensym 'rest)]))
        arg-names      (remove #(= % '&) arg-vector)
        [expanded ctx] (dance `(do ~@body)
                              free-symbols-collecting-dance
                              :return :form-and-context)
        free-vars      (set/difference (set (:free-symbols ctx))
                                       declared-args
                                       (-> &env keys set))
        fn-name        (-> nme name (str "*") symbol)]
    `(do
       (defn ~fn-name [~@free-vars ~@args]
         ~expanded)
       (defmacro ~nme
         {:arglists '[~args]}
         [~@arg-vector]
         ~(if (:more parsed-args)
            `(list* '~fn-name ~@(map #(list 'quote %) free-vars) ~@arg-names)
            `(list '~fn-name ~@(map #(list 'quote %) free-vars) ~@arg-names))))))
