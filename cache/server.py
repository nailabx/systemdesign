from datetime import time
import json
from flask import Flask, Response
import time
import sqlite3
import redis

app = Flask('cache')

r = redis.Redis(host='localhost', port=6379, db=0)
con = sqlite3.connect('data.db', check_same_thread=False)

@app.route('/')
def hello_world():
    return 'Hello!\n'

@app.route('/<id>')
def page(id):
    content = f'Comments from page {id}:\n'
    start = time.time()
    comments = []

    if r.exists(id):
        content += 'Cache: hit\n'
        comments = json.loads(r.get(id))
    else:
        content += 'Cache: miss\n'
        cur = con.cursor()
        cur.execute("select * from comments where page=?", (id,))
        comments = cur.fetchall()
        r.set(id, json.dumps(comments))

    for comment in comments:
        content += comment[2] + '\n'

    cost = time.time() - start
    content += f'Time cost: {cost}\n'
    return Response(content, mimetype='text/plain')

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000)