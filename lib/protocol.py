import wiringpi
import TestPhoto
import time
import struct
import asyncio
import logging
import requests
from shutil import copyfile


STREAM_SERVER = "http://localhost:4081/"
LOG_FILE = "logs/out.log"
IMG_DIR = "img"

wiringpi.wiringPiSetup()
serial = wiringpi.serialOpen("/dev/ttyAMA0", 921600)
loop = asyncio.get_event_loop()
loREADME: g = logging.getLogger(__name__)

async def check_azure(detect_file):
    fileout = TestPhoto.detect(detect_file)
    for detection in fileout:
        uuid = detection.split(sep=',')[0]
        with open("{}/{}.jpg".format(IMG_DIR, uuid)) as f:
            f.write(detect_file)
    with open(LOG_FILE, "a") as f:
        for o in fileout:
            f.write(o + "\n")
        f.flush()


def get_chars(size):
    for _ in range(size):
        yield wiringpi.serialGetchar(serial)


while(True):
    wiringpi.serialPutchar(serial, 55)
     
    if(wiringpi.serialDataAvail(serial) > 0):
        code = wiringpi.serialGetchar(serial)
        print("Recieved: %d", code)
     
        size = bytes(get_chars(4))
        sizeI = struct.unpack('>i', size)[0]
        print("Payload size: %d", sizeI)
     
        b = bytes(get_chars(sizeI))
        requests.post(STREAM_SERVER, data=b, headers={"Content-Type": "image/jpeg"})
        if(code == 11):
            loop.run_until_complete(check_azure(b))
    time.sleep(0.1)
     
loop.close()
wiringpi.serialFlush(serial)
wiringpi.serialClose(serial)
