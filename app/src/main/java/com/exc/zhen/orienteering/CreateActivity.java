package com.exc.zhen.orienteering;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateActivity extends Activity {
    private final static String TAG = "MyDebug";
    private int cur_msid;
    private Mission cur_Mission;
    private List<MsPoint> cur_PointList;
    private MsPoint cur_Point;
    private DbManager dmr;
    private EditText mission_title,question,answer;
    private Button p_get;
    private ImageView imageView;
    private final static String savePath = Environment.getExternalStorageDirectory().
            getPath()+"/orienteeringImg/";
    private Uri imageUri;
    private String imageName;
    public static final int TAKE_PHOTO = 1;
    public static final int CROP_PHOTO = 2;
    private LocationClient locationClient;

//    private SensorManager sm;
//    private Sensor aSensor,mSensor;
//    float[] accelerometerValues = new float[3];
//    float[] magneticFieldValues = new float[3];

    private boolean param_get = true;
    private boolean positioning = false;

//    final SensorEventListener sensorListener = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType())
//                accelerometerValues = event.values;
//            if (Sensor.TYPE_MAGNETIC_FIELD == event.sensor.getType())
//                magneticFieldValues = event.values;
//            float[] values = new float[3];
//            float[] R = new float[9];
//            SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
//            SensorManager.getOrientation(R, values);
//            if (0 > values[0])
//                values[0] = (float) Math.toDegrees(values[0])+360f;
//            else
//                values[0] = (float) Math.toDegrees(values[0]);
//            answer.setText(values[0]+"");
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_create);
        Log.i(TAG, "CreateActivity onCreate");
        initData();
        initView();
        if (-1 != cur_msid){
            mission_title.setText(cur_Mission.name);
        }
        //获取orientation参数
