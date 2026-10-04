"""Clear Windows read-only attributes from generated Shield build output."""
from pathlib import Path
import os,stat
project=Path(__file__).resolve().parents[2]
build=project/'app'/'build'
changed=0
if os.name=='nt' and build.exists():
 for directory,dirs,files in os.walk(build):
  for entry in [Path(directory),*(Path(directory)/name for name in dirs+files)]:
   if entry.stat().st_file_attributes & stat.FILE_ATTRIBUTE_READONLY:
    os.chmod(entry,stat.S_IWRITE|stat.S_IREAD);changed+=1
print(f'Shield generated output: cleared {changed} read-only attributes')
