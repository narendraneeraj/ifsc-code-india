#  input file csv_file_path = 'bank.csv' change the file name 
#   output file json_file_path = 'bank.json' change the file name 

import csv
import json

def csv_to_json(csv_file_path, json_file_path):
    # Initialize an empty list to store row data
    data = []
    
    # Read the CSV file
    with open(csv_file_path, mode='r', encoding='utf-8') as csv_file:
        # Use DictReader to parse CSV rows into dictionaries using headers as keys
        csv_reader = csv.DictReader(csv_file)
        
        for row in csv_reader:
            data.append(row)
            
    # Write the data list to a JSON file
    with open(json_file_path, mode='w', encoding='utf-8') as json_file:
        # indent=4 formats the JSON file with indentation for human readability
        json.dump(data, json_file, indent=4)

# Example usage:
if __name__ == "__main__":
    csv_file_path = 'banks.csv'
    json_file_path = 'banks.json'
    
    csv_to_json(csv_file_path, json_file_path)
    print(f"Successfully converted '{csv_file_path}' to '{json_file_path}'.")