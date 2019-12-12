/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionThread;

/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

public final class Main {
  private static String configFile = "/boot/frc.json";
  private static List<VideoSource> cameras = new ArrayList<>();
  private static Config config = new Config();
  private static final String visiontable = "raspberrypi";

  private Main() {
  }

  /**
   * Start running the camera.
   */
  public static VideoSource startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }

    return camera;
  }

  /**
   * Start running the switched camera.
   */
  public static MjpegServer startSwitchedCamera(SwitchedCameraConfig cameraConfig) {
    System.out.println("Starting switched camera '" + cameraConfig.name + "' on " + cameraConfig.key);
    MjpegServer server = CameraServer.getInstance().addSwitchedCamera(cameraConfig.name);

    NetworkTableInstance.getDefault().getEntry(cameraConfig.key).addListener(event -> {
      if (event.value.isDouble()) {
        int i = (int) event.value.getDouble();
        if (i >= 0 && i < cameras.size()) {
          server.setSource(cameras.get(i));
        }
      } else if (event.value.isString()) {
        String str = event.value.getString();
        for (int i = 0; i < config.getCameraConfigs().size(); i++) {
          if (str.equals(config.getCameraConfigs().get(i).name)) {
            server.setSource(cameras.get(i));
            break;
          }
        }
      }
    }, EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    return server;
  }

  /**
   * Main.
   */
  public static void main(String... args) {
    if (args.length > 0) {
      configFile = args[0];
    }

    // read configuration
    if (!config.readConfig(configFile)) {
      return;
    }

    // start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    if (config.isServer()) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + config.getTeam());
      ntinst.startClientTeam(config.getTeam());
    }

    // start cameras
    for (CameraConfig camera : config.getCameraConfigs()) {
      cameras.add(startCamera(camera));
    }

    // start switched cameras
    for (SwitchedCameraConfig camera : config.getSwitchedCameraConfigs()) {
      startSwitchedCamera(camera);
    }

    NetworkTable table = ntinst.getTable(visiontable);
    NetworkTableEntry key = table.getEntry("frame");

    // start image processing on camera 0 if present
    if (cameras.size() >= 1) {
      VisionThread visionThread = new VisionThread(cameras.get(0), new MyPipeline(), pipeline -> {
        // do something with pipeline results
        System.out.printf("processing generation %d\n", pipeline.getVal());
        key.setNumber(pipeline.getVal());
      });

      visionThread.start();
    }

    // loop forever
    for (;;) {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException ex) {
        return;
      }
    }
  }
}
