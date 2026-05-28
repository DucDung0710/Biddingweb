from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
count = 0
for p in (root / 'demo' / 'src' / 'main' / 'java').rglob('*.java'):
    b = p.read_bytes()
    if b.startswith(b'\xef\xbb\xbf'):
        p.write_bytes(b[3:])
        count += 1
        print('Stripped BOM from', p)
print('Total files updated:', count)
if count == 0:
    print('No BOMs found')
