#!/bin/bash

# Check if at least two file names are provided
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 file1 file2 [file3 ...]"
    exit 1
fi

# Use the first file as a reference
reference_file="$1"

# Loop through all provided files and compare with the reference file
for file in "$@"; do
    if ! cmp -s "$reference_file" "$file"; then
        echo "differences"
        exit 0
    fi
done

echo "correct"
