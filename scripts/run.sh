#!/usr/bin/env bash
set -e

# Define directories
INPUT_DIR="./incoming_cvs"
OUTPUT_DIR="./redacted_cvs"
JAR_PATH="./target/scala-3.3.7/cv-redact-tool-assembly-1.0.jar"

# Ensure the JAR file exists before proceeding
if [ ! -f "$JAR_PATH" ]; then
    echo "Error: JAR not found at $JAR_PATH. Run 'sbt assembly' first."
    exit 1
fi
# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Verify input directory exists
if [ ! -d "$INPUT_DIR" ]; then
    echo "Error: Input directory $INPUT_DIR not found"
    exit 1
fi

# Verify input directory contains at least one PDF file
shopt -s nullglob
pdf_files=("$INPUT_DIR"/*.pdf)
if [ ${#pdf_files[@]} -eq 0 ]; then
    echo "Error: No PDF files found in $INPUT_DIR"
    shopt -u nullglob
    exit 1
fi
shopt -u nullglob
# Loop through all PDF files in the input directory
for pdf_file in "$INPUT_DIR"/*.pdf; do
    # Check if directory is empty to avoid processing literal '*.pdf'
    [ -e "$pdf_file" ] || continue 

    # Extract just the filename from the path
    filename=$(basename "$pdf_file")
    
    # 1. Remove the .pdf extension
    name_without_ext="${filename%.pdf}"
    
    # 2. Replace underscores with spaces (e.g., "Jane_Doe_CV" -> "Jane Doe CV")
    clean_name="${name_without_ext//_/ }"
    
    echo "Redacting $filename using extracted name: $clean_name"
    
    # Execute the Scala JAR
    # Notice $clean_name is NOT in quotes, so bash splits it into separate arguments
    # (e.g., arg3="Jane", arg4="Doe", arg5="CV")
    java -Dconfig.file=conf/application.conf -jar "$JAR_PATH" "$pdf_file" "$OUTPUT_DIR/$filename" $clean_name

    # Check if the Java command succeeded
    if [ $? -ne 0 ]; then
        echo "Error processing $filename"
        continue
    fi
done

echo "Batch redaction complete!"