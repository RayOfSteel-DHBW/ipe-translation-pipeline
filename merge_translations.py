#!/usr/bin/env python3
"""
Merge German text from .work/step-2/ with English translations from .work/step-3/
into JSON dictionaries. Creates one JSON file per txt file pair in manual-work/ folder.
"""

import os
import json
import re
from pathlib import Path
from typing import Dict, Set


def parse_file_content(file_path: str) -> Dict[str, str]:
    """
    Parse a file and extract entries in format @(number):text
    Returns a dictionary mapping order numbers to text content.
    """
    content_dict = {}
    
    if not os.path.exists(file_path):
        return content_dict
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line.startswith('@(') and '):' in line:
                    # Extract order number and text
                    match = re.match(r'@\((\d+)\):(.*)', line)
                    if match:
                        order_num = match.group(1)
                        text = match.group(2)
                        content_dict[order_num] = text
    except (UnicodeDecodeError, Exception):
        # Skip files that can't be read or have encoding issues
        pass
    
    return content_dict


def has_markers(file_path: str) -> bool:
    """
    Check if a file contains @(number): markers.
    Returns True if at least one marker is found, False otherwise.
    """
    if not os.path.exists(file_path):
        return False
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line.startswith('@(') and '):' in line:
                    return True
    except (UnicodeDecodeError, Exception):
        # Skip files that can't be read or have encoding issues
        pass
    
    return False


def get_txt_files(directory: str) -> Set[str]:
    """Get all .txt filenames from a directory."""
    txt_files = set()
    if os.path.exists(directory):
        for filename in os.listdir(directory):
            if filename.endswith('.txt'):
                txt_files.add(filename)
    return txt_files


def merge_translations(step2_dir: str, step3_dir: str, output_dir: str):
    """
    Merge German texts from step-2 with English translations from step-3.
    Creates JSON files in output_dir for each txt file found in step-2.
    """
    # Ensure output directory exists
    os.makedirs(output_dir, exist_ok=True)
    
    # Get all txt files from step-2 (these are our source files)
    step2_files = get_txt_files(step2_dir)
    
    for filename in sorted(step2_files):
        # First check if the step-2 file has markers
        step2_path = os.path.join(step2_dir, filename)
        if not has_markers(step2_path):
            print(f"Skipping {filename} (no @(number): markers found)")
            continue
            
        print(f"Processing {filename}...")
        
        # Parse German content from step-2
        german_content = parse_file_content(step2_path)
        
        # Parse English content from step-3 (if exists)
        step3_path = os.path.join(step3_dir, filename)
        english_content = parse_file_content(step3_path)
        
        # Skip if no German content was parsed
        if not german_content:
            print(f"  → Skipping {filename} (no parseable content found)")
            continue
        
        
        # Parse English content from step-3 (if exists)
        step3_path = os.path.join(step3_dir, filename)
        english_content = parse_file_content(step3_path)
        
        # Skip if no German content was parsed
        if not german_content:
            print(f"  → Skipping {filename} (no parseable content found)")
            continue
        
        # Create merged dictionary
        merged_dict = {}
        
        # Get all order numbers from German content and sort them numerically
        for order_num_str in sorted(german_content.keys(), key=int):
            order_num = int(order_num_str)  # Convert to integer for proper sorting
            german_text = german_content[order_num_str]
            english_text = english_content.get(order_num_str, "")  # Empty string if no translation
            
            merged_dict[order_num] = {
                "german": german_text,
                "english": english_text
            }
        
        # Create output JSON file
        json_filename = filename.replace('.txt', '.json')
        output_path = os.path.join(output_dir, json_filename)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(merged_dict, f, ensure_ascii=False, indent=2, sort_keys=False)
        
        print(f"  → Created {json_filename} with {len(merged_dict)} entries")
        if english_content:
            print(f"    (Found translations for {len(english_content)} entries)")
        else:
            print(f"    (No translation file found)")


def main():
    """Main function to execute the translation merge."""
    # Define paths
    base_dir = Path(__file__).parent
    step2_dir = base_dir / ".work" / "step-2"
    step3_dir = base_dir / ".work" / "step-3"
    output_dir = base_dir / "manual-work"
    
    print("Merging German texts with English translations...")
    print(f"Source (German): {step2_dir}")
    print(f"Translations (English): {step3_dir}")
    print(f"Output directory: {output_dir}")
    print("-" * 50)
    
    merge_translations(str(step2_dir), str(step3_dir), str(output_dir))
    
    print("-" * 50)
    print("Merge completed!")


if __name__ == "__main__":
    main()
