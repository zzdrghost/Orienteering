package com.exc.zhen.orienteering;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZHEN on 2015/9/7 0007.
 * 数据管理类
 */
public class DbManager {
    private SQLiteDatabase db;
    DbHelper dbhelper;
    private final static String mdbName = "defaultDB";

    DbManager(Context context){
        dbhelper = new DbHelper(context,mdbName);
        db = dbhelper.getWritableDatabase();
    }
    /**添加及更新任务
     * @param ms 待添加的任务*/
    public void addMission(Mission ms){
        Cursor c = db.rawQuery("SELECT * FROM mission WHERE mission_id = ?",
                new String[]{Integer.toString(ms.missionId)});
        c.moveToFirst();
        if (c.isAfterLast()){
            db.execSQL("INSERT INTO mission (name,state,limit_time) " +
                    "VALUES(?,?,?)", new Object[]{ms.name, ms.state, ms.limitTime});
        }else {
            ContentValues cv = new ContentValues();
            cv.put("name", ms.name);
            cv.put("state", ms.state);
            cv.put("limit_time", ms.limitTime);
            db.update("mission", cv, "mission_id=?", new String[]{Integer.toString(ms.missionId)});
        }
        c.close();
        //Log.i("MyDebug", "DbManager 添加任务");
    }
    /**添加任务点
     * @param msPs 待添加的任务点集合*/
    public void addPoint(List<MsPoint> msPs){
        //用事务统一插入数据
        db.beginTransaction();
        try {
            for (MsPoint msP : msPs) {
                Cursor c = db.rawQuery("SELECT * FROM ms_point WHERE mission_id = ? AND order_num = ?",
                        new String[]{Integer.toString(msP.missionId),Integer.toString(msP.orderNum)});
                c.moveToFirst();
                if (c.isAfterLast()){
                    db.execSQL("INSERT INTO ms_point (mission_id,order_num,state,latitude,longitude," +
                                    "height,question,answer,img_address,orientation) VALUES(?,?,?,?,?,?,?,?,?,?)",
                            new Object[]{msP.missionId,msP.orderNum,msP.state,msP.latitude,msP.longitude,
                                    msP.height,msP.question,msP.answer, msP.imgAddress,msP.orientation});
//                    Log.i("MyDebug", "DbManager 添加任务点");
                }else {
                    ContentValues cv = new ContentValues();
                    cv.put("latitude",msP.latitude);
                    cv.put("longitude",msP.longitude);
                    cv.put("height",msP.height);
                    cv.put("question",msP.question);
                    cv.put("answer",msP.answer);
                    cv.put("img_address",msP.imgAddress);
                    cv.put("orientation",msP.orientation);
                    db.update("ms_point", cv, "mission_id=? AND order_num=?", new String[]{
                            Integer.toString(msP.missionId), Integer.toString(msP.orderNum)});
                }
                c.close();
            }
            db.setTransactionSuccessful();
        }catch (Exception e){
            System.out.println("添加点集失败");
        }finally {
            db.endTransaction();
        }
        //Log.i("MyDebug", "DbManager 添加任务点");
    }

