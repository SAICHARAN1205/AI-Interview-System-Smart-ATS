import urllib.request
import urllib.error
import json

BASE = 'http://localhost:8080'

def post_json(path, payload):
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(BASE + path, data=data, headers={'Content-Type': 'application/json'})
    return urllib.request.urlopen(req).read().decode()

try:
    candidate = json.loads(post_json('/api/auth/login', {'email': 'john@example.com', 'password': 'securepassword123'}))
    print('candidate token:', candidate['token'])
except urllib.error.HTTPError as e:
    print('candidate login failed', e.code)
    print(e.read().decode())
    raise

try:
    recruiter = json.loads(post_json('/api/auth/login', {'email': 'recruiter@gmail.com', 'password': '123456'}))
    print('recruiter token:', recruiter['token'])
except urllib.error.HTTPError as e:
    print('recruiter login failed', e.code)
    print(e.read().decode())
    raise

print('done')
