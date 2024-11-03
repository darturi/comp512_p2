#!/bin/bash

# Check if at least two file names are provided
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 file1 file2 [file3 ...]"
    exit 1
fi

# Use the first file as a reference and strip the first line
reference_file="$1"
reference_content=$(tail -n +2 "$reference_file")

# Loop through all provided files, ignoring the first line
for file in "$@"; do
    file_content=$(tail -n +2 "$file")

    if [ "$reference_content" != "$file_content" ]; then
        echo "differences"
        exit 0
    fi
done

echo "correct"