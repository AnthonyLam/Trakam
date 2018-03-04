# Hello World Example
#
# Welcome to the OpenMV IDE! Click on the green run arrow button below to run the script!

import sensor
import image
import time
import pyb
from struct import pack
from pyb import UART, SPI


sensor.reset()                      # Reset and initialize the sensor.
sensor.set_contrast(1)
sensor.set_gainceiling(16)
sensor.set_pixformat(sensor.GRAYSCALE) # Set pixel format to RGB565 (or GRAYSCALE)
sensor.set_framesize(sensor.HQVGA)   # Set frame size to QQVGA (160 x 120)
sensor.skip_frames(time = 2000)     # Wait for settings take effect.


# Initialize UART for servo
#uart = pyb.UART(3)
#uart.init(9600, bits=8, parity=None, stop=1, timeout_char=1000)

# Initialize UART communication
# pi = UART(1)
# pi.init(115200, bits=8, parity=0, stop=1, timeout_char=2000)

pi = SPI(2)
pi.init(SPI.SLAVE, polarity=0, phase=0)

front_face = image.HaarCascade("frontalface", stages=25)
print(front_face)

TOP_CHAN = 0
BOT_CHAN = 1

ROT_SPEED = 15  # Make this speed a function of the distance to the object.
LIMIT_MIN = 992
LIMIT_MAX = 2000

FACE_TIMEOUT = 100
X_MAG = 60
Y_MAG = 60

c = pyb.millis()
saved = []
print(pyb.freq())

while(True):
    img = sensor.snapshot()         # Take a picture and return the image.

    blobs = list(img.find_features(front_face, threshold=0.95, scale_factor=1.25))

    invalidated = (pyb.millis() - c > FACE_TIMEOUT) or \
        len(blobs) != len(saved) or \
        any([abs(f[0] - l[0]) > X_MAG or abs(f[1] - l[1]) > Y_MAG  for f, l in zip(blobs, saved)])

    if invalidated:
        c = pyb.millis()

    # Wait for master to trigger request
    try:
        if pi.send_recv(55, timeout=5)[0] == 55:
            print("Request Received")
            cimg = img.compress()
            valid = 11 if invalidated and blobs else 0
            print("Sending image. Detect: {} Size: {}".format(valid, cimg.size()))
            pi.send_recv(11)  # Send whether this frame has a face

            # Sending first chunk
            print("Chunk: 2048")
            pi.send_recv(pack('>i', 2048))  # Send image jpg size
            pi.send_recv(bytes(cimg[:2048]))  # Send image

            # Sending second chunk
            print("Chunk: ", cimg.size()-2048)
            pi.send_recv(pack('>i', cimg.size() - 2048))  # Send image jpg size
            pi.send_recv(bytes(cimg[2048:]))  # Send image

    except OSError:
        print("Timed out sending")
    saved = list(blobs)
