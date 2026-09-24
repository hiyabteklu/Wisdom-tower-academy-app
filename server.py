import http.server
import os
import sys
import time

PORT = 3000
DIRECTORY = "/app/applet/.build-outputs"

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def log_message(self, format, *args):
        sys.stderr.write("%s - - [%s] %s\n" % (self.address_string(), self.log_date_time_string(), format % args))
        sys.stderr.flush()

class ReusableServer(http.server.ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True

def run():
    while True:
        try:
            with ReusableServer(("", PORT), Handler) as httpd:
                print(f"Server started on port {PORT} serving {DIRECTORY}", flush=True)
                httpd.serve_forever()
        except Exception as e:
            print(f"Server error: {e}, restarting in 1s...", flush=True)
            time.sleep(1)

if __name__ == "__main__":
    run()
