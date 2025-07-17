#!/usr/bin/env python3
"""
Fill empty English translations with exact matches from already translated German text.
This script:
1. Scans all JSON files in manual-work/ directory
2. Builds a dictionary of German -> English translations for exact matches
3. For each empty English translation, looks for exact German matches
4. If multiple English translations exist for the same German text, asks user to choose
5. Fills empty translations with the chosen exact matches
6. Never overwrites existing non-empty translations
"""

import os
import json
from pathlib import Path
from typing import Dict, Set, List, Tuple
from collections import defaultdict

# Threshold for detecting potentially bad translations
# If English text is less than 40% of German text length, flag it
THRESHOLD = 0.4


def load_all_translations(directory: str) -> Dict[str, Dict[int, Dict[str, str]]]:
    """
    Load all translations from JSON files in the directory.
    Returns a dictionary mapping filename to translation data.
    """
    all_translations = {}
    
    if not os.path.exists(directory):
        return all_translations
    
    for filename in os.listdir(directory):
        if not filename.endswith('.json'):
            continue
            
        file_path = os.path.join(directory, filename)
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                # Convert string keys to integers
                all_translations[filename] = {int(k): v for k, v in data.items()}
        except (json.JSONDecodeError, Exception) as e:
            print(f"Warning: Could not load {filename}: {e}")
            continue
    
    return all_translations


def build_german_to_english_mapping(all_translations: Dict[str, Dict[int, Dict[str, str]]]) -> Dict[str, Set[str]]:
    """
    Build a mapping from German text to all possible English translations.
    Only includes entries where both German and English are non-empty.
    """
    german_to_english = defaultdict(set)
    
    for filename, translations in all_translations.items():
        for order_num, entry in translations.items():
            german = entry.get('german', '').strip()
            english = entry.get('english', '').strip()
            
            # Only consider entries where both German and English exist
            if german and english:
                german_to_english[german].add(english)
    
    return dict(german_to_english)


def get_user_choice_for_multiple_translations(german_text: str, english_options: List[str]) -> str:
    """
    Ask user to choose between multiple English translations for the same German text.
    Returns the chosen English translation.
    """
    print(f"\nMultiple English translations found for German text:")
    print(f"German: {german_text}")
    print("\nOptions:")
    print(f"  1. Provide custom translation")
    print(f"  2. Skip this text")
    for i, english in enumerate(english_options, 1):
        print(f"  {i + 2}. {english}")
    
    total_options = len(english_options) + 2
    
    while True:
        try:
            choice = input(f"\nChoose option (1-{total_options}): ").strip()
            choice_num = int(choice)
            if choice_num == 1:
                # Custom translation
                custom_translation = input("Enter your custom English translation: ").strip()
                if custom_translation:
                    return custom_translation
                else:
                    print("Empty translation provided, please try again.")
            elif choice_num == 2:
                # Skip
                return ""
            elif 3 <= choice_num <= total_options:
                # Existing translation option
                return english_options[choice_num - 3]
            else:
                print(f"Please enter a number between 1 and {total_options}")
        except ValueError:
            print("Please enter a valid number")
        except KeyboardInterrupt:
            print("\nOperation cancelled by user")
            return ""


