package com.exc.zhen.orienteering;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.LatLngBounds;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    public final static String INTENT_EXTRA = "MISSION ID";
    private final static String TAG = "MyDebug";
    private final static String PREFS_NAME = "Settings";
    private final static String FIRST_RUN = "first_run";
    private static boolean IS_FIRST_RUN;
    private final static int myIndicatorColor = Color.parseColor("#D1EEEE");
    private final static int myDividerColor = Color.parseColor("#DEDEDE");
    private final static String[] tabTitles = {"地图","任务"};
    //TODO 优化数据结构
    private View view1,view2;
    private List<View> viewList;
    private ViewPager pager ;
    private SlidingTabLayout tabLayout;
    private MyPageAdapter myPageAdapter;

    private MapView mapView;
    private TencentMap tencentMap;
    private TencentLocationManager locationManager;
    private Marker marker;
    private LatLng cur_position = null;
    private MyBaseAdapter myBaseAdapter;
    private ListView mListView;

    public DbManager dmr;
    private List<Mission> missionList;
    private List<MsPoint> cmsPointList;
    private Mission currentMission;
    private MsPoint cur_point = null;

    private final static int UPDATE_DONE = 1;
    private final static int LOCATE_DONE = 2;


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case UPDATE_DONE:{
                    myBaseAdapter.notifyDataSetChanged();
                    tencentMap.clearAllOverlays();
                    Toast.makeText(MainActivity.this,"任务进行中",Toast.LENGTH_SHORT).show();
                    addMarkers();
                    break;
                }
                default:{break;}
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity onCreate");
        setContentView(R.layout.activity_main);
        //初始化ViewPager
        initViewPager();
        //初始化DbManager
        dmr = new DbManager(this);
        //获取SharedPreferences对象判断程序第一次安装
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        IS_FIRST_RUN = settings.getBoolean(FIRST_RUN,true);
        //加入测试数据
        initDbData();
        //初始化其他View
        initOtherView();
        //添加当前任务标注
        addMarkers();
        //保存地图状态
        mapView.onCreate(savedInstanceState);
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
        pager =(ViewPager)findViewById(R.id.pager);
        tabLayout=(SlidingTabLayout)findViewById(R.id.tab);

        //建立自定义PagerAdapter对象
        myPageAdapter = new MyPageAdapter(viewList);
        //为ViewPager绑定适配器
        pager.setAdapter(myPageAdapter);
        //设置自定义TabView布局和选中下划线颜色
        tabLayout.setCustomTabView(R.layout.custom_tab, R.id.tab_texView);
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
        //获取mapview
        mapView = (MapView) view1.findViewById(R.id.mapview);
        tencentMap = mapView.getMap();
        locationManager = TencentLocationManager.getInstance(MainActivity.this);
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
        TencentLocationRequest request = TencentLocationRequest.create();
        request.setAllowDirection(true);
        request.setAllowCache(true);
        request.setInterval(10);
        request.setRequestLevel(0);
        int error = locationManager.requestLocationUpdates(request, new TencentLocationListener() {
            @Override
            public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
                if (tencentLocation.ERROR_OK == 0) {
                    cur_position = new LatLng(tencentLocation.getLatitude(),
                            tencentLocation.getLongitude());
                    tencentMap.animateTo(cur_position);
                    tencentMap.setZoom(12);
                    Toast.makeText(MainActivity.this,"定位完成",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this,"定位失败 "+s,Toast.LENGTH_SHORT).show();
                }
                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusUpdate(String s, int i, String s1) {

            }
        });
        if (0 == error) {
            Toast.makeText(MainActivity.this,"正在定位",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(MainActivity.this,"定位失败",Toast.LENGTH_SHORT).show();
        }
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
                        marker = tencentMap.addMarker(new MarkerOptions()
                                .title("已完成")
                                .position(point)
                                .anchor(0.5f, 1f)
                                .icon(BitmapDescriptorFactory.fromAsset("complete_point.png"))
                                .draggable(false));
                        break;
                    }
                    case "未完成":{
                        marker = tencentMap.addMarker(new MarkerOptions()
                                .title("未完成")
                                .position(point)
                                .anchor(0.5f, 1f)
                                .icon(BitmapDescriptorFactory.fromAsset("failed_point.png"))
                                .draggable(false));
                        break;
                    }default:{break;}
                }
                if (null != cur_point && msp.order_num == cur_point.order_num){
                    marker.remove();
                    marker = tencentMap.addMarker(new MarkerOptions()
                            .title("进行中")
                            .position(point)
                            .anchor(0.5f, 1f)
                            .icon(BitmapDescriptorFactory.fromAsset("in_point.png"))
                            .draggable(false));
                }
            }
            LatLngBounds llb = llbb.build();
            tencentMap.zoomToSpan(llb.getSouthwest(), llb.getNortheast());
