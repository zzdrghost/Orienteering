package com.exc.zhen.orienteering;

/**
 * Created by ZHEN on 2015/9/7 0007.
 * 任务点类
 */
public class MsPoint {
    public int missionId = 0;//所属任务id
    public int orderNum = 0;//序号
    public String state = "未完成";//完成状况
    public double latitude = 0.0;//纬度
    public double longitude = 0.0;//经度
    public double height = 0.0;//高度
    public String question = "";//问题
    public String answer = "";//答案
    public String imgAddress = "";//图片地址
    public double orientation = -1;//方向

    MsPoint(){}
    MsPoint(int missionId,int orderNum,String state,double latitude,double longitude,
            double height,String question,String answer,String imgAddress,double orientation){
        this.missionId = missionId;
        this.orderNum = orderNum;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.height = height;
        this.question = question;
        this.answer = answer;
        this.imgAddress = imgAddress;
        this.orientation = orientation;
    }
    public void setMissionId(int missionId) {
        this.missionId=missionId;
    }
    public int getMissionId() {
        return this.missionId;
    }
    public void setOrderNum(int orderNum) {
        this.orderNum=orderNum;
    }
    public int getOrderNum() {
        return this.orderNum;
    }
    public void setState(String state) {
        this.state=state;
    }
    public String getState() {
        return this.state;
    }
    public void setQuestion(String question) {
        this.question=question;
    }
    public String getQuestion() {
        return this.question;
    }
    public void setAnswer(String answer) {
        this.answer=answer;
    }
    public String getAnswer() {
        return this.answer;
    }
    public void setImgAddress(String imgAddress) {
        this.imgAddress=imgAddress;
    }
    public String getImgAddress() {
        return this.imgAddress;
    }
    public void setLatitude(Double latitude) {
        this.latitude=latitude;
    }
    public Double getLatitude() {
        return this.latitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude=longitude;
    }
    public Double getLongitude() {
        return this.longitude;
    }
    public void setHeight(Double height) {
        this.height=height;
    }
    public Double getHeight() {
        return this.height;
    }
    public void setOrientation(Double orientation) {
        this.orientation=orientation;
    }
    public Double getOrientation() {
        return this.orientation;
    }
}
