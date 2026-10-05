"""Clear Windows read-only attributes from this edition's generated build output."""
from pathlib import Path
import ctypes
import os
import stat
import shutil

project = Path(__file__).resolve().parents[2]
build = project / 'app' / 'build'
changed = 0
if os.name == 'nt' and build.exists():
    kernel = ctypes.WinDLL('kernel32', use_last_error=True)
    kernel.SetFileAttributesW.argtypes = [ctypes.c_wchar_p, ctypes.c_uint32]
    kernel.SetFileAttributesW.restype = ctypes.c_int
    for directory, dirs, files in os.walk(build):
        for entry in [Path(directory), *(Path(directory) / name for name in dirs + files)]:
            flags = entry.stat().st_file_attributes
            if flags & stat.FILE_ATTRIBUTE_READONLY:
                if not kernel.SetFileAttributesW(str(entry), flags & ~stat.FILE_ATTRIBUTE_READONLY):
                    raise ctypes.WinError(ctypes.get_last_error())
                if entry.stat().st_file_attributes & stat.FILE_ATTRIBUTE_READONLY:
                    raise OSError('Could not clear generated output attribute: ' + str(entry))
                changed += 1
print(f'Shield build output: cleared {changed} read-only attributes')

# Recreate generated packaging profiles; stale directories can block Gradle on Windows.
profiles = build / 'outputs/apk/full/debug/baselineProfiles'
if os.name == 'nt' and profiles.exists():
    shutil.rmtree(profiles)
    if profiles.exists():
        raise OSError('Could not remove generated packaging profiles')
