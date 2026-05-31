import os, re
root = r"c:\Users\ADMIN\Documents\Baitaplon\Biddingweb\Biddingweb\demo\src\main\resources"
problem = []
for dirpath, dirs, files in os.walk(root):
    for f in files:
        if f.endswith('.fxml'):
            path = os.path.join(dirpath, f)
            with open(path, 'r', encoding='utf-8') as fh:
                txt = fh.read()
            total_opens = len(re.findall(r'<\s*HBox\b', txt))
            total_closes = len(re.findall(r'</\s*HBox\s*>', txt))
            self_closed = len(re.findall(r'<\s*HBox\b[^>]*?/>', txt))
            # treat self-closed as both open+close, so adjust opens = opens - self_closed; closes = closes + self_closed
            net = (total_opens - self_closed) - total_closes
            if net != 0:
                problem.append((path, total_opens, total_closes, self_closed, net))

if not problem:
    print('No mismatched HBox tag counts found.')
else:
    print('Files with mismatched HBox counts:')
    for p,o,c,s,n in problem:
        print(f'{p}: opens={o}, closes={c}, self_closed={s}, net={n}')
