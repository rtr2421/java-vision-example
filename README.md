# Building on desktop

## Building

Java 11 is required to build.  Set your path and/or JAVA_HOME environment
variable appropriately.

1) Run "./gradlew build"

## Deploying

### On the rPi web dashboard

1. Make the rPi writable by selecting the "Writable" tab
2. In the rPi web dashboard Application tab, select the "Uploaded Java jar"
   option for Application
3. Click "Browse..." and select the "java-multiCameraServer-all.jar" file in
   your desktop project directory in the build/libs subdirectory
4. Click Save

The application will be automatically started.  Console output can be seen by
enabling console output in the Vision Status tab.

### Automatically

TODO: Wire in the `deploy` task

## Building locally on rPi

1. Run "./gradlew build"
2. Run "./install.sh" (replaces /home/pi/runCamera)
3. Run "./runInteractive" in /home/pi or "sudo svc -t /service/camera" to
   restart service.

## Using this project

The FRC vision framework manages the cameras and passes camera frames to a Pipeline that processes an image using OpenCV primitives. You can make these pipelines with GRIP and put the generated code directly in this project. Within the `Main` class, each frame is sent to the `process` method in the pipeline. Programmers write a lambda function that is passed an instance of the pipeline after `process` has been called.

A pipeline should provide getter methods to be able to inspect the output of the OpenCV calls. This is done for you if you used GRIP. The lambda then does any further calculations and then does something with the results, such as sending to a Network Table for action on the RIO.

For example, a pipeline may detect blobs, and provide a `MatOfKeyPoint findBlobsOutput()` method that returns a matrix of key points. The lambda might then calculate range or angle, and send it to a network table. This separation allows the team to iterate on the detection within GRIP without overwriting any calculation of the results.

Each vision pipeline is run in a thread, so it is possible to have multiple pipelines running.

## TODOs

* Wire up the `deploy` task (see the `jaci.gradle.EmbeddedTools` gradle plugin that wpilib uses)
* Extract the processing logic from `Main`