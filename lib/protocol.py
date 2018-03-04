import wiringpi
import TestPhoto
import time

wiringpi.wiringPiSetup()
serial = wiringpi.serialOpen("/dev/ttyAMA0", 115200)

while(True):
    wiringpi.serialPutchar(serial, 55)
    
    if(wiringpi.serialDataAvail(serial) > 0):
        code = wiringpi.serialGetchar(serial)
        print("Recieved: ", code)
    
        if(code == 11):
            size = []
            for i in range(4):
                size.append(wiringpi.serialGetchar(serial))
            sizeI = sum(map(lambda x: max(x[0]*256, 1)*x[1], enumerate(size[::-1])))
            print("Payload size:", sizeI)
    
            imageBytes = []
            for i in range(sizeI):
               imageBytes.append(wiringpi.serialGetchar(serial))
    
            newFile = open("test.jpg", "wb")
            newFile.write(bytes(imageBytes))
            newFile.flush()
            fileout = TestPhoto.detect("test.jpg")
            with open("logs/foo.log", "a") as f:
                for o in fileout:
                    f.write(o + "\n")

    else:
        print("No response received.")
    time.sleep(1)
    
wiringpi.serialFlush(serial)
wiringpi.serialClose(serial)

# newFileBytes = [123, 3, 255, 0, 100]
# # make file
# newFile = open("filename.txt", "wb")
# # write to file
# newFile.write(newFileBytes)
