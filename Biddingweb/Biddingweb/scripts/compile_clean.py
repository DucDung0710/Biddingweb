from pathlib import Path
import subprocess
import sys
import ctypes

def get_short_path(path):
    """Return the Windows short (8.3) path for the given path."""
    if path is None:
        return None
    buf = ctypes.create_unicode_buffer(260)
    GetShortPathName = ctypes.windll.kernel32.GetShortPathNameW
    res = GetShortPathName(str(path), buf, len(buf))
    if res == 0:
        return str(path)
    return buf.value

root = Path(__file__).resolve().parents[1]
src = root / 'demo' / 'src' / 'main' / 'java'
files = [p for p in src.rglob('*.java')]
print('Found', len(files), 'java files')

# Use Windows short paths to avoid javac failing on non-ASCII parent folders
short_root = get_short_path(root)
short_target_classes = get_short_path(root / 'target' / 'classes')

# Change working directory to the short path root so relative source paths don't include non-ASCII parent folders
import os
os.chdir(short_root)

# Build relative paths for files
files_rel = [str(p.relative_to(root)).replace('\\', os.sep) for p in files]
# Exclude module-info.java to avoid module system / JavaFX dependency issues during this check
files_rel = [f for f in files_rel if not f.endswith('module-info.java')]

# Compile all Java files now that BOM issues were fixed
print('Sample files (5):', files_rel[:5])
cmd = ['javac', '-d', os.path.join('target', 'classes')] + files_rel
print('Command args ({}):'.format(len(cmd)))
for i,a in enumerate(cmd[:20]):
    print(i, a)
res = subprocess.run(cmd, capture_output=True, text=True)
print('RC', res.returncode)
print(res.stdout)
print(res.stderr)
if res.returncode != 0:
    sys.exit(res.returncode)
else:
    print('Compile succeeded')
