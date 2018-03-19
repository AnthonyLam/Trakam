# Trakam - Final Report

- Professor Glenn Healey
- Anthony Lam
- Darshan Parajuli
- Masa Maeda

## Initial Plan
The original idea was to create a camera tracking system that could track a given object through a combination of machine vision and machine learning. The concept involved realtime tracking to detect any given object.
Machine learning could be applied for shape and color detection to quickly filter out invalid objects. Our initial thoughts were that network round trips for each frame would be too costly to implement and too slow to utilize.

## The Hardware
Hardware in our initial implementation involved using a simple “dumb” camera paired with an FPGA with custom logic built in to implement the necessary machine vision implementations. In addition, we planned on using custom machine learning hardware built on TensorFlow to perform a deeper analysis after the machine vision hardware filter.

Unfortunately, the cost of implementing on an FPGA + custom machine learning was too costly to implement time wise. With just 3 months of development time, there was no chance for a long running iteration. Instead, our team opted to forgo the typical Gantt chart process for a process more akin to typical software engineering paradigms such as SCRUM. This allowed us to iterate much faster but required spreading our three teammates thin in order to get more done in a shorter period of time.

![OpenMV Camera](https://raw.githubusercontent.com/AnthonyLam/Trakam/master/docs/openmv_board2.jpg)

Ultimately, our team decided on the OpenMV camera with built-in OpenCV methods and a Raspberry PI to do all of our processing with as little hardware as possible. For the initial implementation with tracking, our team used the Pololu Maestro servo controller to power a separate set of servos over UART.

## Tracking Color
As an initial implementation, we began working on tracking a small blob of orange color on a white background. In ideal lighting conditions, our working implementation worked well using simple thresholding. Using the OpenMV board, we used UART to communicate with the Pololu Maestro and send the commands required to keep the blob in the center of the camera’s field of view.

![Blob Tracking Video](https://media.giphy.com/media/41ey7tYsUSsn90Tz7g/giphy.gif)


## Tracking Rectangles
Next step was to track rectangles, i.e a red book. However, the rectangle tracking algorithm used by OpenMV did not yield good results. Even after spending a lot of time tweaking the parameters, the end result was not even close to our goal.

As a result, our team ended up jumping straight to tracking faces using Haar Cascades as they were more accurate at tracking any face than the standard Hough transform was at tracking rectangles within the field of view. The Hough transform implementation tended to be overzealous at finding rectangles where our implementation would begin tracking hands that are holding rectangles rather than the bright red rectangle within the frame.

## Tracking Faces
Using a built in Haar Cascade pattern, the OpenMV camera was able to efficiently track faces and follow them around at a respectable pace. An unfortunate effect of using Haar cascades however, was the required use of grayscale footage at a lower framerate. The effect of resolution and color space would become quite apparent as we went through our project; a high resolution and the inclusion of color significantly impacts the framerate of the feed while affecting the performance of the machine learning algorithm.

Even though tracking one person’s face seemed trivial via face detection, the key problem became crystal clear when more than one face was on the camera’s field of view. In order to track a particular face, we realized we’d need to implement a real-time facial recognition system. We failed to recognize the level of difficulty involved in even trying to solve that problem. Using machine learning via TensorFlow library seemed obvious. However, we needed to recognize a face almost per frame while running on Raspberry PI. Even though TensorFlow took less than a second on the computer, which is already not good enough, it took more than 2 seconds on the Raspberry PI. Thus using machine learning locally didn’t seem feasible without significantly more expensive and powerful hardware.

## Pivot - Security Camera

Based on our experiences with implementing object/face tracking, our team was faced with the challenge of implementing our entire project in the three weeks before Winter Design Review. With this in mind, we had several options to present a reasonable project at our booth for review:

1. A partial implementation of our initial design that worked sometimes.
2. A reimplementation of our initial design that had only one function.
3. Pivot our entire project to something that we knew we could do with increased functionality.

Eventually, our team chose option 3 out of a desire to present a completed device. Instead of implementing our project using object detection and recognition combined with servos, we instead opted to implement facial recognition with a notification system, a steady livestream and a companion Android application.

![Application Snapshot](https://raw.githubusercontent.com/AnthonyLam/Trakam/master/docs/app_screenshot2.jpg)

## Structural Diagram

## Summary
If we were to describe our final design project with one word we would most likely describe it as: incomplete. With a little more time devoted to our pivoted idea and less time devoted to our initial ambitious task, we could have definitely implemented a more robust system with a solid feature set that worked well under any circumstance. As it is, our project has troubles with spotty Wi-Fi connections as well as occasional crashes due to overheating and too many detected faces being passed to the recognition subsystem.
### Takeaways
With this in mind, here are a few key takeaways that we gathered based on our experiences with this project as a whole:
- Start prototyping as early as possible
    * Allows you to notice problems early on which gives you more time for working on solutions.
- Have a dedicated work and meeting space.
    * After setting a specific time and location to meet on weekends our group became significantly more productive due to being able to communicate quickly and iterate effectively. Our initial attempt was to have weekly meetings at the Anthill Pub and work independently after our weekly check-ins. This did not hold us accountable for the work we were responsible for and led to many missed deadlines and inferior results.
- Think small and expand before attempting more complex designs.
    * Our mentor for this project, Professor Glenn Healey, consistently advised us to backtrack and take smaller steps with our project in order to avoid the numerous mistakes that we stumbled upon.
    * When taking small steps, we found that we could fix the issues that arose early on in the initial implementations rather than searching for bugs further down the line in our more complex designs.
