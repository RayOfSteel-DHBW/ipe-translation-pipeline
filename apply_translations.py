#!/usr/bin/env python3
"""
Apply English translations from JSON files in manual-work/ directory
back to the placeholdered files from step-2, writing results to step-4.
"""

import os
import json
import re
import shutil
import html
from pathlib import Path
from typing import Dict, Set


def load_translations(json_file_path: str) -> Dict[str, str]:
    """
    Load translations from a JSON file.
    Returns a dictionary mapping order number (as string) to escaped English text.
    Only includes entries where English translation is not empty.
    """
    translations: Dict[str, str] = {}
    
    if not os.path.exists(json_file_path):
        return translations
    
    try:
        with open(json_file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            
        for order_num, entry in data.items():
            if isinstance(entry, dict) and 'english' in entry:
                german_text  = (entry.get('german')  or '').strip()  # keep for log preview
                english_text = (entry.get('english') or '').strip()

                if not english_text:
                    preview = german_text[:40] + ('…' if len(german_text) > 40 else '')
                    print(f"  – No English translation for item {order_num}: \"{preview}\"")
                    continue
                translations[str(order_num)] = html.escape(english_text, quote=False)
                    
    except (json.JSONDecodeError, Exception) as e:
        print(f"Warning: Could not load translations from {json_file_path}: {e}")
    
    return translations


def apply_translations_to_file(input_file_path: str, output_file_path: str, translations: Dict[str, str]) -> int:
    """
    Replace placeholders {{123}} or @PLACEHOLDER(123)@ with the English text.
    """
    if not os.path.exists(input_file_path):
        return 0
    
    applied_count = 0
    
    try:
        with open(input_file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Pattern for placeholders, e.g. {{123}}
        placeholder_re = re.compile(r'(?:@PLACEHOLDER\((\d+)\)@|\{\{(\d+)\}\})')

        def repl(match: re.Match) -> str:
            oid = match.group(1) or match.group(2)     # pick whichever matched
            if oid in translations:
                nonlocal applied_count
                applied_count += 1
                return translations[oid]
            return match.group(0)

        content = placeholder_re.sub(repl, content)
        
        # Write the result to output file
        os.makedirs(os.path.dirname(output_file_path), exist_ok=True)
        with open(output_file_path, 'w', encoding='utf-8') as f:
            f.write(content)
            
    except Exception as e:
        print(f"Error processing {input_file_path}: {e}")
        return 0
    
    return applied_count


def copy_xml_files(step2_dir: str, step4_dir: str):
    """
    Copy all XML files from step-2 to step-4 (these don't need translation processing).
    """
    if not os.path.exists(step2_dir):
        return
    
    os.makedirs(step4_dir, exist_ok=True)
    
    for filename in os.listdir(step2_dir):
        if filename.endswith('.xml'):
            src_path = os.path.join(step2_dir, filename)
            dst_path = os.path.join(step4_dir, filename)
            shutil.copy2(src_path, dst_path)
            print(f"  → Copied {filename}")


def apply_translations(manual_work_dir: str, step2_dir: str, step4_dir: str):
    """
    Apply all translations from manual-work JSON files to step-2 files,
    writing results to step-4. The matching rule is:
        •  <name>.json   ↔  <name>.xml   (Ipe slides)
    """
    os.makedirs(step4_dir, exist_ok=True)

    # 1. copy every XML that has no matching JSON (keeps old behaviour)
    for fname in os.listdir(step2_dir):
        if fname.endswith(".xml") and not Path(manual_work_dir, fname[:-4] + ".json").exists():
            shutil.copy2(Path(step2_dir, fname), Path(step4_dir, fname))

    # 2. iterate over all JSONs
    json_files = [f for f in os.listdir(manual_work_dir) if f.endswith(".json")]
    if not json_files:
        print("No JSON files found in manual-work directory")
        return

    total, processed = 0, 0
    for jname in sorted(json_files):
        base = jname[:-5]                               # file stem without “.json”
        translations = load_translations(Path(manual_work_dir, jname))
        if not translations:
            # still copy verbatim if no translations were supplied
            src = Path(step2_dir, base + ".xml")
            dst = Path(step4_dir, base + ".xml")
            if src.exists():
                shutil.copy2(src, dst)
            continue

        print(f"Processing {jname} …")
        for ext in (".xml",):          # only process XML now
            src = Path(step2_dir,  base + ext)
            dst = Path(step4_dir, base + ext)
            if not src.exists():
                continue
            n = apply_translations_to_file(str(src), str(dst), translations)
            print(f"  → {n:3} placeholders applied to {src.name}")
            total += n
        processed += 1

    print(f"\nSummary:\n  Files processed: {processed}\n  Total translations applied: {total}")


def main():
    """Main function to execute the translation application."""
    # Define paths
    base_dir = Path(__file__).parent
    manual_work_dir = base_dir / "manual-work"
    step2_dir = base_dir / ".work" / "step-2"
    step4_dir = base_dir / ".work" / "step-4"
    
    print("Applying translations from manual-work to step-2 files...")
    print(f"Source translations: {manual_work_dir}")
    print(f"Source files: {step2_dir}")
    print(f"Output directory: {step4_dir}")
    print("-" * 60)
    
    apply_translations(str(manual_work_dir), str(step2_dir), str(step4_dir))
    
    print("-" * 60)
    print("Translation application completed!")


if __name__ == "__main__":
    main()
    manual_work_dir = base_dir / "manual-work"
    step2_dir = base_dir / ".work" / "step-2"
    step4_dir = base_dir / ".work" / "step-4"
    
    print("Applying translations from manual-work to step-2 files...")
    print(f"Source translations: {manual_work_dir}")
    print(f"Source files: {step2_dir}")
    print(f"Output directory: {step4_dir}")
    print("-" * 60)
    
    apply_translations(str(manual_work_dir), str(step2_dir), str(step4_dir))
    
    print("-" * 60)
    print("Translation application completed!")


if __name__ == "__main__":
    main()
if __name__ == "__main__":
    main()
