# -*- coding: utf-8 -*-
import sqlite3
con = sqlite3.connect('bidding_system.db')
c = con.cursor()
print('PRAGMA table_info(users):')
for row in c.execute("PRAGMA table_info(users)"):
    print(row)
print('-' * 40)
print('Users:')
for row in c.execute('SELECT id, username, email, password, role, balance FROM users'):
    print(row)
con.close()
