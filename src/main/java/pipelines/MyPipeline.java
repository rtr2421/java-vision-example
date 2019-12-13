package pipelines;

import org.opencv.core.Mat;

import edu.wpi.first.vision.VisionPipeline;

/**
 * Example pipeline.
 */
public class MyPipeline implements VisionPipeline {
    private int val;

    @Override
    public void process(Mat mat) {
        val += 1;
        
    }

    public int getVal() {
        return this.val;
    }
}