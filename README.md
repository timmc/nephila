# nephila

A nascent Leiningen plugin to show a graph of your namespaces.

## Usage

Put `[org.timmc/nephila "0.1.0"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
org.timmc/nephila 0.1.0`.

Then you can run this in any project:

    $ lein nephila output.png

and then open output.png in your image viewer of choice.

## License

Copyright © 2013 Tim McCormack

Free license pending release by employer.
