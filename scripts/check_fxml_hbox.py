import os
import re

root = r"c:\Users\ADMIN\Documents\Baitaplon\Biddingweb\Biddingweb\demo\src\main\resources"
problem = []
for dirpath, dirs, files in os.walk(root):
    for f in files:
        if f.endswith('.fxml'):
            path = os.path.join(dirpath, f)
            with open(path, 'r', encoding='utf-8') as fh:
                txt = fh.read()
            opens = len(re.findall(r'<\s*HBox\b', txt))
            closes = len(re.findall(r'</\s*HBox\s*>', txt))
            if opens != closes:
                problem.append((path, opens, closes))

if not problem:
    print('No mismatched HBox tag counts found in resources.')
else:
    print('Files with mismatched HBox counts:')
    for p, o, c in problem:
        print(f'{p}: opens={o}, closes={c}')
