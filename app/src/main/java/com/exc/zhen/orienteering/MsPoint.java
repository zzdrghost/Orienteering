package com.exc.zhen.orienteering;

/**
 * Created by ZHEN on 2015/9/7 0007.
 * 任务点类
 */
public class MsPoint {
    public int mission_id = 0;//所属任务id
    public int order_num = 0;//序号
    public String state = "未完成";//完成状况
    public double latitude = 0.0;//纬度
    public double longitude = 0.0;//经度
    public double height = 0.0;//高度
    public String question = "";//问题
    public String answer = "";//答案
    public String img_address = "";//图片地址
    public double orientation = -1;//方向

    MsPoint(){}
    MsPoint(int mission_id,int order_num,String state,double latitude,double longitude,
            double height,String question,String answer,String img_address,double orientation){
        this.mission_id = mission_id;
        this.order_num = order_num;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.height = height;
        this.question = question;
        this.answer = answer;
        this.img_address = img_address;
        this.orientation = orientation;
    }
}
