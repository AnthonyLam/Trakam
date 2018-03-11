import cognitive_face as CF

with open('../faceapi.key', 'r') as f:
    KEY = str(f.readline()).strip()
    print("The key: ", KEY)
if not KEY:
    print("No key found")
    sys.exit(-1)
CF.Key.set(KEY)

