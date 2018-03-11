import wiringpi
import TestPhoto
import time
import asyncio
import logging
from shutil import copyfile


STREAM_FILE = "stream.jpg"
DETECT_FILE = "test.jpg"
LOG_FILE = "logs/out.log"
IMG_DIR = "img"

wiringpi.wiringPiSetup()
serial = wiringpi.serialOpen("/dev/ttyAMA0", 921600)
loop = asyncio.get_event_loop()
log = logging.getLogger(__name__)

log.debug("Starting serial connection")


async def check_azure():
    fileout = TestPhoto.detect(DETECT_FILE)
    for detection in fileout:
        uuid = detection.split(sep=',')[0]
        copyfile(DETECT_FILE, "{}/{}.jpg".format(IMG_DIR, uuid))
    with open(LOG_FILE, "a") as f:
        for o in fileout:
            f.write(o + "\n")
        f.flush()

def get_chars(size):
    for x in range(size):
        yield wiringpi.serialGetchar(serial)


while(True):
    wiringpi.serialPutchar(serial, 55)
     
    if(wiringpi.serialDataAvail(serial) > 0):
        code = wiringpi.serialGetchar(serial)
        log.debug("Recieved: %d", code)
     
        size = list(get_chars(4))
        sizeI = sum(map(lambda x: max(x[0]*256, 1)*x[1], enumerate(size[::-1])))
        log.debug("Payload size: %d", sizeI)
     
        with open(STREAM_FILE, "wb") as f, open(DETECT_FILE, "wb") as t:
            b = bytes(list(get_chars(sizeI)))
            f.write(b)
            f.flush()
            t.write(b)
            t.flush()
             
        if(code == 11):
            loop.run_until_complete(check_azure())
    time.sleep(0.1)
     
loop.close()
wiringpi.serialFlush(serial)
wiringpi.serialClose(serial)
