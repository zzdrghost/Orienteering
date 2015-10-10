package com.exc.zhen.orienteering;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;

import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import org.json.*;

public class MainActivity extends Activity {
    public final static String INTENT_EXTRA = "MISSION ID";
    private final static String TAG = "MyDebug";
    private final static String PREFS_NAME = "Settings";
    private final static String FIRST_RUN = "first_run";
    private static boolean IS_FIRST_RUN;
    private final static int myIndicatorColor = Color.parseColor("#D1EEEE");
    private final static int myDividerColor = Color.parseColor("#DEDEDE");
    private final static String[] tabTitles = {"地图","任务"};
    private final static String URL="http://drghostserver.sinaapp.com:80";
    //private final static String URL="http://202.114.118.90:8080";
    private final static String savePath = Environment.getExternalStorageDirectory().
            getPath()+"/orienteeringImg/";
    private final static String JSON_FORM = "application/json";
    private List<Integer> postMissionId;

    private final static String FAILED_ICON = "failed_point.png";
    private final static String COMPLETED_ICON = "complete_point.png";
    private final static String IN_ICON = "in_point.png";
    private BitmapDescriptor failedBit;
    private BitmapDescriptor completeBit;
    private BitmapDescriptor inBit;
    //TODO 优化数据结构
    private View view1,view2;
    List<View> viewList;
    private ViewPagerCompat pager ;
    SlidingTabLayout tabLayout;
    MyPageAdapter myPageAdapter;

    private MapView mapView;
    private BaiduMap baiduMap;
    private LocationClient locationClient;
    private Overlay marker;
    private LatLng cur_position = null;
    private MyBaseAdapter myBaseAdapter;
    private ListView mListView;

    public DbManager dmr;
    private List<Mission> missionList=null;
    private List<MsPoint> cmsPointList=null;
    private Mission currentMission=null;
    private MsPoint cur_point = null;

    private final static int UPDATE_DONE = 1;
    private final static int SYNCHRONIZE_DONE = 2;
    private final static int SYNCHRONIZE_FAILED = 3;
    private final static int SYNCHRONIZE_LATEST = 4;
    
