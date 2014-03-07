package pl.debuger.irbishdmi;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends Activity {

    static final String ENABLE_HDMI         = "/mnt/sdcard/IrbisHdmiSwitcher/enable/hdmi.ATM702X.so";
    static final String ENABLE_HWCOMPOSSER  = "/mnt/sdcard/IrbisHdmiSwitcher/enable/hwcomposer.ATM702X.so";
    static final String DISABLE_HDMI        = "/mnt/sdcard/IrbisHdmiSwitcher/disable/hdmi.ATM702X.so";
    static final String DISABLE_HWCOMPOSSER = "/mnt/sdcard/IrbisHdmiSwitcher/disable/hwcomposer.ATM702X.so";
    static final String SYSTEM_LIB_HW_PATH  = "/system/lib/hw";
    static final String CURRENT_HDMI_LIB    = SYSTEM_LIB_HW_PATH + "/hdmi.ATM702X.so";
    static final String CURRENT_HWCOMPOSER  = SYSTEM_LIB_HW_PATH + "/hwcomposer.ATM702X.so";
    static final String CONFIG              = "config";
    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        settings = getSharedPreferences(CONFIG, 0);
        Boolean hdmisavedstate = settings.getBoolean("hdmistate", false);
        Switch hdmiswitch = (Switch) findViewById(R.id.switch1);
        if (hdmisavedstate) {hdmiswitch.setChecked(true);} else {hdmiswitch.setChecked(false);}
        if (!RootTools.isAccessGiven()) { displayDialog("ERROR", getString(R.string.noroot)); }

        File en_hd = new File(ENABLE_HDMI);
        File en_hw = new File(ENABLE_HWCOMPOSSER);
        File ds_hd = new File(DISABLE_HDMI);
        File ds_hw = new File(DISABLE_HWCOMPOSSER);
        File ir_dr = new File("/mnt/sdcard/IrbisHdmiSwitcher");
        File ir_en = new File("/mnt/sdcard/IrbisHdmiSwitcher/enable");
        File ir_ds = new File("/mnt/sdcard/IrbisHdmiSwitcher/disable");


        if (!en_hd.exists() || !en_hw.exists() || !ds_hd.exists() || !ds_hw.exists()) {
            if (ir_dr.exists()) {
                long startTime = System.currentTimeMillis();
                ExecuteRoot("rm -r /mnt/sdcard/IrbisHdmiSwitcher");
                while (ir_dr.exists() && ((System.currentTimeMillis() - startTime) / 1000) < 6) {
                    //pass
                }
            }
            ir_dr.mkdirs();
            ir_en.mkdirs();
            ir_ds.mkdirs();
            if (isNetworkConnected()) {
                DownloadFromUrl("enable/hdmi.ATM702X.so", ENABLE_HDMI);
                DownloadFromUrl("enable/hwcomposer.ATM702X.so", ENABLE_HWCOMPOSSER);
                DownloadFromUrl("disable/hdmi.ATM702X.so", DISABLE_HDMI);
                DownloadFromUrl("disable/hwcomposer.ATM702X.so", DISABLE_HWCOMPOSSER);
            } else {
                displayDialog("ERROR", getString(R.string.no_network), true);
            }
        }


    }

    public void changeHdmi(View view) {
        Switch hdmiswitch = (Switch) findViewById(R.id.switch1);
        boolean hdmiState = hdmiswitch.isChecked();

        //if (hdmiState) {Toast.makeText(this, "ENABLE", Toast.LENGTH_SHORT).show();} else {Toast.makeText(this, "DISABLE", Toast.LENGTH_SHORT).show();}
        if (hdmiState) {
            File cur_hdmi = new File(CURRENT_HDMI_LIB);
            File cur_comp = new File(CURRENT_HWCOMPOSER);
            if(cur_comp.exists()) cur_comp.delete();
            if(cur_hdmi.exists()) cur_hdmi.delete();

            File hdmi = new File(ENABLE_HDMI);
            File hwcm = new File(ENABLE_HWCOMPOSSER);

            ExecuteRoot("rm -f " + cur_hdmi);
            ExecuteRoot("rm -f " + cur_comp);

            while (cur_hdmi.exists() || cur_comp.exists()) {
                //pass
            }

            ExecuteRoot("cp " + hdmi + " " + cur_hdmi);
            ExecuteRoot("cp " + hwcm + " " + cur_comp);
            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("hdmistate", true).commit();
            //ExecuteRoot("reboot");
            RebootDevice();

        } else {
            File cur_hdmi = new File(CURRENT_HDMI_LIB);
            File cur_comp = new File(CURRENT_HWCOMPOSER);
            if(cur_comp.exists()) cur_comp.delete();
            if(cur_hdmi.exists()) cur_hdmi.delete();

            File hdmi = new File(DISABLE_HDMI);
            File hwcm = new File(DISABLE_HWCOMPOSSER);

            ExecuteRoot("rm -f " + cur_hdmi);
            ExecuteRoot("rm -f " + cur_comp);

            while (cur_hdmi.exists() || cur_comp.exists()) {
                //pass
            }
            ExecuteRoot("cp " + hdmi + " " + cur_hdmi);
            ExecuteRoot("cp " + hwcm + " " + cur_comp);
            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("hdmistate", false).commit();
            //ExecuteRoot("reboot");
            RebootDevice();

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        System.exit(0);
    }


    public void displayDialog(String sTitle, String sMsg){
        final Dialog alertdialog = new Dialog(MainActivity.this);
        alertdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertdialog.setContentView(R.layout.db_dialog);
        TextView alertTitle = (TextView) alertdialog.findViewById(R.id.textView2);
        alertTitle.setText(sTitle);
        TextView alertText = (TextView) alertdialog.findViewById(R.id.textView);
        alertText.setText(sMsg);
        alertText.setTextColor(Color.RED);
        Button dialogButton = (Button) alertdialog.findViewById(R.id.button);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertdialog.dismiss();
            }
        });
        alertdialog.show();
    }
    public void displayDialog(String sTitle, String sMsg, boolean exitdiag){
        final Dialog alertdialog = new Dialog(MainActivity.this);
        alertdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertdialog.setContentView(R.layout.db_dialog);
        TextView alertTitle = (TextView) alertdialog.findViewById(R.id.textView2);
        alertTitle.setText(sTitle);
        TextView alertText = (TextView) alertdialog.findViewById(R.id.textView);
        alertText.setText(sMsg);
        alertText.setTextColor(Color.RED);
        Button dialogButton = (Button) alertdialog.findViewById(R.id.button);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertdialog.dismiss();
                System.exit(0);
            }
        });
        alertdialog.show();
    }
    public void ExecuteRoot(String commandString) {
        CommandCapture command = new CommandCapture(0, commandString);
        try{ RootTools.getShell(true).add(command); }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this ,getString(R.string.noroot), Toast.LENGTH_SHORT).show(); }
    }

    public void RebootDevice(){
        final Dialog alertdialog = new Dialog(MainActivity.this);
        alertdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertdialog.setContentView(R.layout.db_question);
        TextView alertTitle = (TextView) alertdialog.findViewById(R.id.textView2);
        alertTitle.setText(getString(R.string.reboot_title));
        TextView alertText = (TextView) alertdialog.findViewById(R.id.textView);
        alertText.setText(R.string.confirm);
        Button buttonOne = (Button) alertdialog.findViewById(R.id.button);
        buttonOne.setText(R.string.no);
        Button buttonTwo = (Button) alertdialog.findViewById(R.id.button2);
        buttonTwo.setText(R.string.yes);

        alertText.setTextColor(Color.WHITE);

        buttonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertdialog.dismiss();
            }
        });

        buttonTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //execute======================================================>>>>>>>>>>>>>>>>>>>>>>

                Toast.makeText(MainActivity.this, getString(R.string.reboot_title), Toast.LENGTH_SHORT).show();
                CommandCapture command = new CommandCapture(0, "reboot");
                try{ RootTools.getShell(true).add(command).wait(); }
                catch (Exception e) { Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show(); }
                //=============================================================<<<<<<<<<<<<<<<<<<<<<<
                alertdialog.dismiss();
            }
        });

        alertdialog.show();
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    public void DownloadFromUrl(String source, String destination) {
        try {
            URL url = new URL("http://debuger.pl/irbis/" + source);
            File file = new File(destination);

            long startTime = System.currentTimeMillis();
            Log.e("Download", "download begining");
            Log.e("Download", "download url:" + url);
            Log.e("Download", "downloaded file name:" + destination);

            URLConnection ucon = url.openConnection();

            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
 

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.e("Download", "download ready in"
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " sec");

        } catch (Exception e) {
            Log.e("Download", "Error: " + e);
            displayDialog("ERROR", getString(R.string.download_error));
        }

    }
}