//        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        sm.registerListener(sensorListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        sm.registerListener(sensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //获取MainActivity传递的数据,初始化数据
    private void initData(){
        dmr = new DbManager(this);
        cur_Point = new MsPoint();
        Intent intent = getIntent();
        cur_msid = intent.getIntExtra(MainActivity.INTENT_EXTRA, -1);
        if (-1 != cur_msid){
            cur_Mission = dmr.getMission(cur_msid);
            cur_PointList = dmr.queryMsPoint(cur_msid);
            cur_Point.order_num = cur_PointList.size()+1;
        }else {
            if (null != dmr.getLastMission())
                cur_msid = dmr.getLastMission().mission_id + 1;
            else cur_msid = 1;
            cur_Mission = new Mission();
            cur_PointList = new ArrayList<>();
            cur_Point.order_num = cur_PointList.size()+1;
        }

    }

    //绑定对应控件
    private void initView() {
        mission_title = (EditText) findViewById(R.id.enter_title);
        question = (EditText) findViewById(R.id.enter_question);
        answer = (EditText) findViewById(R.id.enter_answer_c);
//        p_before = (Button) findViewById(R.id.point_before_c);
        p_get = (Button) findViewById(R.id.position_get);
//        p_next = (Button) findViewById(R.id.point_next_c);
        imageView = (ImageView) findViewById(R.id.imageView_c);

        p_get.setText(Integer.toString(cur_Point.order_num));
    }

    //获取手机参数
    private void get_phone_param() {
        locationClient = new LocationClient(this);
        locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (null != bdLocation) {
                    cur_Point.latitude = bdLocation.getLatitude();
                    cur_Point.longitude = bdLocation.getLongitude();
                    cur_Point.height = bdLocation.getAltitude();
                    Toast.makeText(CreateActivity.this, "定位完成\n" + cur_Point.latitude
                            + "," + cur_Point.longitude + "\n" + cur_Point.height, Toast.LENGTH_LONG).show();
                    p_get.setTextColor(Color.RED);
                    param_get = true;//判断定位成功
                    positioning = false;//定位结束
                } else {
                    p_get.setTextColor(Color.BLACK);
                    param_get = false;
                    Toast.makeText(CreateActivity.this, "定位失败，请重新定位", Toast.LENGTH_SHORT).show();
                    positioning = false;
                }
                locationClient.unRegisterLocationListener(this);
                locationClient.stop();
            }
        });
        LocationClientOption locateOption = new LocationClientOption();
        locateOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        locateOption.setCoorType("bd09ll");
        locateOption.setNeedDeviceDirect(true);
        locateOption.setScanSpan(10);
        locationClient.setLocOption(locateOption);
        locationClient.start();
        locationClient.requestLocation();
    }
    // 判断当前point是否成功获取有效手机参数
    private boolean get_phone_param_done(MsPoint msp){
        return (!(msp.latitude == 0.0 || msp.longitude == 0.0) && param_get);
    }
    // 判断当前point是否可以保存
    private boolean isValidPoint(MsPoint msp){
        return (get_phone_param_done(msp) && !"".equals(cur_Point.img_address));
    }
    //将当前point插入pointList（更新及插入）
    private void updateList(){
        if (cur_Point.order_num <= cur_PointList.size())
            cur_PointList.set(cur_Point.order_num-1,cur_Point);
        else
            cur_PointList.add(cur_Point);
    }
    //把当前point的数据赋给各View
    private void setCurP2View(MsPoint msp){
        p_get.setText(Integer.toString(msp.order_num));
        if (get_phone_param_done(msp)){
            p_get.setTextColor(Color.RED);
        }else {
            p_get.setTextColor(Color.BLACK);
        }
        question.setText(msp.question);
        answer.setText(msp.answer);
        //图片解析成Bitmap对象
        BitmapFactory.Options bitmapOption = new BitmapFactory.Options();
        bitmapOption.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(cur_Point.img_address,bitmapOption);
        imageView.setImageBitmap(bitmap);
    }

    //把当前View的值付给当前point
    private void getView2P(){
        cur_Point.question = question.getText().toString();
        cur_Point.answer = answer.getText().toString();
    }
    public void point_before_c_click(View view) {
        if (!positioning){
            param_get = true;
            getView2P();
            if (isValidPoint(cur_Point))
                updateList();
            if (cur_Point.order_num-1 != 0){
                cur_Point = cur_PointList.get(cur_Point.order_num-2);
                setCurP2View(cur_Point);
            }
        }
    }
    //点击imageView获取照片
    public void imageView_c_click(View view) {
        new AlertDialog.Builder(this)
                .setTitle("拍照")
                .setMessage("拍张提示照片")
                .setNegativeButton("取消",null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        imageName =  cur_msid + "_" + cur_Point.order_num;
                        String sdStatus = Environment.getExternalStorageState();
                        if(!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用
                            return;
                        }
                        File imgDir = new File(savePath);
                        imgDir.mkdirs();
                        File outputImage = new File(imgDir,imageName+".jpg");
                        try {
                            if(outputImage.exists()) {
                                outputImage.delete();
                            }
                            outputImage.createNewFile();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                        imageUri = Uri.fromFile(outputImage);
                        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        startActivityForResult(intent,TAKE_PHOTO);
                    }
                }).show();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(CreateActivity.this, "ActivityResult " + resultCode + "error",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            //图片解析成Bitmap对象
            Bitmap bitmap = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(imageUri));
            cur_Point.img_address = imageUri.getPath();
            Toast.makeText(CreateActivity.this, imageUri.getPath(), Toast.LENGTH_LONG).show();
            imageView.setImageBitmap(bitmap); //将照片显示出来
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void position_get_click(View view) {
        if (!positioning){
            if (get_phone_param_done(cur_Point)){
                new AlertDialog.Builder(this)
                        .setMessage("要重新获取任务点位置信息吗？")
                        .setNegativeButton("取消",null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                positioning = true;
                                get_phone_param();
                            }
                        }).show();
            }else {
                positioning = true;
                get_phone_param();
            }
        }
    }

    public void point_next_c_click(View view) {
        if (!positioning){
            param_get = true;
            getView2P();
            if (isValidPoint(cur_Point)){
                updateList();
                if (cur_Point.order_num+1 > cur_PointList.size()){
                    cur_Point = new MsPoint();
                    cur_Point.order_num = cur_PointList.size()+1;
                    setCurP2View(cur_Point);
                }else{
                    cur_Point = cur_PointList.get(cur_Point.order_num);
                    setCurP2View(cur_Point);
                }
            }else {
                Toast.makeText(this,"当前任务点未创建完成",Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.save) {
            getView2P();
            if (isValidPoint(cur_Point))
                updateList();
            if ("".equals(mission_title.getText().toString()))
                Toast.makeText(CreateActivity.this,"请输入任务名称",Toast.LENGTH_SHORT).show();
            else {
                cur_Mission.name = mission_title.getText().toString();
                cur_Mission.limit_time = "";
                cur_Mission.state = "创建中";
                dmr.addMission(cur_Mission);
                cur_Mission = dmr.getLastMission();
                for (MsPoint msp:cur_PointList) {
                    msp.mission_id = cur_Mission.mission_id;
                }
                dmr.addPoint(cur_PointList);
                Toast.makeText(CreateActivity.this,"保存了"+Integer.toString(cur_PointList.size())+
                        "个任务点",Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        else if (id == R.id.create_done) {
            getView2P();
            if (isValidPoint(cur_Point))
                updateList();
            if ("".equals(mission_title.getText().toString()))
                Toast.makeText(CreateActivity.this,"请输入任务名称",Toast.LENGTH_SHORT).show();
            else {
                new AlertDialog.Builder(CreateActivity.this)
                        .setTitle("完成创建")
                        .setMessage("共"+Integer.toString(cur_PointList.size())+"个任务点")
                        .setNegativeButton("取消",null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cur_Mission.name = mission_title.getText().toString();
                                cur_Mission.limit_time = "";
                                cur_Mission.state = "未完成";
                                dmr.addMission(cur_Mission);
                                cur_Mission = dmr.getLastMission();
                                for (MsPoint msp:cur_PointList) {
                                    msp.mission_id = cur_Mission.mission_id;
                                }
                                dmr.addPoint(cur_PointList);
                                CreateActivity.this.finish();
                            }
                        }).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "CreateActivity onStart");
        super.onStart();
    }
    @Override
    protected void onRestart(){
        Log.i(TAG, "CreateActivity onRestart");
        super.onRestart();
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "CreateActivity onResume");
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.i(TAG, "CreateActivity onPause");
//        sm.unregisterListener(sensorListener);
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.i(TAG, "CreateActivity onStop");
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        Log.i(TAG, "CreateActivity onDestroy");
        dmr.closeDB();
        super.onDestroy();
    }
}
