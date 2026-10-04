"""Clear Windows read-only flags from this edition's temporary build output."""
from pathlib import Path
import os
import stat

project = Path(__file__).resolve().parents[2]
build_root = project.parent / "build-firetv-beta4" / "app"
changed = 0
if os.name == "nt" and build_root.exists():
    for directory, dirs, files in os.walk(build_root):
        for entry in [Path(directory), *(Path(directory) / name for name in dirs + files)]:
            if entry.stat().st_file_attributes & stat.FILE_ATTRIBUTE_READONLY:
                os.chmod(entry, stat.S_IWRITE | stat.S_IREAD)
                changed += 1
print(f"Fire TV temporary build: cleared {changed} read-only flags")
