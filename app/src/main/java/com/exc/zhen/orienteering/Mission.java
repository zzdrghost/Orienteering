package com.exc.zhen.orienteering;

/**
 * Created by ZHEN on 2015/9/7 0007.
 * 定向任务类
 */
public class Mission {
    public int missionId = 0;//任务id,插入时自动分配
    public String name = "";//任务名称（标题）
    public String state = "未完成";//任务状态
    public String limitTime = "";//任务时间限制
    public String startTime = "";//如果是执行中任务，记录任务开始时间

    Mission(){}
    Mission(int missionId,String name,String state,String limitTime,String startTime){
        this.missionId = missionId;
        this.name = name;
        this.state = state;
        this.limitTime = limitTime;
        this.startTime = startTime;
    }
    public void setMissionId(int missionId) {
        this.missionId=missionId;
    }
    public int getMissionId() {
        return this.missionId;
    }
    public void setName(String name) {
        this.name=name;
    }
    public String getName() {
        return this.name;
    }
    public void setState(String state) {
        this.state=state;
    }
    public String getState() {
        return this.state;
    }
    public void setLimitTime(String limitTime) {
        this.limitTime=limitTime;
    }
    public String getLimitTime() {
        return this.limitTime;
    }
    public void setStartTime(String startTime) {
        this.startTime=startTime;
    }
    public String getStartTime() {
        return this.startTime;
    }
}
