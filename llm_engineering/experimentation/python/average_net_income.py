#!/usr/bin/env python3
"""
Script to parse test.csv and calculate the average of all Net_Income entries.
"""

import csv
import os

# Get the path to test.csv (one directory up from python/)
script_dir = os.path.dirname(os.path.abspath(__file__))
csv_file = os.path.join(os.path.dirname(script_dir), "test.csv")

# Read CSV and extract Net_Income values
net_income_values = []

try:
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        
        for row in reader:
            # Extract Net_Income value and convert to float
            net_income = float(row['Net_Income'])
            net_income_values.append(net_income)
    
    # Calculate average
    if net_income_values:
        average = sum(net_income_values) / len(net_income_values)
        print(f"Number of entries: {len(net_income_values)}")
        print(f"Net Income values: {net_income_values}")
        print(f"\nAverage Net Income: ${average:,.2f}")
    else:
        print("No Net_Income entries found in the CSV file.")
        
except FileNotFoundError:
    print(f"Error: Could not find {csv_file}")
except KeyError:
    print("Error: 'Net_Income' column not found in CSV file.")
except ValueError as e:
    print(f"Error: Could not convert Net_Income to number. {e}")
except Exception as e:
    print(f"Error: {e}")
