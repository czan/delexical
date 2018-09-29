(defproject delexical "0.1.0-SNAPSHOT"
  :description "Call-time lexical closures for Clojure"
  :url "https://github.com/TristeFigure/delexical"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojars.tristefigure/shuriken "0.14.20"]
                 [dance "0.2.0-SNAPSHOT"]
                 [threading "0.3.3"]
                 [lexikon "0.1.0"]]
  :profiles {:dev {:dependencies [[codox-theme-rdash "0.1.2"]]}}
  :plugins [[lein-codox "0.10.3"]]
  :codox {:source-uri "https://github.com/TristeFigure/delexical/" \
                      "blob/{version}/{filepath}#L{line}"
          :metadata {:doc/format :markdown}
          :themes [:rdash]})
