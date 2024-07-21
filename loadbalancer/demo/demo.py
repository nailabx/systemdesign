from flask import Flask
import sys
import os

app = Flask("demo")
port = os.getenv("PORT") or sys.argv[1] or 8000

@app.route("/", methods=["GET"])
def get_hello_world():
    return {"message": "Hello, World!"}, 200

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=port)