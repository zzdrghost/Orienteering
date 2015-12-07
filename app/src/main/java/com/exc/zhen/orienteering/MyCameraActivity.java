package com.exc.zhen.orienteering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Matrix;

/**
 * Created by ZHEN on 2015/10/11 0011.
 *
 */
public class MyCameraActivity extends Activity {
    private final static String TAG = "MyDebug";
    Camera.Parameters parameters;
    private ImageButton btn_camera_capture = null;
    private ImageButton btn_camera_cancel = null;
    private ImageButton btn_camera_ok = null;
    private TextView ztextView=null;
    private TextView xtextView=null;
    private TextView ytextView=null;
    private SensorManager sm;
    private Sensor aSensor,mSensor;
    private Bitmap bitmap = null;
    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];
    final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType())
                accelerometerValues = event.values;
            if (Sensor.TYPE_MAGNETIC_FIELD == event.sensor.getType())
                magneticFieldValues = event.values;
            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(R, values);
            if (0 > values[0])
                values[0] = (float) Math.toDegrees(values[0])+360f;
            else
                values[0] = (float) Math.toDegrees(values[0]);
            ztextView.setText(String.valueOf(values[0]));
            xtextView.setText(String.valueOf((float)Math.toDegrees(values[1])));
            ytextView.setText(String.valueOf((float)Math.toDegrees(values[2])));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };
    private Camera camera = null;
    private MySurfaceView mySurfaceView = null;

    private byte[] buffer = null;

    private final static String savePath = Environment.getExternalStorageDirectory().
            getPath()+"/orienteeringImg/";
    //private Uri imageUri;
    private String imagePath;
    private PictureCallback pictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null){
                Log.i("MyPicture", "picture taken data: null");
            }else{
                Log.i("MyPicture", "picture taken data: " + data.length);
            }

            buffer = new byte[data.length];
            buffer = data.clone();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "MyCameraActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_camera);
        Intent intent = getIntent();
        //imageUri = Uri.parse(intent.getStringExtra(CreateActivity.INTENT_EXTRA));
        imagePath = intent.getStringExtra(CreateActivity.INTENT_EXTRA);
        btn_camera_capture = (ImageButton) findViewById(R.id.camera_capture);
        btn_camera_ok = (ImageButton) findViewById(R.id.camera_ok);
        btn_camera_cancel = (ImageButton) findViewById(R.id.camera_cancel);
        btn_camera_capture.setVisibility(View.VISIBLE);
        btn_camera_ok.setVisibility(View.INVISIBLE);
        btn_camera_cancel.setVisibility(View.INVISIBLE);
        ztextView = (TextView) findViewById(R.id.z_rotation);
        xtextView = (TextView) findViewById(R.id.x_rotation);
        ytextView = (TextView) findViewById(R.id.y_rotation);

        btn_camera_capture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "MyCameraActivity btn_camera_capture");
                sm.unregisterListener(sensorListener);
                camera.takePicture(null, null, pictureCallback);
                btn_camera_capture.setVisibility(View.INVISIBLE);
                btn_camera_ok.setVisibility(View.VISIBLE);
                btn_camera_cancel.setVisibility(View.VISIBLE);
            }
        });
        btn_camera_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "MyCameraActivity btn_camera_ok");
                //保存图片
                saveImageToFile();
                Intent intent1 = new Intent();
                Bundle bundle = new Bundle();
                bundle.putFloat("zvalue", Float.valueOf(ztextView.getText().toString()));
                bundle.putFloat("xvalue", Float.valueOf(xtextView.getText().toString()));
                bundle.putFloat("yvalue", Float.valueOf(ytextView.getText().toString()));
                intent1.putExtras(bundle);
                MyCameraActivity.this.setResult(RESULT_OK, intent1);
                MyCameraActivity.this.finish();
            }
        });
        btn_camera_cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "MyCameraActivity btn_camera_cancel");
                camera.startPreview();
                sm.registerListener(sensorListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
                sm.registerListener(sensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                btn_camera_capture.setVisibility(View.VISIBLE);
                btn_camera_ok.setVisibility(View.INVISIBLE);
                btn_camera_cancel.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "MyCameraActivity onStart");
        super.onStart();
    }
    @Override
    protected void onRestart(){
        Log.i(TAG, "MyCameraActivity onRestart");
        super.onRestart();
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "MyCameraActivity onResume");
        super.onResume();
        //监听传感器
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(sensorListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(sensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (camera == null){
            camera = getCameraInstance();

            camera.setDisplayOrientation(getPreviewDegree(this));
            WindowManager wm = MyCameraActivity.this.getWindowManager();
            parameters = camera.getParameters();// 获取相机参数集
            List<Camera.Size> SupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size previewSize = SupportedPreviewSizes.get(11);// 从List取出Size
            parameters.setPreviewSize(previewSize.width,previewSize.height);
            List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
            Camera.Size pictureSize = supportedPictureSizes.get(1);// 从List取出Size
            parameters.setPictureSize(pictureSize.width,pictureSize.height);
            parameters.setPreviewFrameRate(5);  //设置每秒显示4帧
            parameters.setJpegQuality(80); // 设置照片质量
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//连续对焦
            camera.setParameters(parameters);
            camera.cancelAutoFocus();

        }
        //必须放在onResume中，不然会出现Home键之后，再回到该APP，黑屏
        mySurfaceView = new MySurfaceView(getApplicationContext(), camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mySurfaceView);
    }
    @Override
    protected void onPause() {
        Log.i(TAG, "MyCameraActivity onPause");
        super.onPause();
        sm.unregisterListener(sensorListener);
        camera.release();
        camera = null;
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "MyCameraActivity onStop");
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        Log.i(TAG, "MyCameraActivity onDestroy");
        if (null!=bitmap)
            bitmap.recycle();
        super.onDestroy();
    }
    /*得到一相机对象*/
    private Camera getCameraInstance(){
        Log.i(TAG, "MyCameraActivity getCameraInstance");
        Camera camera = null;
        try{
            camera = camera.open();
        }catch(Exception e){
            e.printStackTrace();
        }
        return camera;
    }

    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    public static int getPreviewDegree(Activity activity) {
        // 获得手机的方向
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        return degree;
    }
    //-----------------------保存图片---------------------------------------
    private void saveImageToFile(){
        Log.i(TAG, "MyCameraActivity saveImageToFile");
        //File file = new File(savePath+"test.jpg");
        File file = new File(imagePath);
        if (file == null){
            Toast.makeText(getApplicationContext(), "文件创建失败,请检查SD卡读写权限", Toast.LENGTH_SHORT).show();
            return ;
        }
        Log.i("MyPicture", "自定义相机图片路径:" + file.getPath());
        //Toast.makeText(getApplicationContext(), "图片保存路径：" + file.getPath(), Toast.LENGTH_SHORT).show();
        if (buffer == null){
            Log.i("MyPicture", "自定义相机Buffer: null");
        }else{
            try{
                FileOutputStream fos = new FileOutputStream(file);
                BitmapFactory.Options options = new BitmapFactory.Options();
                //图像采样4,缩小4倍，图像质量不变
                options.inSampleSize=4;
                bitmap = BitmapFactory.decodeByteArray(buffer,0,buffer.length,options);
                // 根据拍摄的方向旋转图像（纵向拍摄时要需要将图像选择90度)
                Matrix matrix = new Matrix();
                matrix.setRotate(getPreviewDegree(this));
                bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                fos.flush();
                fos.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

}
