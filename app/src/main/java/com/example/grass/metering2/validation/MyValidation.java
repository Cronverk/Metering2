package com.example.grass.metering2.validation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v1.DbxClientV1;
import com.dropbox.core.v1.DbxEntry;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import static com.example.grass.metering2.Constants.DRIVE_URL;
import static com.example.grass.metering2.Constants.TAG;

/**
 * Created by Yaroslav on 08.03.2016.
 */
public class MyValidation {
    boolean valid = false;
    SharedPreferences preferences;

    public boolean isValid(String imei, Context context) {

        preferences = context.getSharedPreferences("my_settings" , Context.MODE_PRIVATE);
        DbxRequestConfig config = new DbxRequestConfig("taxation", Locale.getDefault().toString());
        DbxClientV1 clientV2 = new DbxClientV1(config,DRIVE_URL);
        Log.d(TAG, "isValid: ");
        //String sr =
        FileOutputStream outStream = null;
        StringBuilder text = null;
        try {
            Log.d(TAG, "isValid: " + clientV2.getAccountInfo().email);

            outStream = new FileOutputStream(context.getFilesDir() + "/licenVisBesBas.txt");
            DbxEntry.File downloadFile = clientV2.getFile("/licenVisBesBas.txt",null, outStream);
            Log.d(TAG, "isValid: file " + downloadFile.asFile().toString());
            outStream.close();

            text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(context.getFilesDir() + "/licenVisBesBas.txt"));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();

            Log.d(TAG, "isValid: chek " + text.toString().contains(imei));
            if (text.toString().contains(imei)){
                Log.d(TAG, "isValid: works");
                Calendar dayD = Calendar.getInstance();
                dayD.add(Calendar.DATE,365);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("dayD",dayD.getTimeInMillis());
                editor.apply();

                return true;
            }
        }
        catch (DbxException | IOException e) {
            e.printStackTrace();
            Log.e(TAG, "isValid: "+ e.toString());
        }
        return false;
    }


}
