package com.exc.zhen.orienteering;

/**
 * Created by ZHEN on 2015/9/7 0007.
 * 定向任务类
 */
public class Mission {
    public int mission_id = 0;//任务id,插入时自动分配
    public String name = "";//任务名称（标题）
    public String state = "未完成";//任务状态
    public String limit_time = "";//任务时间限制
    public String start_time = "";//如果是执行中任务，记录任务开始时间

    Mission(){}
    Mission(int mission_id,String name,String state,String limit_time,String start_time){
        this.mission_id = mission_id;
        this.name = name;
        this.state = state;
        this.limit_time = limit_time;
        this.start_time = start_time;
    }
}
