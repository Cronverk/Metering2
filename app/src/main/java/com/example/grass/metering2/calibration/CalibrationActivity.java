package com.example.grass.metering2.calibration;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;


import com.example.grass.metering2.R;
import com.example.grass.metering2.list.Item;
import com.example.grass.metering2.list.ItemAdapter;

import java.util.ArrayList;

public class CalibrationActivity extends Activity implements View.OnClickListener {
    ItemAdapter adapter;
    ArrayList<Item> list;
    int colibrCount = 0;
    SharedPreferences spAccurate;
    int itemPoistion = 0;
    Context context;
    String calibrType ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        context = this;
        list= new ArrayList<Item>();
        adapter = new ItemAdapter(this,list);
        spAccurate = getSharedPreferences("ACCURATE", MODE_PRIVATE);

        calibrType = getIntent().getStringExtra("calibrType");

        if(spAccurate.contains(calibrType+"colibrCount")!=true) {
            spAccurate.edit().putString(calibrType+"colibrCount", "0");
            spAccurate.edit().apply();
        }
        else
            colibrCount = Integer.parseInt(spAccurate.getString(calibrType+"colibrCount",""));

        importAccuracy(colibrCount);

        ListView view = (ListView)findViewById(R.id.listView);
        if(view!=null)
        view.setAdapter(adapter);
        Button but  = (Button)findViewById(R.id.button4);
        but.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button4:
                Intent intent = new Intent(this, DalCalibrActivity.class);
                intent.putExtra("calibrType",calibrType);
                startActivityForResult(intent,1);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data!=null){
            double angle  = 0.0;
            double length = 0.0;
            double eyeLength = 0.0;
            double eyeHeight= 0.0;
            angle = data.getDoubleExtra(calibrType+"angle",angle);
            length = data.getDoubleExtra(calibrType+"value",length);
            eyeLength = data.getDoubleExtra(calibrType+"eyeLength",eyeLength);
            eyeHeight = data.getDoubleExtra(calibrType+"eyeHeight",eyeHeight);
            double accurate = roundNumber(1-(length/eyeLength),2);

            adapter.notifyDataSetChanged();
            list.add(new Item(eyeHeight,length,angle,eyeLength,accurate));
            addAccuracy(++colibrCount, angle, eyeLength, eyeHeight, accurate, length);
            Log.d("log","angle = "+angle+"length = "+length);
        }
    }

    private void addAccuracy(int id,double angle,double eyeLength,double eyeHeight,double accurate, double length){
        SharedPreferences.Editor editor = spAccurate.edit();
            editor.putString(calibrType+"angle"+id,""+angle);
            editor.putString(calibrType+"eyeLength"+id,""+eyeLength);
            editor.putString(calibrType+"eyeHeight"+id,""+eyeHeight);
            editor.putString(calibrType+"accurate"+id,""+accurate);
            editor.putString(calibrType+"value"+id,""+length);
            if(id>colibrCount)
                colibrCount++;
            editor.putString(calibrType+"colibrCount",""+colibrCount);
            editor.putString(calibrType+"accurate",""+countDisp());
        editor.commit();

    }
    private void importAccuracy(int count){

        for(int i  = 1;i <= count; i++){
            double angle = Double.parseDouble(spAccurate.getString(calibrType+"angle"+i,"0.0"));
            double eyeLength = Double.parseDouble(spAccurate.getString(calibrType+"eyeLength"+i,"0.0"));
            double eyeHeight = Double.parseDouble(spAccurate.getString(calibrType+"eyeHeight"+i,"0.0"));
            double accurate = Double.parseDouble(spAccurate.getString(calibrType+"accurate"+i,"0.0"));
            double length = Double.parseDouble(spAccurate.getString(calibrType+"value"+i,"0.0"));
            list.add(new Item(eyeHeight,length,angle,eyeLength,accurate));
        }
    }

    private double countDisp(){
        double sum  = 0;
        double disp = 0;
        for(Item item : list)
            sum += item.getError()/list.size();
        for(Item item : list){
            disp+=Math.pow(item.getError() - sum,2)/list.size();
        }
        return disp;
    }
    public double roundNumber(double number, double accurancy) {
        accurancy = Math.pow(10, accurancy);
        number = Math.round(number * accurancy);

        return number / accurancy;
    }
}