//            if (null != cur_point){
//                tencentMap.animateTo(new LatLng(cur_point.latitude,cur_point.longitude));
//            }
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
    //测试数据
    private void initDbData(){
        if (IS_FIRST_RUN){
            dmr.clearData();
//            Mission ms = new Mission(1,"任务一","未完成","4小时","");
//            dmr.addMission(ms);
//            ms = new Mission(2,"任务二","已完成","4小时","");
//            dmr.addMission(ms);
//            ms = new Mission(3,"测试任务","进行中","4小时","10点30");
//            dmr.addMission(ms);
//            ms = new Mission(4,"任务四","创建中","4小时","");
//            dmr.addMission(ms);
//            List<MsPoint> mspl = new ArrayList<>();
//            MsPoint msp = new MsPoint(1,1,"未完成",27.2,130.4,10.5,"好?","好","",30);
//            mspl.add(msp);
//            msp = new MsPoint(1,2,"未完成",30.2,130.4,10.5,"不好?","不好","",30);
//            mspl.add(msp);
//            msp = new MsPoint(2,1,"已完成",47.2,130.4,10.5,"好?","好","",30);
//            mspl.add(msp);
//            msp = new MsPoint(3,1,"已完成",30.545132,114.300299,10.5,"好?","好","",30);
//            mspl.add(msp);
//            msp = new MsPoint(3,2,"未完成",30.547183,114.292724,10.5,"不好?","不好","",30);
//            mspl.add(msp);
//            msp = new MsPoint(3,3,"未完成",30.558048,114.301221,10.5,"不好?","不好","",30);
//            mspl.add(msp);
//            msp = new MsPoint(3,4,"未完成",30.560755,114.303249,10.5,"不好?","不好","",30);
//            mspl.add(msp);
//            msp = new MsPoint(4,1,"未完成",77.2,130.4,10.5,"不好?","不好","",30);
//            mspl.add(msp);
//            dmr.addPoint(mspl);
//            Log.i(TAG,"MainActivity 初始化测试数据");
        }
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
                    cur_point = dmr.getCurrentPoint(currentMission.mission_id);
                    cmsPointList = dmr.queryMsPoint(currentMission.mission_id);
                    handler.sendEmptyMessage(UPDATE_DONE);
                }else {
                    cur_point = null;
                    cmsPointList = null;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            myBaseAdapter.notifyDataSetChanged();
                            tencentMap.clearAllOverlays();
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
                        intent.putExtra(INTENT_EXTRA,missionList.get(position).mission_id);
                        startActivity(intent);
                        break;
                    }
                    case "创建中":{
                        Intent intent = new Intent(MainActivity.this,CreateActivity.class);
                        intent.putExtra(INTENT_EXTRA,missionList.get(position).mission_id);
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
                                            currentMission.start_time = "";
                                            dmr.updateMissionState(currentMission);
                                            missionList.get(position).state = "进行中";
                                            missionList.get(position).start_time = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).mission_id);
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
                                            missionList.get(position).start_time = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).mission_id);
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
                                            currentMission.start_time = "";
                                            dmr.updateMissionState(currentMission);
                                            missionList.get(position).state = "进行中";
                                            missionList.get(position).start_time = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).mission_id);
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
                                            missionList.get(position).start_time = "now";
                                            dmr.updateMissionState(missionList.get(position));
                                            Intent intent = new Intent(MainActivity.this,MissionActivity.class);
                                            intent.putExtra(INTENT_EXTRA, missionList.get(position).mission_id);
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
            holder.tv_last_time.setText(missionList.get(position).limit_time);
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

        //设置mean样式
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
        if (id == R.id.action_settings) {
            Log.i(TAG,"设置");
            return true;
        }
        else if(id == R.id.action_new_mission){
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
            Log.i(TAG,"关于");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "MainActivity onStart");
        super.onStart();
    }
    @Override
    protected void onRestart(){
        Log.i(TAG, "MainActivity onRestart");
        mapView.onRestart();
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
        mapView.onStop();
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
        tencentMap.clearCache();
        mapView.onDestroy();
        super.onDestroy();
        dmr.closeDB();
    }
}
