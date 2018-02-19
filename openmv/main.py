# Hello World Example
#
# Welcome to the OpenMV IDE! Click on the green run arrow button below to run the script!

import sensor
import image
import time
import pyb

def send_servo_command(channel: int, target: int):
    target *= 4
    low_bits = 0x7F & target
    high_bits = (target >> 7) & 0x7F

    uart.writechar(0x84)
    uart.writechar(channel)
    uart.writechar(low_bits)
    uart.writechar(high_bits)


def move_left(state: int):
    state -= ROT_SPEED
    state = min(state, LIMIT_MAX)
    send_servo_command(BOT_CHAN, state)
    return state

def move_right(state: int):
    state += ROT_SPEED
    state = max(state, LIMIT_MIN)
    send_servo_command(BOT_CHAN, state)
    return state

def move_down(state: int):
    state -= ROT_SPEED
    state = min(state, LIMIT_MAX)
    send_servo_command(TOP_CHAN, state)
    return state

def move_up(state: int):
    state += ROT_SPEED
    state = max(state, LIMIT_MIN)
    send_servo_command(TOP_CHAN, state)
    return state


sensor.reset()                      # Reset and initialize the sensor.
sensor.set_pixformat(sensor.GRAYSCALE) # Set pixel format to RGB565 (or GRAYSCALE)
sensor.set_framesize(sensor.QVGA)   # Set frame size to VGA (640x480)
sensor.skip_frames(time = 2000)     # Wait for settings take effect.

face_detect = image.HaarCascade("frontalface", stages=25)

# Initialize UART
uart = pyb.UART(3)
uart.init(9600, bits=8, parity=None, stop=1, timeout_char=1000)

TOP_CHAN = 0
BOT_CHAN = 1

# blob thresholds
#THRESHOLDS = [(50, 69, -5, 127, 23, 127)]
THRESHOLDS = [(0, 100, -17, 64, 40, 94), (31, 57, -24, -3, 24, 54)]
CENTER_THRESH = 20
CENTER_X = 160
CENTER_Y = 120

ROT_SPEED = 15  # Make this speed a function of the distance to the object.
LIMIT_MIN = 992
LIMIT_MAX = 2000

TOP_STATE = 1500
BOT_STATE = 1500
send_servo_command(TOP_CHAN, TOP_STATE)
send_servo_command(BOT_CHAN, BOT_STATE)


# Facial Detection
desc = None
while(desc is None):
    img = sensor.snapshot()
    blob = img.find_features(face_detect)
    if not blob:
        continue
    roi = (blob[0][0] - 31, blob[0][1] - 31, blob[0][2]+62, blob[0][3]+62)
    desc = img.find_keypoints(roi=roi, theshold=10, scale_factor=1.1)


print("Face found, starting tracking")
sensor.skip_frames(time=2000)


while(True):
    img = sensor.snapshot()         # Take a picture and return the image.
    # Draw center box
    img.draw_rectangle((CENTER_X-CENTER_THRESH,CENTER_Y-CENTER_THRESH,2*CENTER_THRESH,2*CENTER_THRESH))

    for blob in img.find_features(face_detect):
        img.draw_rectangle(blob, color=0)
        roi = (blob[0] - 31, blob[1] - 31, blob[2]+62, blob[3]+62)
        kpts = img.find_keypoints(threshold=10, scale_factor=1.1, normalized=True, roi=roi)
        count = image.match_descriptor(desc, kpts).count() if kpts else 0

        print(count)

        # if count >= 1:
            # continue
        img.draw_rectangle(blob)
        x,y,w,h = blob
        center_x = x+(w/2)
        center_y = y+(h/2)

        if center_x > (CENTER_X + CENTER_THRESH):
            BOT_STATE = move_left(BOT_STATE)
        elif center_x < (CENTER_X - CENTER_THRESH):
            BOT_STATE = move_right(BOT_STATE)

        if center_y > (CENTER_Y + CENTER_THRESH):
            TOP_STATE = move_up(TOP_STATE)
        elif center_y < (CENTER_Y - CENTER_THRESH):
            TOP_STATE = move_down(TOP_STATE)



