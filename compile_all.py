#!/usr/bin/env python3
"""
Compile all XML files from step-4 to PDFs in step-5.
Python equivalent of CompileStep.java that operates on all files.
"""

import os
import json
import subprocess
import shutil
from pathlib import Path
from typing import Dict, List


def load_configuration() -> Dict[str, str]:
    """Load configuration from settings.json file."""
    config = {
        "ipe_dir": r"C:\Program Files (x86)\IPE\bin"  # Default directory
    }
    
    # Try to load from settings.json
    settings_file = Path(__file__).parent / "src" / "main" / "resources" / "settings.json"
    if settings_file.exists():
        try:
            with open(settings_file, 'r', encoding='utf-8') as f:
                settings = json.load(f)
                if "ipe_dir" in settings:
                    config["ipe_dir"] = settings["ipe_dir"]
        except (json.JSONDecodeError, Exception) as e:
            print(f"Warning: Could not load settings.json: {e}")
    
    return config


def get_ipetoipe_path(ipe_dir: str) -> str:
    """Construct the full path to ipetoipe.exe, following Java Configuration pattern."""
    if not ipe_dir:
        return "ipetoipe.exe"  # Assume it's in PATH
    return os.path.join(ipe_dir, "ipetoipe.exe")


def run_ipe_command(ipe_path: str, input_file: str, output_file: str) -> tuple[bool, str, str]:
    """
    Run ipetoipe command to convert XML to PDF.
    Returns (success, stdout, stderr).
    """
    try:
        # Construct the command
        cmd = [ipe_path, "-pdf", input_file, output_file]
        
        # Run the command
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=30  # 30 second timeout
        )
        
        success = result.returncode == 0
        return success, result.stdout, result.stderr
        
    except subprocess.TimeoutExpired:
        return False, "", "Command timed out after 30 seconds"
    except FileNotFoundError:
        return False, "", f"ipetoipe executable not found at: {ipe_path}"
    except Exception as e:
        return False, "", f"Error running command: {str(e)}"


def save_log(log_dir: str, filename: str, success: bool, stdout: str, stderr: str, input_file: str, output_file: str):
    """Save compilation log to a file."""
    os.makedirs(log_dir, exist_ok=True)
    
    log_filename = filename.replace('.xml', '.log')
    log_path = os.path.join(log_dir, log_filename)
    
    with open(log_path, 'w', encoding='utf-8') as f:
        f.write(f"IPE Compilation Log\n")
        f.write(f"==================\n")
        f.write(f"Input file: {input_file}\n")
        f.write(f"Output file: {output_file}\n")
        f.write(f"Success: {success}\n")
        f.write(f"Timestamp: {os.popen('echo %date% %time%').read().strip()}\n")
        f.write(f"\n")
        
        if stdout:
            f.write(f"STDOUT:\n{stdout}\n\n")
        
        if stderr:
            f.write(f"STDERR:\n{stderr}\n\n")


def get_xml_files(directory: str) -> List[str]:
    """Get all XML files from a directory."""
    xml_files = []
    if os.path.exists(directory):
        for filename in os.listdir(directory):
            if filename.endswith('.xml'):
                xml_files.append(filename)
    return sorted(xml_files)


def compile_all_files(step4_dir: str, step5_dir: str, log_dir: str):
    """
    Compile all XML files from step-4 to PDFs in step-5.
    """
    # Load configuration
    config = load_configuration()
    ipe_dir = config["ipe_dir"]
    ipe_path = get_ipetoipe_path(ipe_dir)
    
    # Ensure output directories exist
    os.makedirs(step5_dir, exist_ok=True)
    os.makedirs(log_dir, exist_ok=True)
    
    # Check if IPE executable exists
    if not os.path.exists(ipe_path):
        print(f"Warning: IPE executable not found at: {ipe_path}")
        print("Please install IPE or update the path in settings.json")
        print("Continuing anyway to demonstrate the process...")
    
    # Get all XML files from step-4
    xml_files = get_xml_files(step4_dir)
    
    if not xml_files:
        print(f"No XML files found in {step4_dir}")
        return
    
    print(f"Found {len(xml_files)} XML files to compile")
    print(f"IPE executable: {ipe_path}")
    print(f"Input directory: {step4_dir}")
    print(f"Output directory: {step5_dir}")
    print(f"Log directory: {log_dir}")
    print("-" * 60)
    
    successful_compilations = 0
    failed_compilations = 0
    
    for xml_filename in xml_files:
        input_path = os.path.join(step4_dir, xml_filename)
        pdf_filename = xml_filename.replace('.xml', '.pdf')
        output_path = os.path.join(step5_dir, pdf_filename)
        
        print(f"Compiling {xml_filename}...")
        
        # Run IPE compilation
        success, stdout, stderr = run_ipe_command(ipe_path, input_path, output_path)
        
        # Save log
        save_log(log_dir, xml_filename, success, stdout, stderr, input_path, output_path)
        
        if success:
            print(f"  ✓ Successfully compiled to {pdf_filename}")
            successful_compilations += 1
        else:
            print(f"  ✗ Failed to compile {xml_filename}")
            if stderr:
                print(f"    Error: {stderr}")
            failed_compilations += 1
    
    print("-" * 60)
    print(f"Compilation Summary:")
    print(f"  Successful: {successful_compilations}")
    print(f"  Failed: {failed_compilations}")
    print(f"  Total: {len(xml_files)}")
    
    if failed_compilations > 0:
        print(f"\nCheck log files in {log_dir} for details on failed compilations.")


def main():
    """Main function to execute the compilation."""
    # Define paths
    base_dir = Path(__file__).parent
    step4_dir = base_dir / ".work" / "step-4"
    step5_dir = base_dir / ".work" / "step-5"
    log_dir = base_dir / "ipelogs"
    
    print("Compiling all XML files from step-4 to PDFs...")
    print(f"Input directory: {step4_dir}")
    print(f"Output directory: {step5_dir}")
    print(f"Log directory: {log_dir}")
    print("=" * 60)
    
    compile_all_files(str(step4_dir), str(step5_dir), str(log_dir))
    
    print("=" * 60)
    print("Compilation process completed!")


if __name__ == "__main__":
    main()
