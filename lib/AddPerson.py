import cognitive_face as CF
import sys

if len(sys.argv) < 4:
	print( "This is no good")
	sys.exit();

KEY = '90823e5520ad43b3b8e828ba6b1a7a3e'
CF.Key.set(KEY)

file = sys.argv[1]
name = sys.argv[2]
age = sys.argv[3]

print("file = {}\nname = {}\nage = {}".format(file,name,age))
input("Press the any key to continue")

output = CF.person.create('people', name, age)
print(output)
print(CF.person.add_face(file, 'people', output['personId']))
CF.person_group.train('people')