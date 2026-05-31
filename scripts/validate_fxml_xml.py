import os
import xml.etree.ElementTree as ET
root = r"c:\Users\ADMIN\Documents\Baitaplon\Biddingweb\Biddingweb\demo\src\main\resources"
errors = []
for dirpath, dirs, files in os.walk(root):
    for f in files:
        if f.endswith('.fxml'):
            path = os.path.join(dirpath, f)
            try:
                ET.parse(path)
            except ET.ParseError as e:
                errors.append((path, str(e)))
print('Found', len(errors), 'XML parse errors.')
for p, err in errors:
    print(p, err)
