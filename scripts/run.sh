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

    # Extract just the filename (e.g., John_Paul_Jones_12345.pdf)
    filename=$(basename "$pdf_file")
    
    # 1. Strip the .pdf extension -> John_Paul_Jones_12345
    name_without_ext="${filename%.pdf}"
    
    # 2. Extract employee number: grab everything AFTER the last underscore -> 12345
    employeenumber="${name_without_ext##*_}"
    
    # 3. Extract the names: grab everything BEFORE the last underscore -> John_Paul_Jones
    names_part="${name_without_ext%_*}"
    
    # 4. Replace underscores with spaces -> "John Paul Jones"
    names_to_pass="${names_part//_/ }"
    
    # Define the new output filename using only the employee number
    output_filename="${employeenumber}.pdf"
    
    echo "Processing $filename -> Saving as $output_filename (Redacting: $names_to_pass)"
    
    # Execute the Scala JAR
    # IMPORTANT: $names_to_pass is intentionally NOT in quotes so bash splits 
    # the string by spaces and passes each name as a separate argument to Scala.
    java -jar "$JAR_PATH" "$pdf_file" "$OUTPUT_DIR/$output_filename" $names_to_pass
    # Check if the Java command succeeded
    if [ $? -ne 0 ]; then
        echo "Error processing $filename"
        continue
    fi
done

echo "Batch redaction complete!"