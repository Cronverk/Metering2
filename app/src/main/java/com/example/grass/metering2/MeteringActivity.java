package com.example.grass.metering2;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.grass.metering2.validation.MyValidator;
import com.example.grass.metering2.validation.ValidationCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import static com.example.grass.metering2.Constants.*;

public class MeteringActivity extends Activity implements View.OnClickListener, SensorEventListener,
        ValidationCallback,SoundPool.OnLoadCompleteListener {
    MeteringDialog dialog;
    SensorManager sensorManager;

    SoundPool sp;
    int sound;

    private float[] accelerometerValues;
    private float[] magneticFieldValues;
    TextView heightView;
    TextView alphaView;
    TextView bettaView;

    //MeteringTask mtask;
    AngleTask    atask;

    Sensor accelerometer;
    Sensor magneticField;

    private int taskCounter = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_metering2);
        dialog = new MeteringDialog();
        dialog.setMeteringActivity(this);

        sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        sp.setOnLoadCompleteListener(this);
        try {
            sound = sp.load(getAssets().openFd("1897.ogg"), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this,magneticField,SensorManager.SENSOR_DELAY_UI);

        heightView = (TextView)findViewById(R.id.heightValue);
        alphaView  = (TextView)findViewById(R.id.alphaValue);
        bettaView  = (TextView)findViewById(R.id.bettaValue);


        dialog.show(getFragmentManager(), "Налаштування");
        //checkDate();


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        resetActivity();
    }

    boolean checkDate(){

        SharedPreferences preferences = getSharedPreferences("my_settings" , MODE_PRIVATE);

        long timeX = preferences.getLong("dayD",0) - Calendar.getInstance().getTimeInMillis();

        Log.d(TAG, "checkDateDiff: " + " timeX" + timeX/(24*60*60*1000));
        if (timeX > 0 ){
            Log.d(TAG, "checkDate() returned: " + true);
            return true;

        }
        else {
            Log.d(TAG, "checkDate() returned: " + false);
            MyValidator val = new MyValidator(getApplicationContext(),this);
            val.execute();
            return preferences.getBoolean(VAL, false);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        resetActivity();

        //startTask();

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layer:
                taskCounter++;
                if(taskCounter==3)
                    atask.stopTask();
                if(taskCounter<=3)
                    sp.play(sound, 1, 1, 0, 0, 1);
                break;
            case R.id.buttonChange:
                dialog.show(getFragmentManager(), "Налаштування");
                resetActivity();
                break;
            case R.id.buttonUpdate:
                resetActivity();
               // startTask();
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            accelerometerValues = event.values;

        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            magneticFieldValues = event.values;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float[] getOrientation(){
        float[] values = new float[3];
/*
        if(magneticFieldValues !=null) {
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(R, values);

            values[0] = (float) Math.toDegrees(values[0]);
            values[1] = (float) Math.toDegrees(values[1]);
            values[2] = (float) Math.toDegrees(values[2]);
            Log.d("orientation","orientation 1 "+ values[0]+" " +values[1]+ " " + values[2]);
        }else {*/
            if(accelerometerValues!=null) {
                double ax = accelerometerValues[0];
                double ay = accelerometerValues[1];
                double az = accelerometerValues[2];
                double x = Math.atan(ax / Math.sqrt(ay * ay + az * az));
                double y = Math.atan(ay / Math.sqrt(ax * ax + az * az));
                double z = Math.atan(az / Math.sqrt(ay * ay + ax * ax));


                values[0] = (float) Math.toDegrees(x);
                values[1] = (float) Math.toDegrees(y);
                values[2] = (float) Math.toDegrees(z) - 90;
            }else values = new float[]{0,0,0};
            Log.d("orientation","orientation 2 "+ values[0]+" " +values[1]+ " " + values[2]);
      //  }
        return values;
    }
    public boolean checkRotate(float angle){
        if(Math.abs(angle)>60&& Math.abs(angle)<115){
            return true;
        }
        return false;
    }
    public double averageAngle(ArrayList<Double> angles){
        double sum = 0;
        for(double value : angles)
            sum +=value;
        return sum/angles.size();
    }

    @Override
    public void valid(Boolean valid) {

        if (valid){
            SharedPreferences preferences = getSharedPreferences("my_settings" , MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.VAL,true);
            editor.apply();

        }else{
            showDiatog();
        }
    }

    public void showDiatog(){
        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String imei = manager.getDeviceId();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Незареєстрована копія")
                .setMessage("Для покупки перейдіть на сайт видавця та повідомте цей номер " + imei + ".")
                .setCancelable(false);
        builder.setNegativeButton("Відмова", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setPositiveButton("На сайт", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.lesovod.com.ua"));
                startActivity(browserIntent);
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();


        dialog.show();
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

    }


    public class AngleTask extends AsyncTask<Double,String,Double>{

        private boolean runFlag =true;
        private double num = 0;

        private double alpha = -1;
        private double betta = -1;

        public void stopTask(){
            runFlag = false;
        }


        @Override
        protected Double doInBackground(Double... params) {
            double angle = 0.0;
            ArrayList<Double> angles = new ArrayList<>();
            while (taskCounter!=3) {
                try {
                    new Thread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                float[] values = getOrientation();
                if (checkRotate(values[2])) {
                    angle = values[1];
                    if(taskCounter==1&&values[1] < 0)
                        angle = 0;
                    if(taskCounter==2&&values[1] > 0)
                        angle = 0;
                    angles.add(Math.abs(roundNumber(angle, 2)));

                    if(angles.size() == 3) {
                        angle = roundNumber(averageAngle(angles), 2);
                        angles = new ArrayList<>();
                    }
                }
                publishProgress("" + Math.abs(roundNumber(angle, 2)));
            }
            return 3.0;
        }
        protected void onProgressUpdate(String... data) {
            switch ((int)taskCounter){
                case 1:
                    alphaView.setText(data[0]);
                    alpha = Double.parseDouble(data[0]);
                    break;
                case 2:
                    bettaView.setText(data[0]);
                    betta = Double.parseDouble(data[0]);
                    break;
            }
        }

        @Override
        protected void onPostExecute(Double value) {
            super.onPostExecute(value);

            if(alpha>=0&&betta>=0) {
                double height = 0;
                if(alpha!=0 && betta !=0)
                    height = calculateHeight(alpha, betta, dialog.getParams());
                heightView.setText("" + roundNumber(height, 2));
            }
        }
    }
    public void resetActivity(){
        alphaView.setText("00.00");
        bettaView.setText("00.00");
        heightView.setText("00.00");
        taskCounter=1;

        atask = new AngleTask();
        atask.execute(1.0);
    }

    private double calculateHeight(double alpha,double betta, double a){
        double h   = roundNumber(a*Math.tan(Math.toRadians(alpha)),2);
        double h1  = roundNumber(a*Math.tan(Math.toRadians(betta)),2);
        return h+h1;
    }
    public double roundNumber(double number , double accurancy){
        accurancy = Math.pow(10,accurancy);
        number    = Math.round(number*accurancy);

        return number/accurancy;
    }


}
