/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.urugn.openvx;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;

/**
 *
 * @author UruGN
 */
public class Main extends Application implements Runnable {

    VxCanvas vxCanvas = new VxCanvas();

    @Override
    public void start(Stage primaryStage) {

        //cap.read(frame);
//        JFrame jframe = new JFrame("Video"); // the lines below create a frame to display the resultant video with object detection and localization//
//        JLabel vidpanel = new JLabel();
//        jframe.setContentPane(vidpanel);
//        jframe.setSize(600, 600);
//        jframe.setVisible(true);// we instantiate the frame here//
        BorderPane root = new BorderPane();
//        Button btn = new Button();
//        btn.setText("Say 'Hello World'");
//        btn.setOnAction(new EventHandler<ActionEvent>() {
//
//            @Override
//            public void handle(ActionEvent event) {
//                System.out.println("Hello World!");
//            }
//        });
//        root.setTop(btn);

        root.setCenter(vxCanvas);

        Scene mScene = new Scene(root, 300, 250);
        mScene.widthProperty().add(vxCanvas.getFitWidth());
        mScene.heightProperty().add(vxCanvas.getFitHeight());

        primaryStage.setTitle("OpenVx Implementation!");
        primaryStage.setScene(mScene);
        primaryStage.show();

        //initialize the cv drawing thread
        Thread t = new Thread(this);
        t.start();

    }

    @Override
    public void run() {

        String dir = System.getProperty("user.dir");
        System.load(dir + File.separator + "opencv" + File.separator + "libopencv_java410.so"); // Load the openCV 4.0 dll //

        String modelWeights = dir + File.separator + "yolo" + File.separator + "yolov3.weights"; //Download and load only wights for YOLO , this is obtained from official YOLO site//
        String modelConfiguration = dir + File.separator + "yolo" + File.separator + "yolov3.cfg";//Download and load cfg file for YOLO , can be obtained from official site//
        String filePath = dir + File.separator + "media" + File.separator + "cars.mp4"; //My video  file to be analysed//
        System.out.println("Loading file: " + filePath);

        //List
        List<String> names = new ArrayList<>();
        String namePath = dir + File.separator + "yolo" + File.separator + "coco.names"; //My video  file to be analysed//
        try ( Stream<String> stream = Files.lines(Paths.get(namePath))) {
            stream.forEach(line -> names.add(line));
        } catch (IOException ex) {

        }

        VideoCapture cap = new VideoCapture(filePath);// Load video using the videocapture method//
        Mat frame = new Mat(); // define a matrix to extract and store pixel info from video//
        Mat dst = new Mat();

        Net net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights); //OpenCV DNN supports models trained from various frameworks like Caffe and TensorFlow. It also supports various networks architectures based on YOLO//
        //Thread.sleep(5000);

        //Mat image = Imgcodecs.imread("D:\\yolo-object-detection\\yolo-object-detection\\images\\soccer.jpg");
        Size sz = new Size(288, 288);

        List<Mat> result = new ArrayList<>();
        List<String> outBlobNames = getOutputNames(net);
        int f = 0;
        while (true) {

            if (cap.read(frame)) {

                Mat blob = Dnn.blobFromImage(frame, 0.00392, sz, new Scalar(0), true, false); // We feed one frame of video into the network at a time, we have to convert the image to a blob. A blob is a pre-processed image that serves as the input.//
                net.setInput(blob);

                net.forward(result, outBlobNames); //Feed forward the model to get output //

                // outBlobNames.forEach(System.out::println);
                // result.forEach(System.out::println);
                float confThreshold = 0.6f; //Insert thresholding beyond which the model will detect objects//
                List<Integer> clsIds = new ArrayList<>();
                List<Float> confs = new ArrayList<>();
                List<Rect> rects = new ArrayList<>();
                for (int i = 0; i < result.size(); ++i) {
                    // each row is a candidate detection, the 1st 4 numbers are
                    // [center_x, center_y, width, height], followed by (N-4) class probabilities
                    Mat level = result.get(i);
                    for (int j = 0; j < level.rows(); ++j) {
                        Mat row = level.row(j);
                        Mat scores = row.colRange(5, level.cols());
                        Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                        float confidence = (float) mm.maxVal;
                        Point classIdPoint = mm.maxLoc;
                        if (confidence > confThreshold) {
                            int centerX = (int) (row.get(0, 0)[0] * frame.cols()); //scaling for drawing the bounding boxes//
                            int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                            int width = (int) (row.get(0, 2)[0] * frame.cols());
                            int height = (int) (row.get(0, 3)[0] * frame.rows());
                            int left = centerX - width / 2;
                            int top = centerY - height / 2;

                            clsIds.add((int) classIdPoint.x);
                            confs.add((float) confidence);
                            rects.add(new Rect(left, top, width, height));
                        }
                    }
                }
                float nmsThresh = 0.5f;
                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
                Rect[] boxesArray = rects.toArray(new Rect[0]);
                MatOfRect boxes = new MatOfRect(boxesArray);
                MatOfInt indices = new MatOfInt();
                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices); //We draw the bounding boxes for objects here//

                int[] ind = indices.toArray();
                int j = 0;
                for (int i = 0; i < ind.length; ++i) {
                    int idx = ind[i];
                    Rect box = boxesArray[idx];
                    Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(002, 78, 255), 2);
                    float value = confs.get(idx);

                    DecimalFormat df = new DecimalFormat("0.00");
                    String formate = df.format(value * 100f);
                    Imgproc.putText(frame, formate + "% " + names.get(clsIds.get(idx)), new Point(box.tl().x, box.tl().y - 5), 2, .6, new Scalar(002, 205, 255));
                    //i=j;

                    System.out.println(idx);
                }
                // Imgcodecs.imwrite("D://out.png", image);
                //System.out.println("Image Loaded");
//                ImageIcon image = new ImageIcon(Mat2bufferedImage(frame)); //setting the results into a frame and initializing it //

                //repaint canvas
                BufferedImage bufImage = Mat2bufferedImage(frame);
                Image fxImage = SwingFXUtils.toFXImage(bufImage, null);
                vxCanvas.update(fxImage);

                //dump the stream into a file.
                try {
                    File tmpFile = new File(f + "_imgTemp.jpg");
                    tmpFile.createNewFile();
                    FileOutputStream fout = new FileOutputStream(tmpFile);
                    ImageIO.write(bufImage, "jpg", fout);
                    f++;

                    //now delete the file
//            boolean dltd = tmpFile.delete();
//            if (!dltd) {//try to delete later
//
//            }
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
//                vidpanel.setIcon(image);
//                vidpanel.repaint();
                // System.out.println(j);
                // System.out.println("Done");
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private static List<String> getOutputNames(Net net) {
        List<String> names = new ArrayList<>();

        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String> layersNames = net.getLayerNames();

        outLayers.forEach((item) -> names.add(layersNames.get(item - 1)));//unfold and create R-CNN layers from the loaded YOLO model//
        return names;
    }

//	}
    private static BufferedImage Mat2bufferedImage(Mat image) {   // The class described here  takes in matrix and renders the video to the frame  //
        MatOfByte bytemat = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, bytemat);
        byte[] bytes = bytemat.toArray();
        InputStream in = new ByteArrayInputStream(bytes);
        BufferedImage img = null;
        try {
            img = ImageIO.read(in);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return img;
    }

}
