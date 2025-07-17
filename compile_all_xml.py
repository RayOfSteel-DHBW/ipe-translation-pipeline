#!/usr/bin/env python3
"""
Python equivalent of CompileStep.java - compiles all XML files from step-4 to PDFs.
This operates on all files instead of just one that is passed.
"""

import os
import json
import subprocess
import shutil
from pathlib import Path
from typing import List, Tuple


def load_configuration() -> dict:
    """
    Load configuration from settings.json file.
    """
    config = {}
    base_dir = Path(__file__).parent
    settings_file = base_dir / "src" / "main" / "resources" / "settings.json"
    
    try:
        with open(settings_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
    except (FileNotFoundError, json.JSONDecodeError) as e:
        print(f"Warning: Could not load settings.json: {e}")
        # Default configuration
        config = {
            "ipe_dir": "C:\\Program Files (x86)\\IPE\\bin",
            "working_directory": "C:\\Dev\\Repos\\Remotes\\JavaProject\\ipe-translation-pipeline\\.work",
            "clean_on_start": False
        }
    
    return config


def get_ipe2ipe_path(config: dict) -> str:
    """
    Get the path to ipetoipe.exe from configuration.
    """
    ipe_dir = config.get("ipe_dir", "C:\\Program Files (x86)\\IPE\\bin")
    return os.path.join(ipe_dir, "ipetoipe.exe")


def get_xml_files(input_dir: str) -> List[str]:
    """
    Get all .xml filenames from the input directory.
    """
    xml_files = []
    if os.path.exists(input_dir):
        for filename in os.listdir(input_dir):
            if filename.endswith('.xml'):
                xml_files.append(filename[:-4])  # Remove .xml extension
    return sorted(xml_files)


def compile_xml_to_pdf(xml_file_path: str, pdf_file_path: str, ipe2ipe_path: str) -> Tuple[bool, str]:
    """
    Compile a single XML file to PDF using ipe2ipe.
    Returns (success, error_message).
    """
    try:
        # Run ipe2ipe -pdf input.xml output.pdf
        cmd = [ipe2ipe_path, "-pdf", xml_file_path, pdf_file_path]
        
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=30  # 30 second timeout
        )
        
        if result.returncode == 0:
            return True, ""
        else:
            error_msg = f"ipe2ipe failed with exit code {result.returncode}"
            if result.stderr:
                error_msg += f": {result.stderr.strip()}"
            return False, error_msg
            
    except subprocess.TimeoutExpired:
        return False, "Process timed out after 30 seconds"
    except FileNotFoundError:
        return False, f"ipetoipe.exe not found at: {ipe2ipe_path}"
    except Exception as e:
        return False, f"Compilation error: {str(e)}"


def save_ipe_log_file(xml_filename: str, ipelogs_dir: str):
    """
    Copy the IPE log file to our ipelogs directory when compilation fails.
    """
    ipe_log_path = r"C:\Users\raine\AppData\Local\ipe\ipetemp.log"
    
    try:
        if not os.path.exists(ipe_log_path):
            print(f"    Warning: IPE log file not found at: {ipe_log_path}")
            return
        
        # Create ipelogs directory if it doesn't exist
        os.makedirs(ipelogs_dir, exist_ok=True)
        
        # Create destination file with XML filename + .log extension
        log_filename = xml_filename + ".log"
        destination_path = os.path.join(ipelogs_dir, log_filename)
        
        # Copy the log file
        shutil.copy2(ipe_log_path, destination_path)
        print(f"    → Saved IPE log to: {destination_path}")
        
    except Exception as e:
        print(f"    Warning: Failed to save IPE log file: {e}")


def compile_all_xml_files(step4_dir: str, step5_dir: str, ipelogs_dir: str):
    """
    Compile all XML files from step-4 to PDFs in step-5.
    """
    # Load configuration
    config = load_configuration()
    ipe2ipe_path = get_ipe2ipe_path(config)
    
    # Ensure output directory exists
    os.makedirs(step5_dir, exist_ok=True)
    
    # Get all XML files to compile
    xml_files = get_xml_files(step4_dir)
    
    if not xml_files:
        print("No XML files found to compile")
        return
    
    print(f"Found {len(xml_files)} XML files to compile")
    print(f"Using ipe2ipe at: {ipe2ipe_path}")
    print()
    
    successful_compilations = 0
    failed_compilations = 0
    
    for xml_filename in xml_files:
        xml_file_path = os.path.join(step4_dir, xml_filename + ".xml")
        pdf_file_path = os.path.join(step5_dir, xml_filename + ".pdf")
        
        if not os.path.exists(xml_file_path):
            print(f"Warning: XML file not found: {xml_file_path}")
            failed_compilations += 1
            continue
        
        print(f"Compiling: {xml_filename}.xml → {xml_filename}.pdf")
        
        success, error_msg = compile_xml_to_pdf(xml_file_path, pdf_file_path, ipe2ipe_path)
        
        if success:
            if os.path.exists(pdf_file_path) and os.path.getsize(pdf_file_path) > 0:
                print(f"  ✓ Successfully compiled: {pdf_file_path}")
                successful_compilations += 1
            else:
                print(f"  ✗ Compilation appeared successful but PDF not created or empty")
                failed_compilations += 1
                save_ipe_log_file(xml_filename, ipelogs_dir)
        else:
            print(f"  ✗ Failed to compile: {error_msg}")
            failed_compilations += 1
            save_ipe_log_file(xml_filename, ipelogs_dir)
    
    print()
    print("Compilation Summary:")
    print(f"  ✓ Successful: {successful_compilations}")
    print(f"  ✗ Failed: {failed_compilations}")
    print(f"  Total: {len(xml_files)}")
    
    if failed_compilations > 0:
        print(f"\nCheck the '{ipelogs_dir}' directory for compilation logs.")


def main():
    """Main function to execute the XML compilation."""
    # Define paths
    base_dir = Path(__file__).parent
    step4_dir = base_dir / ".work" / "step-4"
    step5_dir = base_dir / ".work" / "step-5"
    ipelogs_dir = base_dir / "ipelogs"
    
    print("IPE XML to PDF Compilation")
    print(f"Input directory: {step4_dir}")
    print(f"Output directory: {step5_dir}")
    print(f"Log directory: {ipelogs_dir}")
    print("-" * 60)
    
    compile_all_xml_files(str(step4_dir), str(step5_dir), str(ipelogs_dir))
    
    print("-" * 60)
    print("Compilation completed!")


if __name__ == "__main__":
    main()
