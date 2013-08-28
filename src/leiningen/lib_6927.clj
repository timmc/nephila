(ns leiningen.lib-6927
  (:require [clojure.set :as set]
            [clojure.tools.namespace.find :as ctn-find]
            [clojure.tools.namespace.parse :as ctn-parse]
            [rhizome.viz :as viz]))

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

(defn save
  [graph out-file]
  (let [img (viz/graph->image (keys graph) graph
                              :directed? true
                              :vertical? true
                              :node->descriptor (fn [n] {:label (name n)}))]
    (viz/save-image img out-file)))

(defn lib-6927
  "Show a graph of namespaces in this project."
  [project out-file & _]
  (let [src-dirs (get-source-dirs project)
        decls (read-ns-decls src-dirs)
        graph (decls-to-graph decls)]
    (save graph out-file)))
