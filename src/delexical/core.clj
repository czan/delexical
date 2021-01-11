(ns delexical.core
  (:require [clojure.set :as set]
            [shuriken.core :refer [deconstruct disentangle unwrap-form]]
            [threading.core :refer :all]
            [lexikon.core :refer [lexical-context letmap]]
            [dance.core :refer [dance free-symbols-collecting-dance]]))

;; TODO: does not work for multiple-arities fns
(defmacro defdelexical
  "Defines a closure-like entity whose lexical context can be set at
  both construction-time and call-time.
  Any symbol in the delexical body that is free at construction time
  is expected to be found in the lexical context of the call-site and
  will be magically passed to the delexical.

  Although symbols bound at construction time cannot be bind again at
  call-time, it is possible to pass these implicit variables explicitly
  as additional arguments to the delexical.

  Although delexicals support destructuring they do not support variadic
  arguments.

  ```clojure
  (let [d 1000]
    (defdelexical f [a b]
      (+ a b c d)))

  (let [c 100 d 0]
    (f 1 10)      ;; => 1111
    (f 1 10 200)) ;; => 1211
  ```

  Note that a delexical is a macro under the hood. To use it as a
  parameter to a higher order function you will have to wrap it
  inside a function, e.g. `(map #(f 1 %) [1 2 3])`."
  [nme args & body]
  (let [declared-args (set (deconstruct args))
        _ (assert (not-> args disentangle :more seq)
                  "defdelexical does not work with variadic args.")
        creation-time-env (lexical-context)
        creation-time-locals (-> creation-time-env keys
                                 (map->> (unwrap-form 'quote))
                                 set)
        already-bound-syms (set/union creation-time-locals
                                      declared-args)
        free-vars (dance body
                         free-symbols-collecting-dance
                         :context {:bound-sym? already-bound-syms})
        full-args (vec (concat declared-args free-vars))
        fn-name (-> nme name (str "*") symbol)
        limit (count declared-args)
        uplimit (+ limit (count free-vars))]
    `(do
       (defn ~fn-name ~full-args
         ~@body)
       (defmacro ~nme [& args#]
         (let [args-cnt# (count args#)]
           (assert (>= args-cnt# ~limit)
                   (str "Missing explicit args when calling delexical " '~nme
                        " (expected " ~limit ", got " args-cnt# " instead)"))
           (assert (<= args-cnt# ~uplimit)
                   (str "Too many args when calling delexical " '~nme
                        " (expected " ~uplimit ", got " args-cnt# " instead)")))
         (let [[~'declared ~'implicit] (map vec
                                            (split-at ~limit args#))
               ~'missing-implicit (vec (drop (count ~'implicit)
                                             '~free-vars))]
           `(letmap ~~creation-time-env
              (apply ~'~fn-name (concat ~~'declared
                                        ~~'implicit
                                        ~~'missing-implicit))))))))
