package com.exc.zhen.orienteering;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import java.util.ArrayList;
import java.util.List;

public class MissionActivity extends Activity {
    private final static String TAG = "MyDebug";
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
    private TencentLocationManager locationManager;
    private double latitude,longitude,height;
    private double orientation;
    private boolean positioning = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        locationManager = TencentLocationManager.getInstance(this);
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

        p_confirm.setText(Integer.toString(cur_Point.order_num)+mission_cnt_str);

    }

    //TODO 确认位置等手机参数正确
    private void confirm_position(){
        TencentLocationRequest request = TencentLocationRequest.create();
        request.setInterval(10);//设置定位周期（位置监听器回调周期），单位为ms（毫秒）
        request.setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_GEO);//设置定位的request level
        request.setAllowDirection(true);//设置允许使用设备陀螺仪
        request.setAllowCache(true);//设置是否允许使用缓存，连续多次定位时建议允许缓存
        int error = locationManager.requestLocationUpdates(request, new TencentLocationListener() {
            @Override
            public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
                if (tencentLocation.ERROR_OK == i){
                    boolean FoundPoint = true;
                    latitude = tencentLocation.getLatitude();
                    longitude = tencentLocation.getLongitude();
                    height = tencentLocation.getAltitude();
                    String key = TencentLocation.EXTRA_DIRECTION;
                    orientation = tencentLocation.getExtra().getDouble(key);
//                    TODO 定位数据与任务数据比较
                    new AlertDialog.Builder(MissionActivity.this)
                            .setMessage(cur_Point.latitude + "," + cur_Point.longitude
                                    + "\n" + latitude + "," + longitude
                            +"\n"+cur_Point.orientation+"\n"+orientation).show();
                    if (0.003 < Math.abs(latitude-cur_Point.latitude)) FoundPoint = false;
                    if (0.003 < Math.abs(longitude-cur_Point.longitude)) FoundPoint = false;
                    if (2 < Math.abs(orientation-cur_Point.orientation)) FoundPoint = false;
                    if (FoundPoint){
                        p_confirm.setTextColor(Color.RED);
                        cur_Point.state = "已完成";
                        dmr.updatePointState(cur_Point);
                        cur_PointList.set(cur_Point.order_num-1,cur_Point);
                        answers_done.set(cur_Point.order_num-1,true);
                        if (answers_done.get(cur_Point.order_num-1)){
                            answer.setText(cur_Point.answer);
//TODO                    imageView.setImageBitmap();
                        }
                        if (cur_Point.order_num == cur_PointList.size()){
                            cur_Mission.state = "已完成";
                            cur_Mission.start_time = "";
                            dmr.updateMissionState(cur_Mission);
                            Toast.makeText(MissionActivity.this,"恭喜！任务点全部完成！",Toast.LENGTH_SHORT).show();
                        }else {Toast.makeText(MissionActivity.this,"找到了！快去找下一个点吧！~",Toast.LENGTH_SHORT).show();}
                    }else{
                        Toast.makeText(MissionActivity.this,"不是这儿~",Toast.LENGTH_SHORT).show();
                    }
                    positioning = false;//判断定位结束
                }else{
                    p_confirm.setTextColor(Color.BLACK);
                    Toast.makeText(MissionActivity.this,s+"导致定位失败，重新定位",Toast.LENGTH_SHORT).show();
                    positioning = false;
                }
                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusUpdate(String s, int i, String s1) {

            }
        });//注册位置监听器
        if (error == 0) {
            Toast.makeText(MissionActivity.this,"正在定位，请稍后！",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MissionActivity.this,"定位失败 "+error,Toast.LENGTH_SHORT).show();
        }
    }
    //将当前点数据赋给View
    private void setCurP2View(MsPoint msp){
        question.setText(msp.question);
        answer.setText(msp.answer);
        p_confirm.setText(Integer.toString(msp.order_num)+mission_cnt_str);
        p_confirm.setTextColor(Color.RED);
//TODO        imageView.setImageBitmap();
    }
    public void confirm_answer_click(View view) {
        if (answer.getText().toString().equals(cur_Point.answer)){
            answers_done.set(cur_Point.order_num-1,true);
//TODO            imageView.setImageBitmap();
        }else
            Toast.makeText(this,"答案错误",Toast.LENGTH_SHORT).show();
    }

    public void point_before_m_click(View view) {
        if (!positioning){
            if (cur_Point.order_num-1 != 0){
                cur_Point = cur_PointList.get(cur_Point.order_num-2);
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
                if (cur_Point.order_num+1 > cur_PointList.size()){
                    Toast.makeText(this,"恭喜！任务点全部完成！",Toast.LENGTH_SHORT).show();
                }else {
                    cur_Point = cur_PointList.get(cur_Point.order_num);
                    if ("已完成".equals(cur_Point.state))
                        setCurP2View(cur_Point);
                    else if(answers_done.get(cur_Point.order_num-1)){
                        question.setText(cur_Point.question);
                        answer.setText("");
                        p_confirm.setText(Integer.toString(cur_Point.order_num)+mission_cnt_str);
                        p_confirm.setTextColor(Color.BLACK);
//TODO                  imageView.setImageBitmap();
                    }else {
                        question.setText(cur_Point.question);
                        answer.setText("");
                        p_confirm.setText(Integer.toString(cur_Point.order_num)+mission_cnt_str);
                        p_confirm.setTextColor(Color.BLACK);
                        //设置为空图片
//TODO                  imageView.setImageBitmap();
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
                            cur_Mission.start_time = "";
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
