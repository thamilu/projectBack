import csv
import json

csv_file = r'G:\Project\changes\57-60.csv'
json_file = r'g:\Project\eshop_back\src\main\resources\seed\postal_codes.json'

data = []
with open(csv_file, mode='r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        # Map CSV columns to JSON fields
        # ID,pinCode,postOfficeName,districtName,localityName,stateName,countryName,phoneCode,talukName
        json_row = {
            "isoCode": "IN", # Defaulting to IN as it's not in CSV
            "countryName": row.get("countryName", "India"),
            "phoneCode": row.get("phoneCode") if row.get("phoneCode") else "+91",
            "stateName": row.get("stateName"),
            "stateCode": "", # Will be resolved by seeder
            "districtName": row.get("districtName"),
            "talukName": row.get("talukName"),
            "pinCode": row.get("pinCode"),
            "localityName": row.get("localityName"),
            "postOfficeName": row.get("postOfficeName")
        }
        data.append(json_row)

with open(json_file, mode='w', encoding='utf-8') as f:
    json.dump(data, f, indent=2)

print(f"Successfully converted {len(data)} rows to {json_file}")
