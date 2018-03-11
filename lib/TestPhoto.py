import cognitive_face as CF
import sys
import pprint
import uuid
import time
import SetupCF 

pp = pprint.PrettyPrinter(indent=4)

def detect(file):
    print("file = {}\n".format(file))
        
    detectOut = CF.face.detect(file, attributes='age')
    pp.pprint(detectOut)
    ret = []
    
    if(len(detectOut) > 0):
        output = (list(map(lambda x: x['faceId'], detectOut)))
        
        try:
            identifyOut = CF.face.identify(output, 'people')
        except Exception as e:
            print("No faces trained")
            for x in detectOut:
                name = "unknown"
                ret.append("{},{},{}".format(str(uuid.uuid4()), name, int(time.time() * 1000)))
            return ret

        pp.pprint(identifyOut)
        
        for x in identifyOut:
            for candidate in x['candidates']:
                name = CF.person.get('people', candidate['personId'])['name']
                print(name)
                ret.append("{},{},{}".format(str(uuid.uuid4()), name, int(time.time() * 1000)))
            if len(x['candidates']) == 0:
                name = "unknown"
                ret.append("{},{},{}".format(str(uuid.uuid4()), name, int(time.time() * 1000)))
    return ret
    
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("This is no good")
        sys.exit()

    file = sys.argv[1]
    detect(file)
