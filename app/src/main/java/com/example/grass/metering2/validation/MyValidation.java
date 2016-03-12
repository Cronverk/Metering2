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
import java.util.Locale;

import static com.example.grass.metering2.validation.Constants.*;

/**
 * Created by Yaroslav on 08.03.2016.
 */
public class MyValidation {
    boolean valid = false;
    SharedPreferences preferences;

    public boolean isValid(String imei, Context context) {

        DbxRequestConfig config = new DbxRequestConfig("taxation", Locale.getDefault().toString());
        DbxClientV1 clientV2 = new DbxClientV1(config,DRIVE_URL);
        //String sr =
        FileOutputStream outStream = null;
        StringBuilder text = null;
        try {

            outStream = new FileOutputStream(context.getFilesDir() + "/licen.txt");
            DbxEntry.File downloadFile = clientV2.getFile("/licen.txt",null, outStream);
            outStream.close();

            text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(context.getFilesDir() + "/licen.txt"));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            if (text.toString().contains(imei)){
                return true;
            }
        }
        catch (DbxException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }


}
