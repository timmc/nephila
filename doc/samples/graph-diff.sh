#!/bin/bash
# Show files in diff from $1 to $2 for Clojure project at $3 (output to $4)
# Example: graph-diff.sh master my-branch server/ graph.png

from=$1
to=$2
project_base=$3
output=$(realpath $4)

files=$(git diff --name-only $from...$to | sed "s,^$project_base,," | sed 's/.*/"\0"/' | paste -s -d" " -)

cd $project_base
lein nephila "$output" '{:only [' "$files" ']}'
