(ns leiningen.nephila-test
  (:use clojure.test
        leiningen.nephila))

(deftest pathtree
  (is (= (paths-to-tree [])
         {}))
  (is (= (paths-to-tree [["core"]])
         {"core" {:down {}}}))
  (is (= (paths-to-tree [["core" "foo" "bar"]])
         {"core" {:down {"foo" {:down {"bar" {:down {}}}}}}}))
  (is (= (paths-to-tree [["core" "foo"] ["core" "bar"] ["core" "baz"]
                         ["core" "foo" "quux"] ["other" "root"]])
         {"core" {:down {"foo" {:down {"quux" {:down {}}}}
                         "bar" {:down {}}
                         "baz" {:down {}}}}
          "other" {:down {"root" {:down {}}}}})))

(deftest add-abbrevs
  (let [subject #(add-tree-abbrevs % abbreviator)]
    (is (= (subject {})
           {}))
    (is (= (subject {"core" {:down {}}})
           {"core" {:down {} :abbrev "c"}}))
    (is (= (subject {"core" {:down {"foo" {:down {"quux" {:down {}}}}
                                    "bar" {:down {}}
                                    "baz" {:down {}}}}
                     "other" {:down {"root" {:down {}}}}})
           {"core" {:down {"foo" {:down {"quux" {:down {}
                                                 :abbrev "q"}}
                                  :abbrev "f"}
                           "bar" {:down {} :abbrev "bar"}
                           "baz" {:down {} :abbrev "baz"}}
                    :abbrev "c"}
            "other" {:down {"root" {:down {}
                                    :abbrev "r"}}
                     :abbrev "o"}}))))

(deftest abbrev-level
  (is (= (abbreviator []) {}))
  (is (= (abbreviator ["foo"]) {"foo" "f"}))
  (is (= (abbreviator ["bar" "baz"])
         {"bar" "bar", "baz" "baz"}))
  (is (= (abbreviator ["foo" "bar" "baz"])
         {"foo" "f", "bar" "bar", "baz" "baz"}))
  (is (= (abbreviator ["q" "quux" "nuux"])
         {"q" "q", "quux" "quux", "nuux" "n"})))

(deftest tree-to-path-mapping
  (let [subject tree->abbrev-map]
    (is (= (subject {})
           {}))
    (is (= (subject {"core" {:down {} :abbrev "c"}})
           {["core"] ["c"]}))
    (is (= (subject {"core" {:down {"foo" {:down {"quux" {:down {}
                                                          :abbrev "q"}}
                                           :abbrev "f"}
                                    "bar" {:down {} :abbrev "bar"}
                                    "baz" {:down {} :abbrev "baz"}}
                             :abbrev "c"}
                     "other" {:down {"root" {:down {}
                                             :abbrev "r"}}
                              :abbrev "o"}})
           {["core"] ["c"]
            ["core" "foo"] ["c" "f"]
            ["core" "foo" "quux"] ["c" "f" "q"]
            ["core" "bar"] ["c" "bar"]
            ["core" "baz"] ["c" "baz"]
            ["other"] ["o"]
            ["other" "root"] ["o" "r"]}))))

(deftest prefix-replacement
  (is (= (replace-prefix {} "foo.bar") "foo.bar"))
  (is (= (replace-prefix {"foo.bar" "no"} "foo.bar") "foo.bar"))
  (is (= (replace-prefix {"foo" "yes"} "foo.bar") "yes.bar"))
  (is (= (replace-prefix {"foo" "no"} "foo") "foo"))
  (is (= (replace-prefix {"foo.bar" "no", "foo" "yes"} "foo.bar") "yes.bar")))

(deftest abbrev
  (is (= (abbreviation-map ["core.foo" "core.bar" "core.baz"
                            "core.foo.quux"])
         {"core" "c"
          "core.foo" "c.f"
          "core.bar" "c.bar"
          "core.baz" "c.baz"
          "core.foo.quux" "c.f.q"})))