    /**更新任务点状态
     * @param msP 待更新的任务点*/
    public void updatePointState(MsPoint msP){
        ContentValues cv = new ContentValues();
        cv.put("state", msP.state);
        db.update("ms_point", cv, "mission_id=? AND order_num=?", new String[]{
                Integer.toString(msP.missionId), Integer.toString(msP.orderNum)});
        Log.i("MyDebug", "DbManager 更新任务点状态");
    }
    /**根据待更新任务的状态更新任务表
     * @param ms 待更新任务*/
    public void updateMissionState(Mission ms){
        ContentValues cv = new ContentValues();
        cv.put("state", ms.state);
        cv.put("start_time", ms.startTime);
        db.update("mission", cv, "mission_id=?", new String[]{Integer.toString(ms.missionId)});
        if("进行中".equals(ms.state)){
            cv = new ContentValues();
            cv.put("state","未完成");
            db.update("ms_point", cv, "mission_id=?", new String[]{Integer.toString(ms.missionId)});
        }
        if("未完成".equals(ms.state)){
            cv = new ContentValues();
            cv.put("state","未完成");
            db.update("ms_point", cv, "mission_id=?", new String[]{Integer.toString(ms.missionId)});
        }
        if("已完成".equals(ms.state)){
            cv = new ContentValues();
            cv.put("state","已完成");
            db.update("ms_point", cv, "mission_id=?", new String[]{Integer.toString(ms.missionId)});
        }
        Log.i("MyDebug", "DbManager 根据待更新任务的状态更新任务表");
    }
    /**查询任务表，返回List<Mission>*/
    public List<Mission> queryMission(){
        ArrayList<Mission> missions = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM mission",null);
        c.moveToFirst();
        while (!c.isAfterLast()){
            Mission mission = new Mission();
            mission.missionId = c.getInt(0);
            mission.name = c.getString(1);
            mission.state = c.getString(2);
            mission.limitTime = c.getString(3);
            mission.startTime = c.getString(4);
            missions.add(mission);
            c.moveToNext();
        }
        c.close();
        Log.i("MyDebug", "DbManager 查询任务表,返回" + Integer.toString(missions.size()) + "个结果");
        if (missions.size() > 0)
            return missions;
        return null;
    }
    /**查询任务表，返回一个mission*/
    public Mission getMission(int msid){
        Cursor c = db.rawQuery("SELECT * FROM mission " +
                "WHERE mission_id='"+Integer.toString(msid)+"'",null);
        if (!c.isAfterLast()){
            c.moveToFirst();
            Mission mission = new Mission();
            mission.missionId = c.getInt(0);
            mission.name = c.getString(1);
            mission.state = c.getString(2);
            mission.limitTime = c.getString(3);
            mission.startTime = c.getString(4);
            c.close();
            return mission;
        }else {
            c.close();
            return null;
        }
    }
    /**查询任务表，返回最后一个mission*/
    public Mission getLastMission(){
        Cursor c = db.rawQuery("SELECT * FROM mission ORDER BY mission_id",null);
        if (!c.isAfterLast()){
            c.moveToLast();
            Mission mission = new Mission();
            mission.missionId = c.getInt(0);
            mission.name = c.getString(1);
            mission.state = c.getString(2);
            mission.limitTime = c.getString(3);
            mission.startTime = c.getString(4);
            c.close();
            return mission;
        }else {
            c.close();
            return null;
        }
    }
    /**查询任务表，返回进行中的任务*/
    public Mission getCurrentMission(){
        Cursor c = db.rawQuery("SELECT * FROM mission WHERE state='进行中'",null);
        if (!c.isAfterLast()){
            c.moveToLast();
            Mission mission = new Mission();
            mission.missionId = c.getInt(0);
            mission.name = c.getString(1);
            mission.state = c.getString(2);
            mission.limitTime = c.getString(3);
            mission.startTime = c.getString(4);
            c.close();
            return mission;
        }else {
            c.close();
            return null;
        }
    }
    /**查询任务点，返回List<MsPoint>
     * @param msID 任务id*/
    public List<MsPoint> queryMsPoint(int msID){
        ArrayList<MsPoint> msPoints = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM ms_point " +
                "WHERE mission_id = "+Integer.toString(msID)+
                " ORDER BY order_num",null);
        c.moveToFirst();
        while (!c.isAfterLast()){
            MsPoint mspoint = new MsPoint();
            mspoint.missionId = c.getInt(0);
            mspoint.orderNum = c.getInt(1);
            mspoint.state = c.getString(2);
            mspoint.latitude = c.getDouble(3);
            mspoint.longitude = c.getDouble(4);
            mspoint.height = c.getDouble(5);
            mspoint.question = c.getString(6);
            mspoint.answer = c.getString(7);
            mspoint.imgAddress = c.getString(8);
            mspoint.orientation = c.getDouble(9);
            msPoints.add(mspoint);
            c.moveToNext();
        }
        c.close();
        Log.i("MyDebug", "DbManager 查询任务点,返回"+Integer.toString(msPoints.size())+"个结果");
        if (msPoints.size() > 0)
            return msPoints;
        return null;
    }

    /**查询任务点，当前进行的point*/
    public MsPoint getCurrentPoint(int msID){
        Cursor c = db.rawQuery("SELECT * FROM ms_point " +
                "WHERE mission_id = "+Integer.toString(msID)+
                " AND state = '未完成'"+
                " ORDER BY order_num",null);
        c.moveToFirst();
        MsPoint mspoint = new MsPoint();
        if (!c.isAfterLast()){
            mspoint.missionId = c.getInt(0);
            mspoint.orderNum = c.getInt(1);
            mspoint.state = c.getString(2);
            mspoint.latitude = c.getDouble(3);
            mspoint.longitude = c.getDouble(4);
            mspoint.height = c.getDouble(5);
            mspoint.question = c.getString(6);
            mspoint.answer = c.getString(7);
            mspoint.imgAddress = c.getString(8);
            mspoint.orientation = c.getDouble(9);
            c.close();
            return mspoint;
        }else {
            c.close();
            return null;
        }
    }
    //清空db中的数据
    public void clearData(){
        Log.i("MyDebug", "DbManager 清空数据！！");
        db.execSQL("DELETE FROM mission");
        //db.execSQL("UPDATE sqlite_sequence SET seq=0 WHERE name='mission'");
        db.execSQL("DELETE FROM ms_point");
    }
    /**关闭db*/
    public void closeDB(){
        Log.i("MyDebug", "DbManager 关闭defaultDB！");
        db.close();
    }
}
