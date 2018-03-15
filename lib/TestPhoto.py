import cognitive_face as CF
import sys
import pprint
import uuid
import time
import SetupCF
from PIL import Image, ImageDraw, ImageFont

pp = pprint.PrettyPrinter(indent=4)
#fnt = ImageFont.truetype('Raleway-Regular.ttf', 5)
INDENT = 20


def addUnknown(ret, det, draw, im):
    name = "unknown"
    addIdentify(ret, name, det, draw, im)

def addIdentify(ret, name, det, draw, im):
    thisuuid = str(uuid.uuid4())
    ret.append("{},{},{}".format(thisuuid, name, int(time.time() * 1000)))   
    pp.pprint(det)
    if det:
        rect = det["faceRectangle"]
        x0 = rect["left"] - INDENT
        y0 = rect["top"] - INDENT
        x1 = rect["width"] + x0 + 2*INDENT
        y1 = rect["height"] + y0 + 2*INDENT

        face = im.crop((x0,y0,x1,y1))
        face.save("img/{}-face.jpg".format(thisuuid))

        draw.rectangle([x0, y0, x1, y1])
        #draw.text([x0, y0], name, fnt=fnt, fill=(255,0,0))
        

def detect(file):
    print("file = {}\n".format(file))
        
    detectOut = CF.face.detect(file, attributes='age')
    im = Image.open(file)
    draw = ImageDraw.Draw(im)
    pp.pprint(detectOut)
    ret = []
        
    if(len(detectOut) > 0):
        output = (list(map(lambda x: x['faceId'], detectOut)))
        
        try:
            identifyOut = CF.face.identify(output, 'people')
        except Exception as e:
            print("No faces trained")
            for x in detectOut:
                addUnknown(ret, x, draw, im)
            im.save("/var/detect/file.jpg",'JPEG')
            del draw
            return ret

        pp.pprint(identifyOut)
        
        for x in identifyOut:
            det = next( 
                    (
                        detection 
                        for detection in detectOut 
                        if detection['faceId'] == x['faceId'] 
                    ),
                    None
                    )

            for candidate in x['candidates']:
                name = CF.person.get('people', candidate['personId'])['name']
                print(name)
                addIdentify(ret, name, det, draw, im)
                break
            if len(x['candidates']) == 0:
                addUnknown(ret, det, draw, im)
        im.save("/var/detect/file.jpg", 'JPEG')
    del draw
    return ret
    
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("This is no good")
        sys.exit()

    file = sys.argv[1]
    detect(file)
