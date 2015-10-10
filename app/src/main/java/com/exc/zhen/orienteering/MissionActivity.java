package com.exc.zhen.orienteering;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MissionActivity extends Activity {
    private final static String TAG = "MyDebug";
    private final static String savePath = Environment.getExternalStorageDirectory().
            getPath()+"/orienteeringImg/";
    private int cur_msid;
    private Mission cur_Mission;
    private List<MsPoint> cur_PointList;
    private List<Boolean> answers_done;
    private MsPoint cur_Point;
    private DbManager dmr;
    private TextView mission_title,question;
    private EditText answer;
    private Button p_confirm;
    private ImageView imageView;
    private String mission_cnt_str;
    private LocationClient locationClient;
    private double latitude,longitude,height,orientation;
    private boolean positioning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_mission);
        Log.i(TAG, "MissionActivity onCreate");
        initData();
        initView();
        Toast.makeText(this,"当前任务有"+Integer.toString(cur_PointList.size())+
                "个任务点",Toast.LENGTH_SHORT).show();
    }
    //获取MainActivity传递的数据,初始化数据
    private void initData(){
        dmr = new DbManager(this);
        Intent intent = getIntent();
        cur_msid = intent.getIntExtra(MainActivity.INTENT_EXTRA, -1);
        cur_Mission = dmr.getCurrentMission();
        cur_PointList = dmr.queryMsPoint(cur_msid);
        mission_cnt_str = "/"+Integer.toString(cur_PointList.size());
        answers_done = new ArrayList<>();
        for ( MsPoint p: cur_PointList) {
            if ("已完成".equals(p.state))
                answers_done.add(true);
            else
                answers_done.add(false);
        }
        cur_Point = dmr.getCurrentPoint(cur_msid);
    }

    //绑定对应控件
    private void initView() {
        mission_title = (TextView) findViewById(R.id.current_mission_title);
        question = (TextView) findViewById(R.id.current_question);
        answer = (EditText) findViewById(R.id.answer_question);
//        p_before = (Button) findViewById(R.id.point_before_m);
        p_confirm = (Button) findViewById(R.id.position_confirm);
//        p_next = (Button) findViewById(R.id.point_next_m);
        imageView = (ImageView) findViewById(R.id.imageView_m);

        mission_title.setText(cur_Mission.name);
        question.setText(cur_Point.question);
        if (answers_done.get(cur_Point.orderNum-1)){
            answer.setText(cur_Point.answer);
            displayImg(cur_Point);
        }else {
            imageView.setImageBitmap(null);
        }
        p_confirm.setText(String.format("%s%s", cur_Point.orderNum, mission_cnt_str));

    }
    private void displayImg(MsPoint msp){
        //图片解析成Bitmap对象
        BitmapFactory.Options bitmapOption = new BitmapFactory.Options();
        bitmapOption.inSampleSize = 4;
        if (!"".equals(msp.imgAddress)){
            Bitmap bitmap = BitmapFactory.decodeFile(savePath+msp.imgAddress,bitmapOption);
            imageView.setImageBitmap(bitmap);
        }
    }

    //TODO 确认位置等手机参数正确
    private void confirm_position(){
        locationClient = new LocationClient(this);
        locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (null != bdLocation){
                    boolean FoundPoint = true;
                    latitude = bdLocation.getLatitude();
                    longitude = bdLocation.getLongitude();
                    height = bdLocation.getAltitude();

//                    TODO 定位数据与任务数据比较

                    double distance = DistanceUtil.getDistance(new LatLng(latitude, longitude),
                            new LatLng(cur_Point.latitude,cur_Point.longitude));
                    new AlertDialog.Builder(MissionActivity.this)
                            .setMessage(cur_Point.latitude + "," + cur_Point.longitude
                                    + "\n" + latitude + "," + longitude
                                    +"\n"+distance+"\n"+cur_Point.orientation+"\n"+orientation).show();
                    if (25 < distance) FoundPoint = false;
//                    if (5 < Math.abs(orientation-cur_Point.orientation)) FoundPoint = false;
                    if (FoundPoint){
                        p_confirm.setTextColor(Color.RED);
                        cur_Point.state = "已完成";
                        dmr.updatePointState(cur_Point);
                        cur_PointList.set(cur_Point.orderNum-1,cur_Point);
                        answers_done.set(cur_Point.orderNum-1,true);
                        if (answers_done.get(cur_Point.orderNum-1)){
                            answer.setText(cur_Point.answer);
                            displayImg(cur_Point);
                        }
                        if (cur_Point.orderNum == cur_PointList.size()){
                            cur_Mission.state = "已完成";
                            cur_Mission.startTime = "";
                            dmr.updateMissionState(cur_Mission);
                            Toast.makeText(MissionActivity.this,"恭喜！任务点全部完成！",Toast.LENGTH_SHORT).show();
                        }else {Toast.makeText(MissionActivity.this,"找到了！快去找下一个点吧！~",Toast.LENGTH_SHORT).show();}
                    }else{
                        Toast.makeText(MissionActivity.this,"不是这儿~",Toast.LENGTH_SHORT).show();
                    }
                    positioning = false;//判断定位结束

                }else {
                    p_confirm.setTextColor(Color.BLACK);
                    Toast.makeText(MissionActivity.this,"定位失败，请重新定位",Toast.LENGTH_SHORT).show();
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
    //将当前点数据赋给View
    private void setCurP2View(MsPoint msp){
        question.setText(msp.question);
        answer.setText(msp.answer);
        p_confirm.setText(String.format("%s%s", msp.orderNum, mission_cnt_str));
        p_confirm.setTextColor(Color.RED);
        displayImg(msp);
    }
    public void confirm_answer_click(View view) {
        if (answer.getText().toString().equals(cur_Point.answer)){
            answers_done.set(cur_Point.orderNum-1,true);
            displayImg(cur_Point);
            Toast.makeText(this,"答案正确",Toast.LENGTH_SHORT).show();
        }else
            Toast.makeText(this,"答案错误",Toast.LENGTH_SHORT).show();
    }

    public void point_before_m_click(View view) {
        if (!positioning){
            if (cur_Point.orderNum-1 != 0){
                cur_Point = cur_PointList.get(cur_Point.orderNum-2);
                setCurP2View(cur_Point);
            }
        }
    }

    public void position_confirm_click(View view) {
        if (!positioning){
            if ("未完成".equals(cur_Point.state)){
                positioning = true;
                confirm_position();
            }
        }
    }

    public void point_next_m_click(View view) {
        if (!positioning){
            if ("已完成".equals(cur_Point.state)){
                if (cur_Point.orderNum+1 > cur_PointList.size()){
                    Toast.makeText(this,"恭喜！任务点全部完成！",Toast.LENGTH_SHORT).show();
                }else {
                    cur_Point = cur_PointList.get(cur_Point.orderNum);
                    if ("已完成".equals(cur_Point.state))
                        setCurP2View(cur_Point);
                    else if(answers_done.get(cur_Point.orderNum-1)){
                        question.setText(cur_Point.question);
                        answer.setText("");
                        p_confirm.setText(String.format("%s%s", cur_Point.orderNum, mission_cnt_str));
                        p_confirm.setTextColor(Color.BLACK);
                        displayImg(cur_Point);
                    } else {
                        question.setText(cur_Point.question);
                        answer.setText("");
                        p_confirm.setText(String.format("%s%s", cur_Point.orderNum, mission_cnt_str));
                        p_confirm.setTextColor(Color.BLACK);
                        //设置为空图片
                        imageView.setImageBitmap(null);
                    }
                }
            }else
                Toast.makeText(this,"找到当前任务点才能进行下一个",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mission, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.give_up) {
            new AlertDialog.Builder(MissionActivity.this)
                    .setTitle("放弃任务")
                    .setMessage("确定放弃该任务吗？")
                    .setNegativeButton("取消",null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cur_Mission.state = "未完成";
                            cur_Mission.startTime = "";
                            dmr.updateMissionState(cur_Mission);
                            MissionActivity.this.finish();
                        }
                    }).show();
            return true;
        }
        else if (id == R.id.complete) {
            if ("已完成".equals(cur_Mission.state)){
                new AlertDialog.Builder(MissionActivity.this)
                        .setTitle("任务完成")
                        .setMessage("恭喜完成任务！用时xx")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MissionActivity.this.finish();
                            }
                        }).show();
                return true;
            }else{
                Toast.makeText(this,"任务还未完成，请再接再励！",Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onStart() {
        Log.i(TAG, "MissionActivity onStart");
        super.onStart();
    }
    @Override
    protected void onRestart(){
        Log.i(TAG, "MissionActivity onRestart");
        super.onRestart();
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "MissionActivity onResume");
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.i(TAG, "MissionActivity onPause");
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.i(TAG, "MissionActivity onStop");
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        Log.i(TAG, "MissionActivity onDestroy");
        dmr.closeDB();
        super.onDestroy();
    }
}
