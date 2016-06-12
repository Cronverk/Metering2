package com.example.grass.metering2;


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
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.grass.metering2.calibration.CalibrationActivity;
import com.example.grass.metering2.validation.MyValidator;
import com.example.grass.metering2.validation.ValidationCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import static com.example.grass.metering2.Constants.*;

public class MeteringActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener,
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
    static SharedPreferences spAccurate;
    Menu menu;
    Toolbar toolbar;

    //MeteringTask mtask;
    AngleTask    atask;

    Sensor accelerometer;
    Sensor magneticField;

    private int taskCounter = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_metering2);

        spAccurate = getSharedPreferences("ACCURATE", MODE_PRIVATE);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

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
        //resetActivity();
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


    }

    public void stopTask(){
        atask.stopTask();
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
                //resetActivity();
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
    private void enableButtons(boolean status){
        Button b = (Button)findViewById(R.id.buttonChange);
        b.setEnabled(status);
        b =(Button)findViewById(R.id.buttonUpdate);
        b.setEnabled(status);
        if(status){
            RelativeLayout layer = (RelativeLayout) findViewById(R.id.layer);
            layer.setVisibility(View.GONE);




          //  menu.getItem(R.).setEnabled(false);
           // menu.getItem(2).setEnabled(false);
        }
        else{
            RelativeLayout layer = (RelativeLayout) findViewById(R.id.layer);
            layer.setVisibility(View.VISIBLE);
           }
        Menu menu = toolbar.getMenu();
        menu.setGroupVisible(R.id.main_menu_group, status);
    }


    public class AngleTask extends AsyncTask<Double,Double,Double>{

        private boolean runFlag =true;
        private double num = 0;

        private double alpha = -1;
        private double betta = -1;

        public void stopTask(){
            this.runFlag = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            enableButtons(false);
        }

        @Override
        protected Double doInBackground(Double... params) {
            double angle = 0.0;
            ArrayList<Double> angles = new ArrayList<>();
            while (taskCounter!=3&&runFlag==true) {
                try {
                    new Thread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                float[] values = getOrientation();
                if (checkRotate(values[2])) {
                    angle = values[1];
                    if(taskCounter==1&&values[1] > 0)
                        angle = 0;
                    if(taskCounter==2&&values[1] < 0)
                        angle = 0;
                    angles.add(Math.abs(roundNumber(angle, 2)));

                    if(angles.size() == 3) {
                        angle = roundNumber(averageAngle(angles), 2);
                        angles = new ArrayList<>();
                    }
                }
                publishProgress(angle);
            }
            return 3.0;
        }
        protected void onProgressUpdate(Double... data) {
            switch ((int)taskCounter){
                case 1:
                    bettaView.setText(doubleToDegree(Math.abs(data[0])));
                    betta = data[0];
                    break;
                case 2:
                    alphaView.setText(doubleToDegree(Math.abs(data[0])));
                    alpha = data[0];
                    break;
            }
        }

        @Override
        protected void onPostExecute(Double value) {
            super.onPostExecute(value);

            if(alpha>=0&&betta>=0) {
                double height = 0;
                if(alpha!=0 && betta !=0)
                    height = roundNumber(calculateHeight(alpha, betta, dialog.getParams()),1);
                heightView.setText("" + height);
            }
            enableButtons(true);
        }
    }

    public static String doubleToDegree(double value){
        int degree = (int) value;
        double rawMinute = Math.abs((value % 1) * 60);
        int minute = (int) rawMinute;
        int second = (int) Math.round((rawMinute % 1) * 60);
        return String.format("%d° %d′ %d″", degree,minute,second);
    }

    public void resetActivity(){
        alphaView.setText("00.00");
        bettaView.setText("00.00");
        heightView.setText("00.00");
        taskCounter=1;

        atask = new AngleTask();
        atask.execute(1.0);
    }

    public static double calculateHeight(double alpha,double betta, double h1){

        double acc_l  = Double.parseDouble(spAccurate.getString("calibr2accurate","0.0"));
        double acc_h  = Double.parseDouble(spAccurate.getString("calibr1accurate","0.0"));
        double len = h1/Math.tan(Math.toRadians(betta))+acc_l;
        double h2  = len*Math.tan(Math.toRadians(alpha))+acc_h;
        Log.d("accurate", "l = "+acc_l);
        Log.d("accurate", "h = "+acc_h);
        return h1+h2;
    }
    public double roundNumber(double number , double accurancy){
        accurancy = Math.pow(10,accurancy);
        number    = Math.round(number*accurancy);

        return number/accurancy;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {return;}
        String length = data.getStringExtra("length");
        SharedPreferences mSetting = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = mSetting.edit();
            editor.putString("height",length);
            editor.commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedPreferences.Editor editor = spAccurate.edit();
        editor.clear();
        editor.commit();
        stopTask();
        int id = item.getItemId();
        if (id == R.id.action_calibr2) {
            stopTask();
            Intent intent = new Intent(this, CalibrationActivity.class);
            intent.putExtra("calibrType","calibr2");
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_calibr1) {
            stopTask();
            Intent intent = new Intent(this, CalibrationActivity.class);
            intent.putExtra("calibrType","calibr1");
            startActivity(intent);
            return true;
        }
        if(id == R.id.action_help){
            stopTask();
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
