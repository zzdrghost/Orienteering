package com.exc.zhen.orienteering;


import java.util.*;
import org.json.*;

/**
 * Created by ZHEN on 2015/10/8 0008.
 */
public class JsonTools {
    static JSONArray lmsToJa(List<Mission> lms){
        JSONArray ja = new JSONArray();
        for (Mission ms: lms) {
            ja.put(toJo(ms));
        }
        return ja;
    }
    static JSONArray lmspToJa(List<MsPoint> lmsp){
        JSONArray ja = new JSONArray();
        for (MsPoint msp: lmsp) {
            ja.put(toJo(msp));
        }
        return ja;
    }
    static JSONObject toJo(Mission ms){
        Map<String,Object> map = new HashMap<>();
        map.put("missionId",ms.missionId);
        map.put("name",ms.name);
        map.put("state",ms.state);
        map.put("limitTime",ms.limitTime);
        map.put("startTime",ms.startTime);
        return new JSONObject(map);
    }
    static JSONObject toJo(MsPoint msp){
        Map<String,Object> map = new HashMap<>();
        map.put("missionId",msp.missionId);
        map.put("orderNum",msp.orderNum);
        map.put("state",msp.state);
        map.put("latitude",msp.latitude);
        map.put("longitude",msp.longitude);
        map.put("height",msp.height);
        map.put("question",msp.question);
        map.put("answer",msp.answer);
        map.put("imgAddress",msp.imgAddress);
        map.put("orientation",msp.orientation);
        return new JSONObject(map);
    }
    static Mission joToMs(JSONObject jo){
        if (5==jo.length()){
            try {
                return new Mission(jo.getInt("missionId"),jo.getString("name"),jo.getString("state")
                        ,jo.getString("limitTime"),jo.getString("startTime"));
            }catch (JSONException je){
                je.printStackTrace();
            }
        }
        return null;
    }
    static MsPoint joToMsp(JSONObject jo){
        if (10==jo.length()){
            try {
                return new MsPoint(jo.getInt("missionId"),jo.getInt("orderNum"),jo.getString("state")
                        ,jo.getDouble("latitude"),jo.getDouble("longitude"),jo.getDouble("height")
                        ,jo.getString("question"),jo.getString("answer"),jo.getString("imgAddress")
                        ,jo.getDouble("orientation"));
            }catch (JSONException je){
                je.printStackTrace();
            }
        }
        return null;
    }
}
