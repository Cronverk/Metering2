package com.example.grass.metering2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.grass.metering2.calibration.CalibrationActivity;
import com.example.grass.metering2.calibration.DalCalibrActivity;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Grass on 10.03.2016.
 */
public class MeteringDialog extends DialogFragment implements View.OnClickListener {

    private SharedPreferences mSettings;
    private EditText editText;
    private View view;
    MeteringActivity activity;
    private AlertDialog dialog;

    private double height;
    private Context context;

    HashMap<String,Double> map;

    public void setMeteringActivity(MeteringActivity activity){
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setViews(){
        map        = getData();
        if(map!=null) {
            editText.setText("" + map.get("height"));
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d("crea", "createDialog");

        mSettings = PreferenceManager.getDefaultSharedPreferences(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        view  = inflater.inflate(R.layout.dialog_metering, null);
        builder.setView(view);

        editText   = (EditText)   view.findViewById(R.id.editHeight);
        Button button = (Button) view.findViewById(R.id.buttonOk);
        Button butdal = (Button) view.findViewById(R.id.butDal);
        button.setOnClickListener(this);
        butdal.setOnClickListener(this);

        dialog     = builder.create();
       // setViews();

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        setViews();
    }

    private void saveData(float height){
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putFloat("height", height);
        editor.commit();
    }
    private HashMap<String,Double> getData(){
        if (mSettings==null)
            return null;
        if (mSettings.getAll().size() ==0 )
            return null;
        Map map = mSettings.getAll();
        height  = Double.valueOf(""+map.get("height"));

        HashMap<String,Double> data = null;
        if(height != 0 ) {
            data = new HashMap();
            data.put("height", height);
        }
        return data;
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {

        setViews();
        return super.show(transaction, tag);
    }
    public void setEditText(String height){
        editText.setText(height);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() ==R.id.butDal){
            activity.stopTask();
            Intent intent = new Intent(activity, DalnometerActivity.class);
            //intent.putExtra("",mSettings);
            startActivity(intent);
        }
        else {
            double height = 0;
            try {
                height = Double.parseDouble(editText.getText().toString());
            } catch (Exception e) {

            }
            if (height > 0) {
                saveData(Float.valueOf("" + height));
                this.height = height;
                activity.resetActivity();
                dialog.dismiss();
            } else
                Toast.makeText(activity.getApplicationContext(),
                        "Значення має бути більше нуля ", Toast.LENGTH_LONG).show();
        }
    }

    public double getParams(){
        return height;
    }
}
