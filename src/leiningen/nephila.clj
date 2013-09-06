(ns leiningen.nephila
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.namespace.file :as ctn-file]
            [clojure.tools.namespace.find :as ctn-find]
            [clojure.tools.namespace.parse :as ctn-parse]
            [rhizome.viz :as viz]))

;;;; Options

(def default-opts
  {:graph-orientation :horizontal})

(defn get-opts
  "Compute options from the project map and optional command-line options."
  [project cli-opts]
  (merge default-opts (:nephila project) cli-opts))

(def graph-orientations #{:horizontal :vertical})

;;;; Computing the graph

(defn get-source-dirs
  [project]
  ;;TODO
  [(java.io.File. "./src")])

(defn decl->ns-sym
  "Given a c.t.n decl, return the namespace symbol."
  [decl]
  (second decl))

(defn ns-at-file
  "Return the ns symbol for the namespace in the specified file."
  [f]
  (or (decl->ns-sym (ctn-file/read-file-ns-decl (io/file f)))
      (throw (RuntimeException. (str "Could not read namespace for file: " f)))))

(defn read-ns-decls
  "Read namespace declarations in the source dirs."
  [src-dirs]
  (ctn-find/find-ns-decls src-dirs))

(defn decls-to-graph
  "From a coll of files and dirs, derive a map of namespace strings
to sets of namespace strings, where the keyset is the set of namespaces
found in the filesystem and the value sets are subsets of the keyset.
Optionally, restrict graph further to the symbols in `further-restrict`."
  [decls further-restrict]
  (let [untrimmed (for [decl decls
                        :let [sym (decl->ns-sym decl)
                              deps (ctn-parse/deps-from-ns-decl decl)]]
                    [sym deps])
        own-nses (set (map first untrimmed))
        restrict-to (if further-restrict
                      (set/intersection own-nses (set further-restrict))
                      own-nses)]
    (into {}
          (for [[sym deps] untrimmed
                :when (contains? restrict-to sym)]
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

(defn random-colorer
  [from to]
  {:color (format "%.3f %.3f %.3f"
                  (rand) 0.9 0.7)})

(defn save
  [graph out-file opts]
  (let [abbrs (abbreviation-map (keys graph))
        node-namer (fn [n] {:label (replace-prefix abbrs n)})
        vert? (= (get graph-orientations
                      (:graph-orientation opts)
                      (:graph-orientation default-opts))
                 :vertical)
        img (viz/graph->image (keys graph) graph
                              :directed? true
                              :vertical? vert?
                              :options {"ranksep" "2.5"}
                              :node->descriptor node-namer
                              :edge->descriptor random-colorer)]
    (viz/save-image img out-file)))

;;;; Task

(defn compute-ns-restrict
  "Use the opts map to compute the set of namespace symbols to restrict graph to."
  [opts]
  (when-let [only (:only opts)]
    (set (for [spec only]
           (cond
            (symbol? spec) spec
            ;; Not very graceful, but it works. (Wouldn't it
            ;; be nicer to just filter when reading the source
            ;; dirs in the first place?)
            (string? spec) (ns-at-file spec)
            :else (throw (RuntimeException. (str "Unrecognized :only member: "
                                                 (pr-str spec)))))))))

(defn nephila
  "Emit a graph of namespaces in this project to the specified file.

Options available from :nephila in project:

- :graph-orientation can be :horizontal (default) or :vertical
- :only can be a coll of namespace names (as symbols) and paths (as strings)
  to limit graph to. Use nil to override a previous restriction."
  [project out-file & [opts-str]]
  (let [cli-opts (if opts-str
                   (binding [*read-eval* false]
                     (read-string opts-str))
                   {})
        opts (get-opts project cli-opts)
        src-dirs (get-source-dirs project)
        decls (read-ns-decls src-dirs)
        graph (decls-to-graph decls (compute-ns-restrict opts))]
    (save graph out-file opts)))
