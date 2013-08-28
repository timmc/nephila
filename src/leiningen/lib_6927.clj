(ns leiningen.lib-6927
  (:require [clojure.set :as set]
            [clojure.tools.namespace.find :as ctn-find]
            [clojure.tools.namespace.parse :as ctn-parse]))

(defn get-source-dirs
  [project]
  ;;TODO
  [(java.io.File. "./src")])

(defn read-ns-decls
  "Read namespace declarations in the source dirs."
  [src-dirs]
  (ctn-find/find-ns-decls src-dirs))

(defn decls-to-graph
  "From a coll of files and dirs, derive a map of namespace symbol
to sets of namespace symbols, where the keyset is the set of namespaces
found in the filesystem and the value sets are subsets of the keyset."
  [decls]
  (let [untrimmed (for [decl decls
                        :let [name (second decl)
                              deps (ctn-parse/deps-from-ns-decl decl)]]
                    [name deps])
        restrict-to (set (map first untrimmed))]
    (into {}
          (for [[name deps] untrimmed]
            [name (set/intersection restrict-to deps)]))))

(defn lib-6927
  "Show a graph of namespaces in this project."
  [project & _]
  (->> project
       get-source-dirs
       read-ns-decls
       decls-to-graph
       prn)
  #_(let [src-dirs ()
        decls ( src-dirs)
        graph ( decls)]
    (prn graph)))
