from datetime import time
from flask import Flask, Response
import time
import sqlite3

app = Flask('cache')

con = sqlite3.connect('data.db', check_same_thread=False)

@app.route('/')
def hello_world():
    return 'Hello!\n'

@app.route('/<id>')
def page(id):
    content = f'Comments from page {id}:\n'
    start = time.time()

    cur = con.cursor()
    cur.execute("select * from comments where page=?", (id,))
    comments = cur.fetchall()
    for comment in comments:
        content += comment[2] + '\n'

    cost = time.time() - start
    content += f'Time cost: {cost}\n'
    return Response(content, mimetype='text/plain')

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000)