def fill_exact_matches(directory: str):
    """
    Fill empty English translations with exact matches from already translated text.
    """
    print("Loading all translations...")
    all_translations = load_all_translations(directory)
    
    if not all_translations:
        print("No translation files found!")
        return
    
    print("Building German-to-English mapping...")
    german_to_english = build_german_to_english_mapping(all_translations)
    
    print(f"Found {len(german_to_english)} unique German texts with translations")
    
    # Track user choices for consistent application
    user_choices = {}  # german_text -> chosen_english
    
    # Statistics
    total_filled = 0
    files_modified = set()
    
    # Process each file
    for filename in sorted(all_translations.keys()):
        print(f"\nProcessing {filename}...")
        
        # Reload file data to get latest state
        file_path = os.path.join(directory, filename)
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                translations = json.load(f)
                translations = {int(k): v for k, v in translations.items()}
        except Exception as e:
            print(f"✗ Error loading {filename}: {e}")
            continue
        
        file_filled_count = 0
        
        for order_num, entry in translations.items():
            german = entry.get('german', '').strip()
            english = entry.get('english', '').strip()
            
            # Only process entries with German text but empty English
            if german and not english:
                if german in german_to_english:
                    english_options = list(german_to_english[german])
                    
                    if len(english_options) == 1:
                        # Single option - use it directly
                        chosen_english = english_options[0]
                    elif german in user_choices:
                        # User already made a choice for this German text
                        chosen_english = user_choices[german]
                    else:
                        # Multiple options - ask user
                        chosen_english = get_user_choice_for_multiple_translations(german, english_options)
                        if chosen_english:
                            user_choices[german] = chosen_english
                        else:
                            continue  # User cancelled or chose to skip
                    
                    # Fill the translation and save immediately
                    if chosen_english:
                        entry['english'] = chosen_english
                        file_filled_count += 1
                        total_filled += 1
                        
                        # Save file immediately after each change
                        try:
                            with open(file_path, 'w', encoding='utf-8') as f:
                                json.dump(translations, f, ensure_ascii=False, indent=2, sort_keys=False)
                            print(f"✓ Saved {filename}: filled translation #{file_filled_count} (total: {total_filled})")
                            files_modified.add(filename)
                        except Exception as e:
                            print(f"✗ Error saving {filename}: {e}")
                            return  # Stop on save error
    
    print(f"\n" + "="*50)
    print(f"Summary:")
    print(f"- Files modified: {len(files_modified)}")
    print(f"- Total translations filled: {total_filled}")
    print(f"- User choices made: {len(user_choices)}")
    
    if user_choices:
        print(f"\nUser choices applied:")
        for german, english in user_choices.items():
            print(f"  '{german}' -> '{english}'")


def check_translation_inconsistencies(directory: str):
    """
    Check for German texts that have been translated differently across files.
    """
    print("Loading all translations...")
    all_translations = load_all_translations(directory)
    
    if not all_translations:
        print("No translation files found!")
        return
    
    print("Building German-to-English mapping...")
    german_to_english = build_german_to_english_mapping(all_translations)
    
    # Find inconsistencies
    inconsistencies = {}
    for german, english_set in german_to_english.items():
        if len(english_set) > 1:
            # Find which files contain these translations
            file_mappings = []
            for filename, translations in all_translations.items():
                for order_num, entry in translations.items():
                    if entry.get('german', '').strip() == german:
                        english = entry.get('english', '').strip()
                        if english:  # Only consider non-empty English translations
                            file_mappings.append((filename, order_num, english))
            
            if file_mappings:
                inconsistencies[german] = {
                    'english_options': list(english_set),
                    'file_mappings': file_mappings
                }
    
    if inconsistencies:
        print(f"\n" + "="*60)
        print(f"TRANSLATION INCONSISTENCIES FOUND")
        print(f"="*60)
        print(f"Found {len(inconsistencies)} German texts with multiple English translations:\n")
        
        for i, (german, data) in enumerate(inconsistencies.items(), 1):
            print(f"{i}. German: {german}")
            print(f"   English options: {data['english_options']}")
            print(f"   Found in:")
            for filename, order_num, english in data['file_mappings']:
                print(f"     - {filename}:{order_num} -> '{english}'")
            print()
            
            if i >= 10:  # Limit to first 10 for readability
                remaining = len(inconsistencies) - 10
                if remaining > 0:
                    print(f"   ... and {remaining} more inconsistencies")
                break
        
        print(f"Recommendation: Review these translations for consistency.")
        return inconsistencies
    else:
        print("✓ No translation inconsistencies found!")
        return {}


