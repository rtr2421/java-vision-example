package processors;

import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.vision.VisionThread;
import pipelines.MyPipeline;

public class MyPipelineProcessor {
    private NetworkTableEntry output;
    private VideoSource camera;

    public MyPipelineProcessor(VideoSource camera, NetworkTableEntry output) {
        this.output = output;
        this.camera = camera;
    }

    public boolean run() {
        VisionThread visionThread = new VisionThread(camera, new MyPipeline(), pipeline -> {
            p.inspect(pipeline);
        });

        visionThread.start();
        return true;
    }

    public void inspect(MyPipeline in) {
        // do something with pipeline results
        System.out.printf("processing generation %d\n", in.getVal());
        output.setNumber(in.getVal());
    }
}