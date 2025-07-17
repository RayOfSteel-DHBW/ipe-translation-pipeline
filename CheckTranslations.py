import json
from pathlib import Path

# Threshold for detecting potentially bad translations
# If English text is less than 40% of German text length, flag it
THRESHOLD = 0.4

def scan_file(path: Path) -> list[dict]:
    """Scan a file and return problematic translations as a list of dictionaries."""
    bad_entries = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, entry in data.items():
            german = entry.get("german", "").strip()
            english = entry.get("english", "").strip()

            # Skip entirely if either text is empty
            if not german or not english:
                continue

            # Check if English translation is too short compared to German
            if len(english) < THRESHOLD * len(german):
                bad_entries.append({
                    "key": key,
                    "german": german,
                    "english": english,
                    "file": path.name
                })

        # Only log the file when problems were found
        if bad_entries:
            print(f"Scanning file: {path}")
            print(f"Found {len(data)} entries in {path.name}")
            print(f"Found {len(bad_entries)} problematic translations in {path.name}")
            print()
            
            # Log each problematic translation
            for entry in bad_entries:
                length_ratio = len(entry["english"]) / len(entry["german"])
                print(f"  Key: {entry['key']} (ratio: {length_ratio:.2f})")
                print(f"    German:  {entry['german']}")
                print(f"    English: {entry['english']}")
                print()
    except Exception as e:
        print(f"Error scanning {path}: {e}")

    return bad_entries


def main():
    """Main function to scan all JSON files in the manual-work directory."""
    manual_work_dir = Path("manual-work")
    
    if not manual_work_dir.exists():
        print(f"Directory {manual_work_dir} does not exist!")
        return
    
    print(f"Scanning JSON files in {manual_work_dir}...")
    print("-" * 50)
    
    all_bad_entries = []
    json_files = list(manual_work_dir.glob("*.json"))
    
    if not json_files:
        print("No JSON files found in manual-work directory!")
        return
    
    for json_file in json_files:
        bad_entries = scan_file(json_file)
        if bad_entries:
            all_bad_entries.extend(bad_entries)
            print()  # Add spacing between files
    
    print("-" * 50)
    print(f"Summary: Found {len(all_bad_entries)} problematic translations across {len(json_files)} files")
    
    if all_bad_entries:
        # Write problematic translations to a file
        with open("bad_translations.txt", "w", encoding="utf-8") as f:
            f.write("Problematic Translations Report\n")
            f.write("=" * 40 + "\n\n")
            
            current_file = None
            for entry in all_bad_entries:
                if entry.get("file") != current_file:
                    current_file = entry.get("file")
                    if current_file:
                        f.write(f"\nFile: {current_file}\n")
                        f.write("-" * 20 + "\n")
                
                f.write(f"Key: {entry['key']}\n")
                f.write(f"German: {entry['german']}\n")
                f.write(f"English: {entry['english']}\n")
                f.write(f"Length ratio: {len(entry['english'])/len(entry['german']):.2f}\n")
                f.write("\n")
        
        print(f"Detailed report saved to bad_translations.txt")


if __name__ == "__main__":
    main()