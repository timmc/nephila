(ns leiningen.nephila
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.namespace.find :as ctn-find]
            [clojure.tools.namespace.parse :as ctn-parse]
            [rhizome.viz :as viz]))

;;;; Computing the graph

(defn get-source-dirs
  [project]
  ;;TODO
  [(java.io.File. "./src")])

(defn read-ns-decls
  "Read namespace declarations in the source dirs."
  [src-dirs]
  (ctn-find/find-ns-decls src-dirs))

(defn decls-to-graph
  "From a coll of files and dirs, derive a map of namespace strings
to sets of namespace strings, where the keyset is the set of namespaces
found in the filesystem and the value sets are subsets of the keyset."
  [decls]
  (let [untrimmed (for [decl decls
                        :let [sym (second decl)
                              deps (ctn-parse/deps-from-ns-decl decl)]]
                    [sym deps])
        restrict-to (set (map first untrimmed))]
    (into {}
          (for [[sym deps] untrimmed]
            [(name sym) (map name (set/intersection restrict-to deps))]))))

;;;; Path abbreviating

;; Paths are sequences of zero or more non-empty strings.
;; Path-strings are dot-delimited paths.

(defn paths-to-tree
  "Build a tree out of paths. Each level is a map with keys as path segments and values as maps of :down to subtree."
  [paths]
  (into {}
        (for [[k vs] (group-by first (filter #(pos? (count %)) paths))
              :let [down (into {}
                               (filter not-empty
                                       (paths-to-tree (map rest vs))))]]
          [k {:down down}])))

(defn add-tree-abbrevs
  "Given a path tree, add abbreviations for nodes (:abbrev key.)
`abbreviator` is a fn taking a set of node names at a single level and
returning a map of names to their abbreviations (or original values.)"
  [tree abbreviator]
  (let [abbrevs (abbreviator (keys tree))]
    (into {}
          (for [[k info] tree]
            [k (-> info
                   (assoc :abbrev (abbrevs k))
                   (update-in [:down] add-tree-abbrevs abbreviator))]))))

(defn abbreviator
  "An abbreviator for path-tree-abbrevs, shortening strings to their first
letter as long as they are distinct. Assumes the input is distinct."
  [strs]
  (let [initials (map first strs)
        unique-initials (->> initials
                             (group-by identity)
                             (filter #(= (count (val %)) 1))
                             (map key)
                             (set))]
    (into {}
          (map (fn abbrev [orig init]
                 [orig
                  (str (get unique-initials init orig))])
               strs initials))))

(defn tree->abbrev-map
  "Take a tree with abbreviations to a map of paths to abbreviated paths."
  ([tree]
     (into {} (tree->abbrev-map tree [] [])))
  ([tree pre-full pre-abb]
     (for [[k info] tree
           :let [pre-full (conj pre-full k)
                 pre-abb (conj pre-abb (:abbrev info))]
           pair (cons [pre-full pre-abb]
                      (tree->abbrev-map (:down info) pre-full pre-abb))]
       pair)))

(defn mapkv
  [kf vf m]
  (zipmap (map kf (keys m))
          (map vf (vals m))))

(defn string->path
  [^String path-string]
  (.split path-string "\\."))

(defn path->string
  [path]
  (str/join \. path))

(defn abbreviation-map
  "Map of path strings to fully abbreviated path strings (including
final segment.)"
  [path-strings]
  (-> (map string->path path-strings)
      (paths-to-tree)
      (add-tree-abbrevs abbreviator)
      (tree->abbrev-map)
      (->> (mapkv path->string path->string))))

(defn replace-prefix
  "Replace the prefix of a path-string (all but last segment.)"
  [replacements ^String pstr]
  (let [last-dot (.lastIndexOf pstr (int \.))]
    (if (neg? last-dot)
      pstr
      (let [candidate (.substring pstr 0 last-dot)]
        (if-let [rep (get replacements candidate)]
          (str rep (.substring pstr last-dot))
          pstr)))))

;;;; Output

(defn save
  [graph out-file]
  (let [abbrs (abbreviation-map (keys graph))
        node-namer (fn [n] {:label (replace-prefix abbrs n)})
        img (viz/graph->image (keys graph) graph
                              :directed? true
                              :vertical? true
                              :node->descriptor node-namer)]
    (viz/save-image img out-file)))

(defn nephila
  "Emit a graph of namespaces in this project to the specified file."
  [project out-file & _]
  (let [src-dirs (get-source-dirs project)
        decls (read-ns-decls src-dirs)
        graph (decls-to-graph decls)]
    (save graph out-file)))