def resolve_inconsistencies(directory: str, inconsistencies: Dict):
    """
    Allow user to resolve translation inconsistencies by choosing a canonical translation.
    """
    if not inconsistencies:
        print("No inconsistencies to resolve!")
        return
    
    print(f"\nResolving {len(inconsistencies)} translation inconsistencies...")
    print("For each inconsistency, choose the correct translation to use everywhere.")
    
    def apply_canonical_choice(german_text: str, chosen_english: str) -> int:
        """
        Apply canonical choice immediately to all affected files and save them.
        Returns number of files changed.
        """
        files_changed = 0
        for filename, order_num, current_english in inconsistencies[german_text]['file_mappings']:
            if current_english == chosen_english:
                continue  # Already correct
            
            file_path = os.path.join(directory, filename)
            try:
                # Load current file state
                with open(file_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    data = {int(k): v for k, v in data.items()}
                
                # Check if someone else already fixed this
                if data[order_num]["english"].strip() == chosen_english:
                    continue
                
                # Update and save immediately
                data[order_num]["english"] = chosen_english
                with open(file_path, 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2, sort_keys=False)
                
                print(f"  ✓ Updated {filename}:{order_num}")
                files_changed += 1
                
            except Exception as e:
                print(f"  ✗ Could not update {filename}:{order_num} - {e}")
                # Continue with other files
        
        return files_changed
    
    total_resolved = 0
    
    for i, (german, data) in enumerate(inconsistencies.items(), 1):
        print(f"\n" + "-"*60)
        print(f"Inconsistency {i}/{len(inconsistencies)}")
        print(f"GER: {german}")
        
        # Show English translations compactly
        english_options = data['english_options']
        for j, english in enumerate(english_options, 1):
            option_num = j + 2  # Start at 3 to match button numbers
            print(f"EN{option_num}: {english}")
        
        print(f"\nPick an option:")
        print(f"  1. Provide custom translation")
        print(f"  2. Skip this inconsistency")
        for j, english in enumerate(english_options, 1):
            option_num = j + 2  # Start at 3 to match button numbers
            matching_files = [f"{fm[0]}:{fm[1]}" for fm in data['file_mappings'] if fm[2] == english]
            print(f"  {option_num}. EN{option_num} : {english}")
            print(f"       Found in: {', '.join(matching_files)}")
        
        total_options = len(english_options) + 2
        
        # Get user choice
        while True:
            try:
                choice = input(f"\nChoose canonical translation (1-{total_options}): ").strip()
                choice_num = int(choice)
                
                if choice_num == 1:
                    custom = input("Enter custom translation: ").strip()
                    if custom:
                        files_changed = apply_canonical_choice(german, custom)
                        print(f"  ✓ Applied to {files_changed} file(s)")
                        total_resolved += 1
                        # Clear terminal for next inconsistency
                        os.system('cls' if os.name == 'nt' else 'clear')
                        break
                    else:
                        print("Empty translation provided, please try again.")
                elif choice_num == 2:
                    # Clear terminal even when skipping
                    os.system('cls' if os.name == 'nt' else 'clear')
                    break  # Skip this inconsistency
                elif 3 <= choice_num <= total_options:
                    chosen_english = english_options[choice_num - 3]
                    files_changed = apply_canonical_choice(german, chosen_english)
                    print(f"  ✓ Applied to {files_changed} file(s)")
                    total_resolved += 1
                    # Clear terminal for next inconsistency
                    os.system('cls' if os.name == 'nt' else 'clear')
                    break
                else:
                    print(f"Please enter a number between 1 and {total_options}")
            except ValueError:
                print("Please enter a valid number")
            except KeyboardInterrupt:
                print(f"\nOperation cancelled - progress up to this point is saved.")
                print(f"Resolved {total_resolved} inconsistencies before stopping.")
                return
    
    print(f"\n" + "="*50)
    print(f"Inconsistency Resolution Summary:")
    print(f"- Total inconsistencies resolved: {total_resolved}")
    print("All changes have been saved immediately.")


def find_suspicious_translations(directory: str, threshold: float = THRESHOLD) -> Dict[str, Dict]:
    """
    Find translations where English text is suspiciously short compared to German.
    Returns mapping: german_text -> {
        'bad_entries': [(filename, order_num, current_english)],
        'english_options': list[str]  # other longer variants available
    }
    """
    all_translations = load_all_translations(directory)
    german_to_english = build_german_to_english_mapping(all_translations)
    
    suspicious = {}
    
    for filename, translations in all_translations.items():
        for order_num, entry in translations.items():
            german = entry.get('german', '').strip()
            english = entry.get('english', '').strip()
            
            if not german or not english:
                continue
                
            # Check if English is suspiciously short
            if len(english) < threshold * len(german):
                if german not in suspicious:
                    suspicious[german] = {
                        'bad_entries': [],
                        'english_options': sorted(german_to_english.get(german, []))
                    }
                suspicious[german]['bad_entries'].append((filename, order_num, english))
    
    return suspicious


def fix_suspicious_translations(directory: str, suspicious: Dict[str, Dict]):
    """
    Interactively fix suspiciously short translations with better variants.
    """
    if not suspicious:
        print("✓ No suspiciously short translations found!")
        return
    
    print(f"\nFound {len(suspicious)} German texts with suspiciously short English translations.")
    print("For each suspicious translation, choose a better alternative.")
    
    def apply_choice(german_text: str, new_english: str) -> int:
        """Apply chosen translation to all affected files and save immediately."""
        files_changed = 0
        for filename, order_num, current_english in suspicious[german_text]['bad_entries']:
            file_path = os.path.join(directory, filename)
            try:
                # Load current file state
                with open(file_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    data = {int(k): v for k, v in data.items()}
                
                # Check if someone else already fixed this
                if data[order_num]["english"].strip() == new_english:
                    continue
                
                # Update and save immediately
                data[order_num]["english"] = new_english
                with open(file_path, 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2, sort_keys=False)
                
                print(f"  ✓ Updated {filename}:{order_num}")
                files_changed += 1
                
            except Exception as e:
                print(f"  ✗ Could not update {filename}:{order_num} - {e}")
                # Continue with other files
        
        return files_changed
    
    total_resolved = 0
    
    for i, (german, data) in enumerate(suspicious.items(), 1):
        print(f"\n" + "-"*60)
        print(f"Suspicious translation {i}/{len(suspicious)}")
        print(f"GER: {german}")
        
        # Show current short translation(s)
        print(f"\nCurrent short translation(s):")
        for filename, order_num, short_english in data['bad_entries']:
            ratio = len(short_english) / len(german)
            print(f"  {filename}:{order_num} -> '{short_english}' (ratio: {ratio:.2f})")
        
        # Show available longer alternatives
        english_options = data['english_options']
        if english_options:
            print(f"\nAvailable longer alternatives:")
            for j, english in enumerate(english_options, 1):
                option_num = j + 2  # Start at 3 to match button numbers
                ratio = len(english) / len(german)
                print(f"EN{option_num}: {english} (ratio: {ratio:.2f})")
        
        print(f"\nPick an option:")
        print(f"  1. Provide custom translation")
        print(f"  2. Skip this text")
        for j, english in enumerate(english_options, 1):
            option_num = j + 2  # Start at 3 to match button numbers
            print(f"  {option_num}. EN{option_num} : {english}")
        
        total_options = len(english_options) + 2
        
        # Get user choice
        while True:
            try:
                choice = input(f"\nChoose option (1-{total_options}): ").strip()
                choice_num = int(choice)
                
                if choice_num == 1:
                    custom = input("Enter custom translation: ").strip()
                    if custom:
                        files_changed = apply_choice(german, custom)
                        print(f"  ✓ Applied to {files_changed} file(s)")
                        total_resolved += 1
                        # Clear terminal for next suspicious translation
                        os.system('cls' if os.name == 'nt' else 'clear')
                        break
                    else:
                        print("Empty translation provided, please try again.")
                elif choice_num == 2:
                    # Clear terminal even when skipping
                    os.system('cls' if os.name == 'nt' else 'clear')
                    break  # Skip this suspicious translation
                elif 3 <= choice_num <= total_options:
                    chosen_english = english_options[choice_num - 3]
                    files_changed = apply_choice(german, chosen_english)
                    print(f"  ✓ Applied to {files_changed} file(s)")
                    total_resolved += 1
                    # Clear terminal for next suspicious translation
                    os.system('cls' if os.name == 'nt' else 'clear')
                    break
                else:
                    print(f"Please enter a number between 1 and {total_options}")
            except ValueError:
                print("Please enter a valid number")
            except KeyboardInterrupt:
                print(f"\nOperation cancelled - progress up to this point is saved.")
                print(f"Fixed {total_resolved} suspicious translations before stopping.")
                return
    
    print(f"\n" + "="*50)
    print(f"Suspicious Translation Fix Summary:")
    print(f"- Total suspicious translations fixed: {total_resolved}")
    print("All changes have been saved immediately.")


def preview_exact_matches(directory: str):
    """
    Preview what exact matches would be filled without actually modifying files.
    """
    print("Loading all translations...")
    all_translations = load_all_translations(directory)
    
    if not all_translations:
        print("No translation files found!")
        return
    
    print("Building German-to-English mapping...")
    german_to_english = build_german_to_english_mapping(all_translations)
    
    print(f"Found {len(german_to_english)} unique German texts with translations\n")
    
    # Statistics
    total_empty = 0
    total_fillable = 0
    needs_user_choice = 0
    
    # Preview each file
    for filename in sorted(all_translations.keys()):
        translations = all_translations[filename]
        file_empty_count = 0
        file_fillable_count = 0
        file_needs_choice = 0
        
        for order_num, entry in translations.items():
            german = entry.get('german', '').strip()
            english = entry.get('english', '').strip()
            
            # Count entries with German text but empty English
            if german and not english:
                file_empty_count += 1
                total_empty += 1
                
                if german in german_to_english:
                    file_fillable_count += 1
                    total_fillable += 1
                    
                    if len(german_to_english[german]) > 1:
                        file_needs_choice += 1
                        needs_user_choice += 1
        
        if file_empty_count > 0:
            print(f"{filename}:")
            print(f"  - Empty translations: {file_empty_count}")
            print(f"  - Can be filled: {file_fillable_count}")
            print(f"  - Need user choice: {file_needs_choice}")
    
    print(f"\n" + "="*50)
    print(f"Overall statistics:")
    print(f"- Total empty translations: {total_empty}")
    print(f"- Can be filled with exact matches: {total_fillable}")
    print(f"- Need user choice (multiple options): {needs_user_choice}")
    
    # Show some examples of German texts that need user choice
    if needs_user_choice > 0:
        print(f"\nExamples of German texts with multiple English options:")
        count = 0
        for german, english_set in german_to_english.items():
            if len(english_set) > 1:
                print(f"  '{german}' -> {list(english_set)}")
                count += 1
                if count >= 5:  # Show max 5 examples
                    break


def main():
    """Main function to execute the exact match filling."""
    # Define paths
    base_dir = Path(__file__).parent
    manual_work_dir = base_dir / "manual-work"
    
    if not manual_work_dir.exists():
        print(f"Directory {manual_work_dir} does not exist!")
        return
    
    print("Fill Exact Matches - Translation Helper")
    print("="*50)
    print(f"Working directory: {manual_work_dir}")
    print()
    
    # Ask user what they want to do
    while True:
        print("Options:")
        print("1. Preview what would be filled (no changes)")
        print("2. Fill exact matches (modify files)")
        print("3. Check for translation inconsistencies")
        print("4. Fix suspiciously short translations")
        print("5. Exit")
        
        try:
            choice = input("\nChoose option (1-5): ").strip()
            
            if choice == "1":
                print("\n" + "="*50)
                print("PREVIEW MODE - No files will be modified")
                print("="*50)
                preview_exact_matches(str(manual_work_dir))
                break
            elif choice == "2":
                print("\n" + "="*50)
                print("FILL MODE - Files will be modified")
                print("="*50)
                confirm = input("Are you sure you want to modify files? (y/N): ").strip().lower()
                if confirm in ('y', 'yes'):
                    fill_exact_matches(str(manual_work_dir))
                else:
                    print("Operation cancelled")
                break
            elif choice == "3":
                print("\n" + "="*50)
                print("INCONSISTENCY CHECK MODE")
                print("="*50)
                inconsistencies = check_translation_inconsistencies(str(manual_work_dir))
                
                if inconsistencies:
                    print("\nWould you like to resolve these inconsistencies?")
                    resolve_choice = input("(y/N): ").strip().lower()
                    if resolve_choice in ('y', 'yes'):
                        resolve_inconsistencies(str(manual_work_dir), inconsistencies)
                break
            elif choice == "4":
                print("\n" + "="*50)
                print("SUSPICIOUS TRANSLATION CHECK MODE")
                print("="*50)
                suspicious = find_suspicious_translations(str(manual_work_dir))
                
                if suspicious:
                    print("\nWould you like to fix these suspicious translations?")
                    fix_choice = input("(y/N): ").strip().lower()
                    if fix_choice in ('y', 'yes'):
                        fix_suspicious_translations(str(manual_work_dir), suspicious)
                break
            elif choice == "5":
                print("Goodbye!")
                return
            else:
                print("Please enter 1, 2, 3, 4, or 5")
        except KeyboardInterrupt:
            print("\nGoodbye!")
            return


if __name__ == "__main__":
    main()
