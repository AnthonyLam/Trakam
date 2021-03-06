import cognitive_face as CF
import sys
import time
from PIL import Image
import SetupCF

if len(sys.argv) < 3:
	print( "This is no good")
	sys.exit();

OFFSET = 25

file = sys.argv[1]
name = sys.argv[2]

print("file = {}\nname = {}".format(file,name))

output = CF.person.create('people', name)
print(output)
print(CF.person.add_face(file, 'people', output['personId']))
CF.person_group.train('people')
time.sleep(5)
output = CF.face.detect(file)
print(output)
if(len(output) > 0):
    img = Image.open(file)
    rect = output[0]['faceRectangle']
    box=(rect['left']-OFFSET, rect['top']-OFFSET,  rect['left']+rect['width']+OFFSET, rect['top']+rect['height']+OFFSET)
    img2 = img.crop(box)
    img2.save("img/{}.jpg".format(name.replace(" ", "_")))
