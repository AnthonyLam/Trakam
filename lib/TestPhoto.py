import cognitive_face as CF
import sys
import pprint
from PIL import Image, ImageDraw, ImageFont

pp = pprint.PrettyPrinter(indent=4)

if len(sys.argv) < 2:
    print("This is no good")
    sys.exit()

KEY = '90823e5520ad43b3b8e828ba6b1a7a3e'
CF.Key.set(KEY)

file = sys.argv[1]

print("file = {}\n".format(file))

detectOut = CF.face.detect(file, attributes='age')
pp.pprint(detectOut)

output = (list(map(lambda x: x['faceId'], detectOut)))

identifyOut = CF.face.identify(output, 'people')
pp.pprint(identifyOut)

for x in identifyOut:
    for candidate in x['candidates']:
        print(CF.person.get('people', candidate['personId'])['name'])

im = Image.open(file)
draw = ImageDraw.Draw(im)
fnt = ImageFont.truetype('Raleway-Regular.ttf', 40)

for detection in detectOut:
    rect = detection["faceRectangle"]
    x0 = rect["left"]
    y0 = rect["top"]
    x1 = rect["width"] + x0
    y1 = rect["height"] + y0
    draw.rectangle([x0, y0, x1, y1])
    io = CF.face.identify([detection["faceId"]], 'people')
    for x in io:
        for candidate in x['candidates']:
            draw.text([x0, y0], CF.person.get('people', candidate['personId'])['name'], font=fnt, fill=(255, 0, 0))

del draw

im.save(file.split(sep=".")[0] + "ed.png", "PNG")
