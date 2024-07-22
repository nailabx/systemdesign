# init.py
import sqlite3
import random

con = sqlite3.connect('data.db')
cur = con.cursor()
size = 1000000

# Create table
cur.execute('''CREATE TABLE comments
              (domain text, page int, content text)''')

# Insert data
for i in range(size):
    cur.execute("insert into comments values (?, ?, ?)", ("algo.monster",
                random.randint(0, size // 10), f'Comment number {i}'))

con.commit()
con.close()