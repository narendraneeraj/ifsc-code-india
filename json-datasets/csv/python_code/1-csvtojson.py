import csv
import json
import os

def convert_folder_csv_to_json(input_folder, output_folder):
    # Create the output folder if it doesn't already exist
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"Created output directory: '{output_folder}'")

    # Get all .csv files from the input folder
    csv_files = [f for f in os.listdir(input_folder) if f.lower().endswith('.csv')]

    if not csv_files:
        print(f"No CSV files found in '{input_folder}'.")
        return

    print(f"Found {len(csv_files)} CSV file(s) to convert...\n")

    for file_name in csv_files:
        csv_file_path = os.path.join(input_folder, file_name)
        
        # Keep the exact same filename, replacing .csv with .json
        json_file_name = os.path.splitext(file_name)[0] + '.json'
        json_file_path = os.path.join(output_folder, json_file_name)

        data = []

        # Read CSV file using 'utf-8-sig' to automatically handle BOM headers
        with open(csv_file_path, mode='r', encoding='utf-8-sig') as csv_file:
            # DictReader uses the exact CSV headers as keys for each JSON object
            csv_reader = csv.DictReader(csv_file)
            for row in csv_reader:
                data.append(row)

        # Write data to the JSON file with the same filename
        with open(json_file_path, mode='w', encoding='utf-8') as json_file:
            json.dump(data, json_file, indent=4, ensure_ascii=False)

        print(f"✅ Converted: '{file_name}' ➔ '{json_file_name}' ({len(data)} records)")

if __name__ == "__main__":
    # Define folder paths (Update these folder paths as needed)
    INPUT_FOLDER = r"D:\GITHUB\ifsc-code-india\json-datasets\csv\python_code"
    OUTPUT_FOLDER = r"D:\GITHUB\ifsc-code-india\json-datasets\csv\python_code"

    convert_folder_csv_to_json(INPUT_FOLDER, OUTPUT_FOLDER)