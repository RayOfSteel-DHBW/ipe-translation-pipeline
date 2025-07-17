#!/usr/bin/env python3
"""
Upsert German text from .work/step-2/ with existing English translations from manual-work/
into JSON dictionaries. This script:
1. Reads existing translations from manual-work/ JSON files
2. Updates German text from step-2 files
3. Preserves existing English translations where possible
4. Adds new entries for new German text
5. Handles renumbering gracefully
"""

import os
import json
import re
from pathlib import Path
from typing import Dict, Set, Tuple, List  # +List
from difflib import SequenceMatcher


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


def load_existing_translations(json_path: str) -> Dict[int, Dict[str, str]]:
    """
    Load existing translations from a JSON file.
    Returns a dictionary mapping order numbers to {german, english} dicts.
    """
    if not os.path.exists(json_path):
        return {}
    
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            # Convert string keys to integers
            return {int(k): v for k, v in data.items()}
    except (json.JSONDecodeError, Exception) as e:
        print(f"Warning: Could not load existing translations from {json_path}: {e}")
        return {}


def similarity(a: str, b: str) -> float:
    """Calculate similarity between two strings (0.0 to 1.0)."""
    return SequenceMatcher(None, a.lower().strip(), b.lower().strip()).ratio()


def find_best_match(new_german: str, existing_translations: Dict[int, Dict[str, str]], 
                   used_matches: Set[int], similarity_threshold: float = 0.9) -> Tuple[int, float]:
    """
    Find the best matching existing translation for a new German text.
    Returns (order_number, similarity_score) or (-1, 0.0) if no good match found.
    """
    best_match = -1
    best_score = 0.0
    
    for order_num, translation in existing_translations.items():
        if order_num in used_matches:
            continue
            
        existing_german = translation.get('german', '')
        score = similarity(new_german, existing_german)
        
        if score > best_score and score >= similarity_threshold:
            best_score = score
            best_match = order_num
    
    return best_match, best_score


def get_txt_files(directory: str) -> Set[str]:
    """Get all .txt filenames from a directory."""
    txt_files = set()
    if os.path.exists(directory):
        for filename in os.listdir(directory):
            if filename.endswith('.txt'):
                txt_files.add(filename)
    return txt_files


def upsert_translations(step2_dir: str, output_dir: str):
    """
    Upsert German texts from step-2 into existing JSON files, preserving English translations.
    """
    # Ensure output directory exists
    os.makedirs(output_dir, exist_ok=True)
    
    # Get all txt files from step-2 (these are our source files)
    step2_files = get_txt_files(step2_dir)
    
    for filename in sorted(step2_files):
        # First check if the step-2 file has markers
        step2_path = os.path.join(step2_dir, filename)
        if not has_markers(step2_path):
            continue
            
        # Parse new German content from step-2
        new_german_content = parse_file_content(step2_path)
        
        # Skip if no German content was parsed
        if not new_german_content:
            continue
        
        # Load existing translations
        json_filename = filename.replace('.txt', '.json')
        json_path = os.path.join(output_dir, json_filename)
        existing_translations = load_existing_translations(json_path)
        
        # Create merged dictionary with intelligent matching
        merged_dict = {}
        used_matches = set()
        
        # Process new German content in order
        for order_num_str in sorted(new_german_content.keys(), key=int):
            new_order_num = int(order_num_str)
            new_german_text = new_german_content[order_num_str]
            
            # Try to find a matching existing translation
            best_match, match_score = find_best_match(
                new_german_text, existing_translations, used_matches
            )
            
            if best_match != -1:
                # Found a good match - preserve the English translation
                existing_translation = existing_translations[best_match]
                english_text = existing_translation.get('english', '')
                used_matches.add(best_match)
            else:
                # No match found - new entry with empty English
                english_text = ""
            
            merged_dict[new_order_num] = {
                "german": new_german_text,
                "english": english_text
            }
        
        # ------------------------------------------------------------------
        # Handle existing translations that could not be matched by content
        unmatched_added: List[int] = []
        id_conflicts: List[int] = []

        for order_num, translation in existing_translations.items():
            if order_num in used_matches:
                continue  # was matched already
            if order_num not in merged_dict:
                merged_dict[order_num] = translation          # keep as-is
                unmatched_added.append(order_num)
            else:
                id_conflicts.append(order_num)                # id taken, can't add
        # ------------------------------------------------------------------
        
        # Create / overwrite JSON file
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(merged_dict, f, ensure_ascii=False, indent=2, sort_keys=False)
        
        preserved_count = len([v for v in merged_dict.values() if v['english'].strip()])
        new_count = len([v for v in merged_dict.values() if not v['english'].strip()])
        
        print(f"✓ Updated {json_filename}: {len(merged_dict)} entries "
              f"({preserved_count} preserved, {new_count} new)")
        if unmatched_added:
            print(f"  ⚠ Added {len(unmatched_added)} unmatched existing translations (ids: {unmatched_added})")
        if id_conflicts:
            print(f"  ⚠ Skipped {len(id_conflicts)} unmatched translations due to id conflict (ids: {id_conflicts})")


def main():
    """Main function to execute the translation upsert."""
    # Define paths
    base_dir = Path(__file__).parent
    step2_dir = base_dir / ".work" / "step-2"
    output_dir = base_dir / "manual-work"
    
    print("Upserting German texts while preserving English translations...")
    print(f"Source (German): {step2_dir}")
    print(f"Output directory: {output_dir}")
    print("-" * 50)
    
    upsert_translations(str(step2_dir), str(output_dir))
    
    print("-" * 50)
    print("Upsert completed!")


if __name__ == "__main__":
    main()
