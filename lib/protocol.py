import wiringpi
import TestPhoto
import time
import struct
import logging
import requests
import threading
from shutil import copyfile


STREAM_SERVER = "http://127.0.0.1:4081/"
IMG_TMP = "/var/img/"
LOG_FILE = "logs/out.log"
IMG_DIR = "img"

wiringpi.wiringPiSetup()
serial = wiringpi.serialOpen("/dev/ttyAMA0", 921600)
log = logging.getLogger(__name__)

class AzureDetect(threading.Thread):
    def __init__(self, img):
        threading.Thread.__init__(self)
        self.img = img

    def run(self):
        fileout = TestPhoto.detect(self.img)
        for detection in fileout:
            uuid = detection.split(sep=',')[0]
            copyfile("/var/detect/file.jpg","{}/{}.jpg".format(IMG_DIR, uuid))
        with open(LOG_FILE, "a") as f:
            for o in fileout:
                f.write(o + "\n")

def get_chars(size):
    for _ in range(size):
        yield wiringpi.serialGetchar(serial)

def bytes_safe(chars):
    try:
        return bytes(chars)
    except Exception as e:
        print("Exception Thrown: ", e)
        return bytes()

count = 0
while(True):
    wiringpi.serialPutchar(serial, 55)

    if(wiringpi.serialDataAvail(serial) > 0):
        code = wiringpi.serialGetchar(serial)
        print("Recieved: %d", code)

        # try:
        size = bytes_safe(get_chars(4))
        sizeI = struct.unpack('>i', size)[0]
        print("Payload size: %d", sizeI)

        b = bytes_safe(get_chars(sizeI))
        file_name = "{}{}.jpg".format(IMG_TMP, count)
        if count >= 100:
            count = 0
        with open(file_name, "wb") as f:
            f.write(b)
        count += 1
        if(code == 11):
            t = AzureDetect(file_name)
            t.start()
        # except Exception as e:
        #     print("Exception Thrown: ", e)

    time.sleep(0.1)

wiringpi.serialFlush(serial)
wiringpi.serialClose(serial)
