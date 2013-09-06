# nephila

<img src="https://github.com/timmc/nephila/raw/master/doc/samples/cassaforte.png"
 alt="Example graph" title="clojurewerkz/cassaforte namespace graph"
 align="right" />

A nascent Leiningen plugin to show a graph of your namespaces.

## Usage

To use nephila, graphviz must be installed and available on the path.

Put `[org.timmc/nephila "0.2.0"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
org.timmc/nephila 0.2.0`.

Then you can run this in any project:

    $ lein nephila output.png

and then open output.png in your image viewer of choice.

See `lein help nephila` for options. (View it online at end of
[nephila.clj][rel-neph].)

[rel-neph]:https://github.com/timmc/nephila/blob/release/src/leiningen/nephila.clj

## Tips and tricks

* The sample [graph-diff.sh][rel-diff] script uses nephila to show
  just the subgraph containing namespaces changed on a git branch.

[rel-diff]:https://github.com/timmc/nephila/blob/release/doc/samples/graph-diff.sh

## Changelog

### v0.2.0

* Take options from project map and optional command line argument
* Option added: :graph-orientation
* Option added: :only for restricting set of nodes in graph (symbols
  and path strings)
* Graph has random coloring for edges (makes intersections more readable)
* Graph has wider spacing

### v0.1.0

* Basic functionality: Read src/, output graph to named file.

## License

Copyright © 2013 Tim McCormack

Free license pending release by employer.
