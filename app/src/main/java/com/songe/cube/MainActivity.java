package com.songe.cube;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.os.Bundle;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class MainActivity extends CameraActivity implements CvCameraViewListener2, View.OnClickListener{
    private static final String TAG = "OCVSample::Activity";

    private Mat mrgba = null;
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private SeekBar rlower = null;
    private SeekBar glower = null;
    private SeekBar blower = null;

    private SeekBar rupper = null;
    private SeekBar gupper = null;
    private SeekBar bupper = null;

    private Button toastButton;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.textureView);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        rlower = (SeekBar)findViewById(R.id.rlower);
        glower = (SeekBar)findViewById(R.id.glower);
        blower = (SeekBar)findViewById(R.id.blower);

        rupper = (SeekBar)findViewById(R.id.rupper);
        gupper = (SeekBar)findViewById(R.id.gupper);
        bupper = (SeekBar)findViewById(R.id.bupper);

        toastButton = findViewById(R.id.btntoast);
        toastButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        Context context = getApplicationContext();
        Toast.makeText(context, "Lower R: " + rlower.getProgress() + " G: " + glower.getProgress() + " B: " + blower.getProgress() +
                " Upper R: " + rupper.getProgress() + " G: " + gupper.getProgress() + " B: " + bupper.getProgress()  , Toast.LENGTH_LONG).show();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);

        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {
    }

    private void captureBitmap(){

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mrgba = inputFrame.rgba();

        Bitmap bitmap = Bitmap.createBitmap(mOpenCvCameraView.getWidth()/4,mOpenCvCameraView.getHeight()/4, Bitmap.Config.ARGB_8888);
        try {
            bitmap = Bitmap.createBitmap(mrgba.cols(), mrgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mrgba, bitmap);
            /*ImageView mBitmap = null;
            mBitmap.setImageBitmap(bitmap);
            mBitmap.invalidate();*/
            mrgba = drawContours(bitmap);

        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }



        return mrgba;
    }

    private Mat drawContours(Bitmap sourceBitmap) {
        Mat sourceMat = new Mat(sourceBitmap.getWidth(), sourceBitmap.getHeight(), CvType.CV_8UC3);
        Utils.bitmapToMat(sourceBitmap, sourceMat);
        Mat roiTmp = sourceMat.clone();
        double bitmapWidth = sourceBitmap.getWidth();
        Log.e("bitmapWidth", String.valueOf(bitmapWidth));
        final Mat hsvMat = new Mat();
        sourceMat.copyTo(hsvMat);


        // convert mat to HSV format for Core.inRange()
        Imgproc.cvtColor(hsvMat, hsvMat, Imgproc.COLOR_RGB2HSV);

        Vector<Scalar> lower = new Vector<Scalar>();

        lower.add(new Scalar(blower.getProgress(), glower.getProgress(), rlower.getProgress()));

        Vector<Scalar> upper = new Vector<Scalar>();

        upper.add(new Scalar(bupper.getProgress(), gupper.getProgress(), rupper.getProgress()));

        ArrayList<String> colors = new ArrayList<>();
        colors.add("C");

        for (int ranges = 0; ranges < lower.size(); ranges++) {
            Core.inRange(hsvMat, lower.get(ranges), upper.get(ranges), roiTmp);

            Vector<MatOfPoint> contours = new Vector<>();
            Imgproc.findContours(roiTmp, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            List<Moments> mu = new ArrayList<Moments>(contours.size());
            for (int i = 0; i < contours.size(); i++) {

            }
            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                mu.add(contourIdx, Imgproc.moments(contours.get(contourIdx), false));
                Moments p = mu.get(contourIdx);
                if ((contours.get(contourIdx).size(0)) > 70) {
                    int x = (int) (p.get_m10() / p.get_m00());
                    int y = (int) (p.get_m01() / p.get_m00());
                    //Imgproc.circle(sourceMat, new Point(x, y), 4, new Scalar(255, 49, 0, 255));
                    Imgproc.drawContours(sourceMat, contours, contourIdx, new Scalar(0, 0, 0) , 7);
                    Imgproc.putText (
                            sourceMat,                          // Matrix obj of the image
                            colors.get(ranges),          // Text to be added
                            new Point(x, y),               // point
                            Imgproc.FONT_HERSHEY_SIMPLEX ,      // front face
                            1,                               // front scale
                            new Scalar(0, 0, 0),             // Scalar object for color
                            3                                // Thickness
                    );
                }
            }
        }
        return sourceMat;
    }
}