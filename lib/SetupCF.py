import cognitive_face as CF
import os

KEY = os.environ.get("KEY")
print(KEY)
if not KEY:
    print("No key found")
    sys.exit(-1)
CF.Key.set(KEY)