    private class  MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case UPDATE_DONE:{
                    myBaseAdapter.notifyDataSetChanged();
                    baiduMap.clear();
                    Toast.makeText(MainActivity.this,"任务进行中",Toast.LENGTH_SHORT).show();
                    addMarkers();
                    break;
                }
                case SYNCHRONIZE_DONE:{
                    updateMainData();
                    Toast.makeText(MainActivity.this,"同步完成",Toast.LENGTH_SHORT).show();
                    break;
                }
                case SYNCHRONIZE_FAILED:{
                    Toast.makeText(MainActivity.this,"同步失败",Toast.LENGTH_SHORT).show();
                    break;
                }
                case SYNCHRONIZE_LATEST:{
                    Toast.makeText(MainActivity.this,"已是最新数据",Toast.LENGTH_SHORT).show();
                    break;
                }
                default:{break;}
            }
        }
    }
    MyHandler handler = new MyHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        Log.i(TAG, "MainActivity onCreate");
        setContentView(R.layout.activity_main);
        //初始化ViewPager
        initViewPager();
        //初始化DbManager
        dmr = new DbManager(this);
        //获取SharedPreferences对象判断程序第一次安装
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        IS_FIRST_RUN = settings.getBoolean(FIRST_RUN,true);
        //初始化数据
        initDbData();
        //初始化其他View
        initOtherView();
        //添加当前任务标注
        addMarkers();
    }
    //初始化数据
    private void initDbData(){
        failedBit = BitmapDescriptorFactory.fromAsset(FAILED_ICON);
        completeBit = BitmapDescriptorFactory.fromAsset(COMPLETED_ICON);
        inBit = BitmapDescriptorFactory.fromAsset(IN_ICON);
        if (IS_FIRST_RUN){
            dmr.clearData();
            File imgDir = new File(savePath);
            imgDir.mkdirs();
            //synchronize();
        }
    }
    //TODO 优化标签布局
    private void initViewPager(){

        LayoutInflater lf = LayoutInflater.from(this);
        view1 = lf.inflate(R.layout.map_layout,(ViewGroup) findViewById(R.id.layout1));
        view2 = lf.inflate(R.layout.mission_layout, (ViewGroup) findViewById(R.id.layout2));

        viewList = new ArrayList<>();
        viewList.add(view1);
        viewList.add(view2);

        //设置ViewPager和SlidingTabLayout布局文件
        pager =(ViewPagerCompat)findViewById(R.id.pager);
        tabLayout=(SlidingTabLayout)findViewById(R.id.tab);

        //建立自定义PagerAdapter对象
        myPageAdapter = new MyPageAdapter(viewList);
        //为ViewPager绑定适配器
        pager.setAdapter(myPageAdapter);
        //设置自定义TabView布局和选中下划线颜色
        tabLayout.setCustomTabView(R.layout.custom_tab, R.id.tab_texView);
        tabLayout.setHorizontalScrollBarEnabled(true);
        tabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return myIndicatorColor;
            }

            @Override
            public int getDividerColor(int position) {
                return myDividerColor;
            }
        });

        //为SlidingTabLayout绑定ViewPager
        tabLayout.setViewPager(pager);

    }
    //TODO 优化初始化结构
    private void initOtherView(){
        //获取mapView
        mapView = (MapView) view1.findViewById(R.id.mapview);
        baiduMap = mapView.getMap();
        //获取view2中的控件
        mListView = (ListView) view2.findViewById(R.id.mListView);
        //更新数据
        missionList = sortMissions(dmr.queryMission());
        currentMission = dmr.getCurrentMission();
        myBaseAdapter = new MyBaseAdapter(getApplicationContext());
        mListView.setAdapter(myBaseAdapter);
        setListener();
    }
    //获取用户当前位置
    private void getUserPosition() {
        locationClient = new LocationClient(this);
        locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null)
                    return;
                cur_position = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(cur_position,19);
                baiduMap.setMapStatus(mapStatusUpdate);
                Toast.makeText(MainActivity.this, "定位完成", Toast.LENGTH_SHORT).show();
                locationClient.unRegisterLocationListener(this);
                locationClient.stop();
            }
        });
        LocationClientOption locateOption = new LocationClientOption();
        locateOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        locateOption.setCoorType("bd09ll");
        locateOption.setScanSpan(10);
        locationClient.setLocOption(locateOption);
        locationClient.start();
        locationClient.requestLocation();
    }

    //添加地图标注
    private void addMarkers(){
        if (null != cmsPointList){
            LatLngBounds.Builder llbb = new LatLngBounds.Builder();
            for (MsPoint msp: cmsPointList) {
                LatLng point = new LatLng(msp.latitude,msp.longitude);
                llbb.include(point);
                switch (msp.state){
                    case "已完成":{
                        marker = baiduMap.addOverlay(new MarkerOptions()
                                .title("已完成")
                                .position(point)
                                .anchor(0.5f, 1f)
                                .icon(completeBit)
                                .draggable(false));
                        break;
                    }
                    case "未完成":{
                        marker = baiduMap.addOverlay(new MarkerOptions()
                                .title("未完成")
                                .position(point)
                                .anchor(0.5f, 1f)
                                .icon(failedBit)
                                .draggable(false));
                        break;
                    }default:{break;}
                }
                if (null != cur_point && msp.orderNum == cur_point.orderNum){
                    marker.remove();
                    marker = baiduMap.addOverlay(new MarkerOptions()
                            .title("进行中")
                            .position(point)
                            .anchor(0.5f, 1f)
                            .icon(inBit)
                            .draggable(false));
                }
            }
            LatLngBounds llb = llbb.build();
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(llb);
            baiduMap.setMapStatus(mapStatusUpdate);
        }
    }
    //排序查询得到的任务列表
    private List<Mission> sortMissions(List<Mission> lms){
        List<Mission> lms1 = new ArrayList<>();//进行中任务
        List<Mission> lms2 = new ArrayList<>();//创建中任务
        List<Mission> lms3 = new ArrayList<>();//未完成任务
        List<Mission> lms4 = new ArrayList<>();//已完成任务
        if (null != lms){
            for ( Mission m: lms) {
                switch (m.state){
                    case "进行中": {lms1.add(m);break;}
                    case "创建中": {lms2.add(m);break;}
                    case "未完成": {lms3.add(m);break;}
                    case "已完成": {lms4.add(m);break;}
                    default:{break;}
                }
            }
            lms1.addAll(lms2);
            lms1.addAll(lms3);
            lms1.addAll(lms4);
        }
        return lms1;
    }
    //重写PagerAdapter
    private class MyPageAdapter extends PagerAdapter {

        private List<View> mListViews;

        MyPageAdapter(List<View> mListViews) {
            this.mListViews = mListViews;
        }
        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mListViews.get(position), 0);

            return mListViews.get(position);
        }
        //设置PagerView切换时的动作
        @Override
        public void startUpdate(ViewGroup container) {
            //重新调用onPrepareOptionsMenu
            invalidateOptionsMenu();
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mListViews.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }
    }

    //根据数据库变化更新主视图
    public void updateMainData() {
        new Thread(){
            public void run(){
                missionList = sortMissions(dmr.queryMission());
                currentMission = dmr.getCurrentMission();
                if (null != currentMission){
                    cur_point = dmr.getCurrentPoint(currentMission.missionId);
                    cmsPointList = dmr.queryMsPoint(currentMission.missionId);
                    handler.sendEmptyMessage(UPDATE_DONE);
                }else {
                    cur_point = null;
                    cmsPointList = null;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            myBaseAdapter.notifyDataSetChanged();
                            baiduMap.clear();
                            addMarkers();
                            getUserPosition();
                        }
                    });
                }
            }
        }.start();
    }

    //ListView 中某项任务被选中后的逻辑
    private void setListener(){
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.i(TAG, missionList.get(position).name);
//                String toast_str ="点击了：";
//                toast_str=toast_str+missionList.get(position).name;
//                Toast.makeText(MainActivity.this,toast_str,Toast.LENGTH_SHORT).show();
                switch (missionList.get(position).state){
                    case "进行中":{
                        Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                        intent.putExtra(INTENT_EXTRA,missionList.get(position).missionId);
                        startActivity(intent);
                        break;
                    }
                    case "创建中":{
                        Intent intent = new Intent(MainActivity.this,CreateActivity.class);
                        intent.putExtra(INTENT_EXTRA,missionList.get(position).missionId);
                        startActivity(intent);
                        break;
                    }
                    case "未完成":{
                        if (null!=currentMission){
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("放弃任务("+currentMission.name+")开始新任务("+
                                            missionList.get(position).name+")?")
                                    .setNegativeButton("取消",null)
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            currentMission.state = "未完成";
                                            currentMission.startTime = "";
                                            dmr.updateMissionState(currentMission);
                                            missionList.get(position).state = "进行中";
                                            missionList.get(position).startTime = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).missionId);
                                            startActivity(intent);
                                        }
                                    }).show();
                        }else {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("开始新任务("+missionList.get(position).name+")?")
                                    .setNegativeButton("取消",null)
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            missionList.get(position).state = "进行中";
                                            missionList.get(position).startTime = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).missionId);
                                            startActivity(intent);
                                        }
                                    }).show();
                        }
                        break;
                    }
                    case "已完成":{
                        if (null!=currentMission){
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("放弃任务("+currentMission.name+")重新开始任务("+
                                            missionList.get(position).name+")?")
                                    .setNegativeButton("取消",null)
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            currentMission.state = "未完成";
                                            currentMission.startTime = "";
                                            dmr.updateMissionState(currentMission);
                                            missionList.get(position).state = "进行中";
                                            missionList.get(position).startTime = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).missionId);
                                            startActivity(intent);
                                        }
                                    }).show();
                        }else {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("重新开始任务("+missionList.get(position).name+")?")
                                    .setNegativeButton("取消",null)
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            missionList.get(position).state = "进行中";
                                            missionList.get(position).startTime = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).missionId);
                                            startActivity(intent);
                                        }
                                    }).show();
                        }
                        break;
                    }
                    default:{break;}
                }
            }
        });
    }

    //重写BaseAdapter
    private class MyBaseAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater;

        public MyBaseAdapter(Context ctx){
            layoutInflater = LayoutInflater.from(ctx);
        }
        @Override
        public int getCount() {
            return missionList.size();
        }

        @Override
        public Object getItem(int position) {
            return missionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LiViewHolder holder;
            if(convertView == null){
                holder = new LiViewHolder();
                convertView = layoutInflater.inflate(R.layout.list_item_layout,null);
                holder.tv_mission_title = (TextView) convertView.findViewById(R.id.tv_mission_title);
                holder.tv_mission_state = (TextView) convertView.findViewById(R.id.tv_mission_state);
                holder.tv_last_time = (TextView) convertView.findViewById(R.id.tv_last_time);
                convertView.setTag(holder);
            }else{
                holder = (LiViewHolder) convertView.getTag();
            }
            //Log.i("ListItem", Integer.toString(position));
            //Log.i("ListItem", mData.get(position).get("tv_mission_title"));
            //设置当前itemView的内容
            holder.tv_mission_title.setText(missionList.get(position).name);
            holder.tv_mission_state.setText(missionList.get(position).state);
            holder.tv_last_time.setText(missionList.get(position).limitTime);
            //设置当前convertView的布局
            switch (missionList.get(position).state){
                case "已完成":{
                    convertView.setBackgroundResource(R.drawable.ms_comlete_bg);
                    break;
                }
                case "进行中":{
                    convertView.setBackgroundResource(R.drawable.ms_in_bg);
                    break;
                }
                case "未完成":{
                    convertView.setBackgroundResource(R.drawable.ms_failed_bg);
                    break;
                }
                case "创建中":{
                    convertView.setBackgroundResource(R.drawable.ms_create_bg);
                    break;
                }default:{break;}
            }
            return convertView;
        }
    }
    //任务列表views临时存放
    private class LiViewHolder {
        public TextView tv_mission_title;
        public TextView tv_mission_state;
        public TextView tv_last_time;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        menu.clear();

        //设置menu样式
        MenuInflater menuInflater = this.getMenuInflater();
        switch (pager.getCurrentItem()){
            case 1:{
                menuInflater.inflate(R.menu.menu_main,menu);
                break;
            }default:{break;}
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_synchronize) {
            new AlertDialog.Builder(this)
                    .setMessage("同步在线任务？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            synchronize();
                        }
                    }).show();
            Log.i(TAG,"MainActivity 同步任务");
            return true;
        } else if(id == R.id.action_new_mission){
            Log.i(TAG, "创建新任务");
            new AlertDialog.Builder(this)
                    .setTitle("新建任务")
                    .setMessage("是否创建新任务?")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(MainActivity.this,CreateActivity.class);
                            startActivity(intent);
                        }
                    }).show();
            return true;
        }
        else if (id == R.id.action_about){
            Log.i(TAG,"MainActivity 关于");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int confirmData(){
        Log.i(TAG,"MainActivity confirmData");
        List<Integer> client_missionId_list = new ArrayList<>();
        if (null!=missionList){
            for (Mission ms: missionList) {
                client_missionId_list.add(ms.getMissionId());
            }
        }
        JSONArray ja = new JSONArray(client_missionId_list);
        String resp = null;
        try {
            resp = MyHttpRequest.postJson(URL+"/confirmdata",ja.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null==resp){
            return -1;
        }
        Log.i(TAG, resp);
        try {
            JSONObject rjo = new JSONObject(resp);
            JSONArray uploadmsid_ja = rjo.getJSONArray("uploadlist");
            JSONArray downloadmsid_ja = rjo.getJSONArray("downloadlist");
            postMissionId = new ArrayList<>();
            for (int i = 0; i < uploadmsid_ja.length(); i++) {
                postMissionId.add((Integer)uploadmsid_ja.get(i));
            }
            for (int i = 0; i < downloadmsid_ja.length(); i++) {
                postMissionId.add((Integer)downloadmsid_ja.get(i));
            }
            if (0 != uploadmsid_ja.length()) {
                return 1;
            }
            if (0 != downloadmsid_ja.length()) {
                return 2;
            }
        }catch (JSONException je){
            je.printStackTrace();
            return -1;
        }
        return 0;
    }

    private int postData(){
        Log.i(TAG,"MainActivity postData");
        //注意将state设置为未完成
        List<Mission> listMS = new ArrayList<>();
        List<MsPoint> listMSP = new ArrayList<>();
        List<String> files = new ArrayList<>();

        for (int msid: postMissionId) {
            Mission mstemp = dmr.getMission(msid);
            mstemp.setState("未完成");
            listMS.add(mstemp);
            List<MsPoint> mspltemp = dmr.queryMsPoint(msid);
            if (null != mspltemp){
                for(MsPoint msp : mspltemp){
                    msp.setState("未完成");
                    listMSP.add(msp);
                    files.add(msp.getImgAddress());
                }
            }
        }
        JSONObject jo = new JSONObject();
        try {
            jo.put("mission", JsonTools.lmsToJa(listMS));
            jo.put("mspoint", JsonTools.lmspToJa(listMSP));
        }catch (JSONException je){
            je.printStackTrace();
            return -1;
        }
        int postJson_status = postJson(jo);
        if (-1==postJson_status)
            return -1;
        int postImage_status = postImage(files);
        if (-1==postImage_status)
            return -1;
        return 1;
    }
    private int postJson(JSONObject jo){
        Log.i(TAG,"MainActivity postJson");
        //Log.i(TAG,jo.toString());
        String resp = null;
        try {
            resp = MyHttpRequest.postJson(URL + "/postjson", jo.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        if (null==resp){
            Log.i(TAG, "MainActivity post json failed.");
            return -1;
        }
        Log.i(TAG, resp);
        return 1;
    }
    private int postImage(List<String> filenames){
        Log.i(TAG,"MainActivity postImage");
        String resp = null;
        try {
            resp = MyHttpRequest.postFiles(URL + "/postimage", filenames);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        Log.i(TAG, resp);
        return 1;
    }
    private int getData(){
        Log.i(TAG,"MainActivity getData");
        JSONArray ja = new JSONArray(postMissionId);
        String resp = null;
        try {
            resp = MyHttpRequest.postJson(URL+"/downloadjson",ja.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        if (null==resp){
            Log.i(TAG,"MainActivitypost json failed.");
            return -1;
        }
        //Log.i(TAG, resp);
        try {
            JSONObject rjo= new JSONObject(resp);
            JSONArray msja=rjo.getJSONArray("mission");
            Log.i(TAG, String.valueOf(msja.length()));
            JSONArray mspja=rjo.getJSONArray("mspoint");
            Log.i(TAG, String.valueOf(mspja.length()));
            for(int i=0;i<msja.length();i++){
                Mission mstemp=JsonTools.joToMs(msja.getJSONObject(i));
                if (null != mstemp){
                    dmr.addMission(mstemp);
                    Log.i(TAG, mstemp.name);
                }else {
                    Log.i(TAG, "null");
                }

            }
            List<MsPoint> listMSP = new ArrayList<>();
            List<String> files = new ArrayList<>();
            for (int i = 0; i < mspja.length(); i++) {
                MsPoint msptemp=JsonTools.joToMsp(mspja.getJSONObject(i));
                if (null != msptemp){
                    Log.i(TAG, msptemp.orderNum + "\t" + msptemp.missionId);
                    listMSP.add(msptemp);
                    files.add(msptemp.imgAddress);
                }
            }
            dmr.addPoint(listMSP);
            int failedTimes=0;
            for (String filename : files) {
                try {
                    if(-1==MyHttpRequest.getFile(URL + "/downloadimage/", filename))
                        failedTimes++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG,"MainActivity"+ failedTimes + "个图片下载失败");
        }catch (JSONException je){
            je.printStackTrace();
            return -1;
        }
        return 1;
    }
    //判断当前网络是否可用
    public boolean isNetworkAvailable(Activity activity)
    {
        Context context = activity.getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null)
        {
            return false;
        }
        else
        {
            // 获取NetworkInfo对象
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null )
            {
                return networkInfo.isConnected();
            }
        }
        return false;
    }
    private void synchronize(){
        if (!isNetworkAvailable(MainActivity.this)){
            Toast.makeText(MainActivity.this,"同步任务失败，当前网络状况不好",Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(){
            public void run(){
                Log.i(TAG,"MainActivity getData");
                int confirm_type = confirmData();
                if (1==confirm_type) {
                    Log.i(TAG,"MainActivity upload data:" + postMissionId);
                    int post_data_status=postData();
                    if (1==post_data_status)
                        handler.sendEmptyMessage(SYNCHRONIZE_DONE);
                    else if (-1==post_data_status)
                        handler.sendEmptyMessage(SYNCHRONIZE_FAILED);
                }
                else if (2==confirm_type) {
                    Log.i(TAG,"MainActivity download data:" + postMissionId);
                    int get_data_status=getData();
                    if (1==get_data_status)
                        handler.sendEmptyMessage(SYNCHRONIZE_DONE);
                    else if (-1==get_data_status)
                        handler.sendEmptyMessage(SYNCHRONIZE_FAILED);
                }
                else if (-1==confirm_type){
                    handler.sendEmptyMessage(SYNCHRONIZE_FAILED);
                }
                else {
                    handler.sendEmptyMessage(SYNCHRONIZE_LATEST);
                }
            }
        }.start();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "MainActivity onStart");
        super.onStart();
    }
    @Override
    protected void onRestart(){
        Log.i(TAG, "MainActivity onRestart");
        super.onRestart();
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "MainActivity onResume");
        updateMainData();
        mapView.onResume();
        super.onResume();
    }
    @Override
    protected void onPause() {
        Log.i(TAG, "MainActivity onPause");
        mapView.onPause();
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.i(TAG, "MainActivity onStop");
        super.onStop();
        //创建Settings.xml中的first_run
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (IS_FIRST_RUN)
            editor.putBoolean(FIRST_RUN,false);
        editor.apply();
    }
    @Override
    protected void onDestroy() {
        Log.i(TAG, "MainActivity onDestroy");
        mapView.onDestroy();
        super.onDestroy();
        inBit.recycle();
        failedBit.recycle();
        completeBit.recycle();
        dmr.closeDB();
    }
}
