# Data Management Utilities

This directory contains standalone Java utilities for managing large datasets in the E-Shop project. These tools help maintain Git performance while ensuring zero data loss.

## Utilities

### 1. GzipCompressor
Compresses a large file into GZIP format.
- **Usage**: `javac GzipCompressor.java && java GzipCompressor <file_path>`
- **Example**: `java GzipCompressor src/main/resources/seed/postal_codes.json`

### 2. FullDataRestorer
Restores a full file from a `.gz` archive.
- **Usage**: `javac FullDataRestorer.java && java FullDataRestorer <gz_path>`
- **Example**: `java FullDataRestorer src/main/resources/seed/postal_codes.json.gz`

### 3. CsvToJsonConverter
Converts a raw Pincode CSV into the standardized master JSON with proper field mapping and escaping.
- **Usage**: `javac CsvToJsonConverter.java && java CsvToJsonConverter <csv_path> <output_json_path>`
- **Example**: `java CsvToJsonConverter "G:\Project\changes\57-60.csv" "src/main/resources/seed/postal_codes.json"`

### 4. ExcelToCsvTalukMerger
Merges missing Taluk names from an All-India Pincode Excel into the standardized CSV.
- **Usage**: Use Gradle test classpath or an IDE to run this class.
- **Example**: `java ExcelToCsvTalukMerger <excel_path> <input_csv> <output_csv>`

### 5. PostalCodeSplitter
Specifically designed to split the master JSON into Git-friendly 40MB chunks.

## Why these exist?
According to the **Enterprise Backend Standards**, we must avoid uploading files larger than 100MB to Git. These scripts allow us to store compressed or chunked data while keeping the full source available for the Seeder.
