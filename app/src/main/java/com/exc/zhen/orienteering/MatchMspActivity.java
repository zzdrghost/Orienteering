package com.exc.zhen.orienteering;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public class MatchMspActivity extends Activity {
    private final static String TAG = "MyDebug";
    private SensorManager sm;
    private Sensor aSensor,mSensor;
    double [] sensor_paras;
    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];
    final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType())
                accelerometerValues = event.values;
            if (Sensor.TYPE_MAGNETIC_FIELD == event.sensor.getType())
                magneticFieldValues = event.values;
            float[] values = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(R, values);
            if (0 > values[0])
                values[0] = (float) Math.toDegrees(values[0])+360f;
            else
                values[0] = (float) Math.toDegrees(values[0]);

            if (match(sensor_paras[0],values[0])){
                sm.unregisterListener(sensorListener);
                Intent intent = new Intent();
                intent.putExtra("sensor_result",1);
                MatchMspActivity.this.setResult(Activity.RESULT_OK, intent);
                MatchMspActivity.this.finish();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    boolean match(double sensor_para,float current_para){
        return (Math.abs(sensor_para-current_para)<5);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "MatchMspActivity onPause");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_msp);
        //获取orientation参数
        Intent intent = getIntent();
        sensor_paras = intent.getDoubleArrayExtra("cur_sensor_paras");
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(sensorListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(sensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onStart() {
        Log.i(TAG, "MatchMspActivity onStart");
        super.onStart();
    }
    @Override
    protected void onRestart(){
        Log.i(TAG, "MatchMspActivity onRestart");
        super.onRestart();
    }
    @Override
    protected void onPause() {
        Log.i(TAG, "MatchMspActivity onPause");
        super.onPause();
        sm.unregisterListener(sensorListener);
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "MatchMspActivity onStop");
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        Log.i(TAG, "MatchMspActivity onDestroy");
        super.onDestroy();
    }
}
