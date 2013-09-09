#!/bin/bash
# Show files in diff from $1 to $2 for Clojure project at $3 (output to $4)
# Example: graph-diff.sh master my-branch server/ graph.png

# Requires realpath to be on path. (Available in ubuntu as package `realpath`.)

from=$1
to=$2
project_base=$3
output_dir=$(dirname "$4")
mkdir -p -- "$output_dir"
output=$(realpath -- "$output_dir")/$(basename "$4")

files=$(git diff --name-only $from...$to | sed "s,^$project_base,," | sed 's/.*/"\0"/' | paste -s -d" " -)

cd $project_base
lein nephila "$output" '{:only [' "$files" ']}'
