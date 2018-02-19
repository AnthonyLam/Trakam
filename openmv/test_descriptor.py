# Untitled - By: anthony - Sun Feb 18 2018

import sensor, image, time

sensor.reset()
sensor.set_pixformat(sensor.GRAYSCALE)
sensor.set_framesize(sensor.QVGA)
sensor.skip_frames(time = 2000)

face_detect = image.HaarCascade("frontalface")
desc = None

while(True):
    img = sensor.snapshot()
    for blob in img.find_features(face_detect):
        img.draw_rectangle(blob, color=0)
        roi = (blob[0] - 31, blob[1] - 31, blob[2]+62, blob[3]+62)
        temp = img.find_keypoints(roi=roi, theshold=10, scale_factor=1.1)
        if temp is not None and desc is None:
            desc = temp
        elif temp:
            if image.match_descriptor(desc, temp).count() > 5:
                img.draw_rectangle(blob)
