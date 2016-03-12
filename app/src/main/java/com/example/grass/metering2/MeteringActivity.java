package com.example.grass.metering2;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.grass.metering2.validation.Constants;
import com.example.grass.metering2.validation.MyValidator;
import com.example.grass.metering2.validation.ValidationCallback;

import java.util.ArrayList;
import java.util.Calendar;

import com.example.grass.metering2.validation.Constants.*;

public class MeteringActivity extends Activity implements View.OnClickListener, SensorEventListener,
        ValidationCallback{
    MeteringDialog dialog;
    SensorManager sensorManager;
    SharedPreferences preferences;

    private float[] accelerometerValues;
    private float[] magneticFieldValues;
    TextView heightView;
    TextView alphaView;
    TextView bettaView;

    MeteringTask mtask;
    AngleTask    atask;

    private double alpha;
    private double betta;

    Sensor accelerometer;
    Sensor magneticField;

    private int taskCounter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkDate()){
            MyValidator val = new MyValidator(getApplicationContext(),this);
            val.execute();
        }

        setContentView(R.layout.activity_metering);
        dialog = new MeteringDialog();
        dialog.setMeteringActivity(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this,magneticField,SensorManager.SENSOR_DELAY_UI);

        heightView = (TextView)findViewById(R.id.heightValue);
        alphaView  = (TextView)findViewById(R.id.alphaValue);
        bettaView  = (TextView)findViewById(R.id.bettaValue);


        dialog.show(getFragmentManager(),"Налаштування");


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    boolean  checkDate(){

        long installed = 0;
        try {
            installed = this.getPackageManager().getPackageInfo(this.getPackageName(),0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        long diff = Calendar.getInstance().getTimeInMillis() - installed;
        long days = diff/(24*60*60*1000);

        Calendar dayD = Calendar.getInstance();
        dayD.set(Calendar.DAY_OF_MONTH,15);
        dayD.set(Calendar.MONTH, 7);
        dayD.set(Calendar.YEAR, 2016);

        long dayDdiff = Calendar.getInstance().getTimeInMillis() - dayD.getTimeInMillis();
        long daysD = dayDdiff/(24*60*60*100);


        if (days<30&&dayDdiff<0){

            return true;
        }
        else {
            return preferences.getBoolean(Constants.VAL, false);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        startTask();

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layer:

                if(taskCounter <= 2) {
                atask.stopTask();
                taskCounter++;
                    atask = new AngleTask();
                    atask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 2.0);
                }
                break;
            case R.id.buttonChange:
                dialog.show(getFragmentManager(), "Налаштування");
                resetActivity();
                break;
            case R.id.buttonUpdate:
                resetActivity();
                startTask();
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

        if(magneticFieldValues !=null) {
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(R, values);

            values[0] = (float) Math.toDegrees(values[0]);
            values[1] = (float) Math.toDegrees(values[1]);
            values[2] = (float) Math.toDegrees(values[2]);
            Log.d("orientation","orientation 1 "+ values[0]+" " +values[1]+ " " + values[2]);
        }else {
            double ax = accelerometerValues[0];
            double ay = accelerometerValues[1];
            double az = accelerometerValues[2];
            double x  = Math.atan(ax/Math.sqrt(ay*ay+az*az));
            double y  = Math.atan(ay/Math.sqrt(ax*ax+az*az));
            double z  = Math.atan(az/Math.sqrt(ay*ay+ax*ax));


            values[0] = (float) Math.toDegrees(x);
            values[1] = (float) Math.toDegrees(y);
            values[2] = (float) Math.toDegrees(z)-90;
            Log.d("orientation","orientation 2 "+ values[0]+" " +values[1]+ " " + values[2]);
        }
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


    public class AngleTask extends AsyncTask<Double,String,Double>{

        private boolean runFlag =true;
        private double num;

        public void stopTask(){
            runFlag = false;
        }
        @Override
        protected Double doInBackground(Double... params) {
            num = params[0];
            double angle = 0.0;
            ArrayList<Double> angles = new ArrayList<>();
            while (runFlag) {
                try {
                    new Thread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                float[] values = getOrientation();
                if (checkRotate(values[2])) {
                    angle = values[1];
                    if(num==1&&values[1] > 0)
                        angle = 0;
                    if(num==2&&values[1] < 0)
                        angle = 0;
                    angles.add(Math.abs(roundNumber(angle, 2)));

                    if(angles.size() == 3) {
                        angle = roundNumber(averageAngle(angles), 2);
                        angles = new ArrayList<>();
                    }
                }
                publishProgress("" + Math.abs(roundNumber(angle, 2)));
            }
            return Math.abs(roundNumber(angle,2));
        }
        protected void onProgressUpdate(String... data) {
            switch ((int)num){
                case 1:
                    alphaView.setText(data[0]);
                    break;
                case 2:
                    bettaView.setText(data[0]);
                    break;
            }
        }

        @Override
        protected void onPostExecute(Double value) {
            super.onPostExecute(value);
            switch ((int)num){
                case 1:
                    alphaView.setText(""+value);
                    alpha = value;
                    break;
                case 2:
                    bettaView.setText(""+value);
                    betta = value;
                    break;
            }
        }
    }

    public class MeteringTask extends AsyncTask<Void,Double,Double>{
        private boolean runFlag =true;
        @Override
        protected Double doInBackground(Void... params) {
            Log.d("ran","ran");
            double height = 0.0;
            while (runFlag){
                try {
                    new Thread().sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(alpha>0.0 && betta>0.0 ){
                    runFlag = false;
                    height  = calculateHeight(alpha,betta,dialog.getParams());
                }
            }
            return roundNumber(height,2);
        }

        @Override
        protected void onPostExecute(Double aDouble) {
            super.onPostExecute(aDouble);
            heightView.setText(""+aDouble);
        }
    }

    public void startTask(){
        mtask= new MeteringTask();
        mtask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void resetActivity(){
        alphaView.setText("00.00");
        bettaView.setText("00.00");
        heightView.setText("00.00");
        alpha = 0;
        betta = 0;
        taskCounter=1;

    }

    private double calculateHeight(double alpha,double betta, double a){
        double h   = roundNumber(a/Math.tan(Math.toRadians(alpha)),2);
        double h1  = roundNumber(a*Math.tan(Math.toRadians(betta)),2);
        return h+h1;
    }
    public double roundNumber(double number , double accurancy){
        accurancy = Math.pow(10,accurancy);
        number    = Math.round(number*accurancy);

        return number/accurancy;
    }


}
