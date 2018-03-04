import cognitive_face as CF
import sys
import pprint
import uuid
import time

pp = pprint.PrettyPrinter(indent=4)
KEY = '90823e5520ad43b3b8e828ba6b1a7a3e'
CF.Key.set(KEY)

def detect(file):
    print("file = {}\n".format(file))
        
    detectOut = CF.face.detect(file, attributes='age')
    pp.pprint(detectOut)
    output = []
    
    if(len(detectOut) > 0):
        output = (list(map(lambda x: x['faceId'], detectOut)))
        
        identifyOut = CF.face.identify(output, 'people')
        pp.pprint(identifyOut)
        
        for x in identifyOut:
            for candidate in x['candidates']:
                name = CF.person.get('people', candidate['personId'])['name']
                output.append("{},{},{}".format(str(uuid.uuid4()), name, int(time.time() * 1000)))
    return output
    
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("This is no good")
        sys.exit()

    file = sys.argv[1]
    detect(file)