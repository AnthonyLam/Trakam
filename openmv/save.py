 #img.draw_rectangle((CENTER_X-CENTER_THRESH,CENTER_Y-CENTER_THRESH,2*CENTER_THRESH,2*CENTER_THRESH), color=0)
    #roi = (rect[0] - 10, blob[1] - 10, blob[2]+20, blob[3]+20)
    #img.draw_rectangle(roi,color=(0,0,255))
    #for blob in img.find_blobs(THRESHOLDS, merge=True, pixel_threshold=200, area_threshold=1000, margin=10):
        #img.draw_rectangle(blob.rect(), color=(255,0,0))
        #print("ROI: ", blob.rect())
        #for rect in img.find_rects(roi=blob.rect(),):

            #x,y,w,h = rect.rect()

            #x = max(min(x, WIDTH), 0)
            #y = max(min(y, HEIGHT), 0)
            #w = max(min(w, WIDTH-1), 1)
            #h = max(min(h, HEIGHT-1), 1)
            #print(x,y,w,h)

            #img.draw_rectangle((x,y,w,h), color=(0,255,0))

            #center_x = x+(w/2)
            #center_y = y+(h/2)

            #if center_x > (CENTER_X + CENTER_THRESH):
                #BOT_STATE = move_left(BOT_STATE)
            #elif center_x < (CENTER_X - CENTER_THRESH):
                #BOT_STATE = move_right(BOT_STATE)

            #if center_y > (CENTER_Y + CENTER_THRESH):
                #TOP_STATE = move_up(TOP_STATE)
            #elif center_y < (CENTER_Y - CENTER_THRESH):
                #TOP_STATE = move_down(TOP_STATE)
            #break
