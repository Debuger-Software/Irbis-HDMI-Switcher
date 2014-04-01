package pl.debuger.irbishdmi;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.adsdk.sdk.Ad;
import com.adsdk.sdk.AdListener;
import com.adsdk.sdk.AdManager;
import com.adsdk.sdk.banner.AdView;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends Activity implements AdListener {

    private final String ERRORFILE          = "/mnt/sdcard/IrbisTweak/lastError.txt";
    static final String ENABLE_HDMI         = "/mnt/sdcard/IrbisTweak/enable/hdmi.ATM702X.so";
    static final String ENABLE_HWCOMPOSSER  = "/mnt/sdcard/IrbisTweak/enable/hwcomposer.ATM702X.so";
    static final String DISABLE_HDMI        = "/mnt/sdcard/IrbisTweak/disable/hdmi.ATM702X.so";
    static final String DISABLE_HWCOMPOSSER = "/mnt/sdcard/IrbisTweak/disable/hwcomposer.ATM702X.so";
    static final String CPU_BOOST_FILE      = "/mnt/sdcard/IrbisTweak/boost/cpuboost";
    static final String GPU_BOOST_FILE      = "/mnt/sdcard/IrbisTweak/boost/gpuboost";
    static final String SYSTEM_LIB_HW_PATH  = "/system/lib/hw";
    static final String CURRENT_HDMI_LIB    = SYSTEM_LIB_HW_PATH + "/hdmi.ATM702X.so";
    static final String CURRENT_HWCOMPOSER  = SYSTEM_LIB_HW_PATH + "/hwcomposer.ATM702X.so";
    static final String INIT_D              = "/system/etc/init.d/";
    final Intent SLATEDROID_INTENT          = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.slatedroid.com/topic/102945-custom-romrom-based-on-irbis-tq-72-by-firomi-hdmi-support/"));
    final Intent ANDROIDPL_INTENT           = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.android.com.pl/f1096/quadra-7-us-4-2-2-rom-oparty-na-irbis-tq72-antutu-13k-quadrant-6k-v2-hdmi-support-385450/"));
    static final String CONFIG              = "config";
    SharedPreferences settings;
    ProgressDialog pd;
    static final int NO_NETWORK             = 1;
    static final int DOWNLOAD_ERROR         = 2;
    static final int DOWNLOAD_OK            = 0;
    int networkError                        = DOWNLOAD_OK;

    private RelativeLayout layout;
    private AdView mAdView;
    private AdManager mManager;
    private String DOUBLE_LINE_SEP = "\n\n";
    private String SINGLE_LINE_SEP = "\n";
    String version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, final Throwable e) {
                StackTraceElement[] arr = e.getStackTrace();
                final StringBuffer report = new StringBuffer(e.toString());
                final String lineSeperator = "-------------------------------\n\n";
                report.append(DOUBLE_LINE_SEP);
                report.append("--------- Stack trace ---------\n\n");
                for (int i = 0; i < arr.length; i++) {
                    report.append( "    ");
                    report.append(arr[i].toString());
                    report.append(SINGLE_LINE_SEP);
                }
                report.append(lineSeperator);
                report.append("--------- Cause ---------\n\n");
                Throwable cause = e.getCause();
                if (cause != null) {
                    report.append(cause.toString());
                    report.append(DOUBLE_LINE_SEP);
                    arr = cause.getStackTrace();
                    for (int i = 0; i < arr.length; i++) {
                        report.append("    ");
                        report.append(arr[i].toString());
                        report.append(SINGLE_LINE_SEP);
                    }
                }
                report.append(lineSeperator);
                report.append("--------- Device ---------\n\n");
                report.append("App ver:");
                report.append(version);
                report.append(SINGLE_LINE_SEP);
                report.append("Brand: ");
                report.append(Build.BRAND);
                report.append(SINGLE_LINE_SEP);
                report.append("Device: ");
                report.append(Build.DEVICE);
                report.append(SINGLE_LINE_SEP);
                report.append("Model: ");
                report.append(Build.MODEL);
                report.append(SINGLE_LINE_SEP);
                report.append("Id: ");
                report.append(Build.ID);
                report.append(SINGLE_LINE_SEP);
                report.append("Product: ");
                report.append(Build.PRODUCT);
                report.append(SINGLE_LINE_SEP);
                report.append(lineSeperator);
                report.append("--------- Firmware ---------\n\n");
                report.append("SDK: ");
                report.append(Build.VERSION.SDK_INT);
                report.append(SINGLE_LINE_SEP);
                report.append("Release: ");
                report.append(Build.VERSION.RELEASE);
                report.append(SINGLE_LINE_SEP);
                report.append("Incremental: ");
                report.append(Build.VERSION.INCREMENTAL);
                report.append(SINGLE_LINE_SEP);
                report.append(lineSeperator);

                Log.e("Report ::", report.toString());
                writeToFile(report.toString());
                System.exit(0);
            }
        });

        settings = getSharedPreferences(CONFIG, 0);
        Boolean hdmisavedstate = settings.getBoolean("hdmistate", false);
        Boolean cpusavestate   = settings.getBoolean("cpustate" , false);
        Boolean gpusavestate   = settings.getBoolean("gpustate" , false);
        Switch hdmiswitch = (Switch) findViewById(R.id.switch1);
        Switch cpuswitch  = (Switch) findViewById(R.id.switchCPU);
        Switch gpuswitch  = (Switch) findViewById(R.id.switchGPU);
        if (hdmisavedstate) {hdmiswitch.setChecked(true);} else {hdmiswitch.setChecked(false);}
        if (cpusavestate) {cpuswitch.setChecked(true);} else {cpuswitch.setChecked(false);}
        if (gpusavestate) {gpuswitch.setChecked(true);} else {gpuswitch.setChecked(false);}
        if (!RootTools.isAccessGiven()) { displayDialog("ERROR", getString(R.string.noroot)); }

        new LoadViewTask().execute();

        layout = (RelativeLayout) findViewById(R.id.adsdkContent);
        mManager = new AdManager(this, "http://my.mobfox.com/vrequest.php",
                "b52103e7be1f7a28577dda06537976eb", true);
        mManager.setListener(this);
        if (mAdView != null) {
            removeBanner();
        }
        mAdView = new AdView(this, "http://my.mobfox.com/request.php","b52103e7be1f7a28577dda06537976eb", true, true);
        mAdView.setAdspaceStrict(false); // Optional, tells the server to only supply banner ads that are exactly of the desired size. Without setting it, the server could also supply smaller Ads when no ad of desired size is available.
        mAdView.setAdListener(this);
        layout.addView(mAdView);

    }

    public void writeToFile(String data) {
        try {
            Log.e("TRYING TO SAVE ERROR FILE","................");
            File file = new File(ERRORFILE);
            FileOutputStream fos;
            fos = new FileOutputStream(file);
            byte[] tresc = data.getBytes();
            fos.write(tresc);
            fos.flush();
            fos.close();
            Log.e("COMPLETE SAVE ERROR FILE","................");
        } catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void removeBanner(){
        if(mAdView!=null){
            layout.removeView(mAdView);
            mAdView = null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.slatedroid:
                startActivity(SLATEDROID_INTENT);
                return true;

            case R.id.androidpl:
                startActivity(ANDROIDPL_INTENT);
                return true;

            case R.id.donate:
                donateApp();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void changeHdmi() {
        Switch hdmiswitch = (Switch) findViewById(R.id.switch1);
        boolean hdmiState = hdmiswitch.isChecked();

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
                Log.w("IRBIS TWEAK", "Warning: lib(s) still exist !");
            }

            ExecuteRoot("cp " + hdmi + " " + cur_hdmi);
            ExecuteRoot("cp " + hwcm + " " + cur_comp);

            while (!cur_hdmi.exists() || !cur_comp.exists()) {
                Log.w("IRBIS TWEAK", "Warning: lib(s) still exist !");
            }

            ExecuteRoot("chmod 644 " + cur_hdmi);
            ExecuteRoot("chmod 644 " + cur_comp);

            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("hdmistate", true).commit();
//            RebootDevice();

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
                Log.w("IRBIS TWEAK", "Warning: lib(s) still exist !");
            }

            ExecuteRoot("cp " + hdmi + " " + cur_hdmi);
            ExecuteRoot("cp " + hwcm + " " + cur_comp);


            while (!cur_hdmi.exists() || !cur_comp.exists()) {
                Log.w("IRBIS TWEAK", "Warning: lib(s) still exist !");
            }

            ExecuteRoot("chmod 644 " + cur_hdmi);
            ExecuteRoot("chmod 644 " + cur_comp);

            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("hdmistate", false).commit();
//            RebootDevice();

        }
    }

    public void changeCpu() {
        Switch cpuswitch = (Switch) findViewById(R.id.switchCPU);
        boolean cpustate = cpuswitch.isChecked();

        if (cpustate) {
            File cur_cpu = new File(INIT_D + "cpuboost");
            if(cur_cpu.exists()) return;
            File cpufile = new File(CPU_BOOST_FILE);
            ExecuteRoot("cp " + cpufile + " " + cur_cpu);
            while (!cur_cpu.exists()) {
                Log.w("IRBIS TWEAK", "Warning: script not exist !");
            }
            ExecuteRoot("chmod 755 " + cur_cpu);
            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("cpustate", true).commit();
//            RebootDevice();

        } else {

            File cur_cpu = new File(INIT_D + "cpuboost");
            if (!cur_cpu.exists()) return;
            ExecuteRoot("rm -f " + cur_cpu);
            ExecuteRoot("chmod 666 /sys/devices/system/cpu/cpufreq/user/boost");
            ExecuteRoot("echo \"0\">/sys/devices/system/cpu/cpufreq/user/boost");
            ExecuteRoot("chmod 777 /sys/devices/system/cpu/cpufreq/user/boost");
            while (cur_cpu.exists()) {
                Log.w("IRBIS TWEAK", "Warning: script still exist !");
            }
            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("cpustate", false).commit();
//            RebootDevice();
        }
    }

    public void changeGpu() {
        Switch gpuswitch = (Switch) findViewById(R.id.switchGPU);
        boolean gpustate = gpuswitch.isChecked();

        if (gpustate) {
            File cur_gpu = new File(INIT_D + "gpuboost");
            if(cur_gpu.exists()) return;
            File gpufile = new File(GPU_BOOST_FILE);
            ExecuteRoot("cp " + gpufile + " " + cur_gpu);
            while (!cur_gpu.exists()) {
                Log.w("IRBIS TWEAK", "Warning: script not exist !");
            }
            ExecuteRoot("chmod 755 " + cur_gpu);
            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("gpustate", true).commit();
//            RebootDevice();

        } else {

            File cur_gpu = new File(INIT_D + "gpuboost");
            if (!cur_gpu.exists()) return;

            ExecuteRoot("rm -f " + cur_gpu);
            ExecuteRoot("chmod 666 /sys/devices/system/cpu/cpufreq/gpufreq/policy");
            ExecuteRoot("echo '0'>/sys/devices/system/cpu/cpufreq/gpufreq/policy");
            ExecuteRoot("chmod 777 /sys/devices/system/cpu/cpufreq/gpufreq/policy");

            while (cur_gpu.exists()) {
                Log.w("IRBIS TWEAK", "Warning: script still exist !");
            }

            SharedPreferences.Editor preferencesEditor = settings.edit();
            preferencesEditor.putBoolean("gpustate", false).commit();
//            RebootDevice();
        }
    }

    public void applyOnClick(View view) {
        RebootDevice();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    protected void onDestroy() {
        if (pd!=null) {
            pd.dismiss();
        }

        mManager.release();
        if(mAdView!=null) {
            mAdView.release();
        }
        super.onDestroy();
        finish();
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

                changeHdmi();
                changeCpu();
                changeGpu();
//debugCheck();
                Toast.makeText(MainActivity.this, getString(R.string.reboot_title), Toast.LENGTH_SHORT).show();
                CommandCapture command = new CommandCapture(0, "reboot");
                try{ RootTools.getShell(true).add(command).wait(); }
                catch (Exception e) { Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show(); }
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

            URLConnection ucon = url.openConnection();
            ucon.setConnectTimeout(5000);
            ucon.setReadTimeout(5000);

            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());

        } catch (Exception e) {
            Log.e("Download", "Error: " + e.toString());
            //displayDialog("ERROR", getString(R.string.download_error));
            networkError = DOWNLOAD_ERROR;
        }

    }

    public void donateApp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=pl.debuger.donate"));
        startActivity(intent);
    }

    @Override
    public void adClicked() {

    }

    @Override
    public void adClosed(Ad ad, boolean b) {

    }

    @Override
    public void adLoadSucceeded(Ad ad) {

    }

    @Override
    public void adShown(Ad ad, boolean b) {

    }

    @Override
    public void noAdFound() {

    }

    private class LoadViewTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(MainActivity.this);
            pd.setTitle(getString(R.string.progress_title));
            pd.setMessage(getString(R.string.progress_text));
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            File en_hd = new File(ENABLE_HDMI);
            File en_hw = new File(ENABLE_HWCOMPOSSER);
            File ds_hd = new File(DISABLE_HDMI);
            File ds_hw = new File(DISABLE_HWCOMPOSSER);
            File cp_bo = new File(CPU_BOOST_FILE);
            File gp_bo = new File(GPU_BOOST_FILE);
            File ir_dr = new File("/mnt/sdcard/IrbisTweak");
            File ir_en = new File("/mnt/sdcard/IrbisTweak/enable");
            File ir_ds = new File("/mnt/sdcard/IrbisTweak/disable");
            File bo_dr = new File("/mnt/sdcard/IrbisTweak/boost");

            if (!en_hd.exists() || !en_hw.exists() || !ds_hd.exists() || !ds_hw.exists() || !cp_bo.exists() || !gp_bo.exists()) {
                Log.e("IRBIS TWEAK", "Some req libs not found !!!");
                if (ir_dr.exists()) {
                    Log.e("IRBIS TWEAK", "Irbis storage directory exist - must be removed");
                    long startTime = System.currentTimeMillis();
                    ExecuteRoot("rm -r /mnt/sdcard/IrbisTweak");
                    while (ir_dr.exists() && ((System.currentTimeMillis() - startTime) / 1000) < 6) {
                        Log.e("IRBIS TWEAK", "Irbis storage directory still exist !!!");
                        //pass
                    }
                }

                ir_dr.mkdirs();
                ir_en.mkdirs();
                ir_ds.mkdirs();
                bo_dr.mkdirs();
                if (isNetworkConnected()) {

                    DownloadFromUrl("enable/hdmi.ATM702X.so", ENABLE_HDMI);
                    DownloadFromUrl("enable/hwcomposer.ATM702X.so", ENABLE_HWCOMPOSSER);
                    DownloadFromUrl("disable/hdmi.ATM702X.so", DISABLE_HDMI);
                    DownloadFromUrl("disable/hwcomposer.ATM702X.so", DISABLE_HWCOMPOSSER);
                    DownloadFromUrl("boost/cpuboost", CPU_BOOST_FILE);
                    DownloadFromUrl("boost/gpuboost", GPU_BOOST_FILE);

                } else {
                    Log.e("IRBIS TWEAK", "Error: Device not connected to network, download aborted");
                    //displayDialog("ERROR", getString(R.string.no_network), true);
                    networkError = NO_NETWORK;

                }
            }
            return networkError;
        }


        @Override
        protected void onPostExecute(Integer result) {
            if (pd!=null) {
                pd.dismiss();
            }

            if (networkError == NO_NETWORK) {
                displayDialog("ERROR", getString(R.string.no_network), true);
            } else if (networkError == DOWNLOAD_ERROR) {
                displayDialog("ERROR", getString(R.string.download_error), true);
            }

            if (!settings.getBoolean("notify2", false)) {
                displayDialog(getString(R.string.app_name), getString(R.string.update_notify));
                SharedPreferences.Editor prefEdit = settings.edit();
                prefEdit.putBoolean("notify2", true).commit();
            }

            File errfile = new File(ERRORFILE);
            if (errfile.exists()) {
                try {
                    sendErrorReport(readFromFile());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private String readFromFile() throws Exception {

        File fl = new File(ERRORFILE);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }
    public String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\r\n");
        }
        return sb.toString();
    }
    protected void sendErrorReportMail(String report) {
        Log.e("Irbis Tweaks", "Sending error report");

        String[] TO = {"root@debuger.pl"};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Irbis Tweaks - Crash Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, report);
        new File(ERRORFILE).delete();
        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
            Log.e("Finished sending email...", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "No email client installed to send crash report", Toast.LENGTH_SHORT).show();
        }
    }
    void sendErrorReport(final String report){
        final Dialog alertdialog = new Dialog(MainActivity.this);
        alertdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertdialog.setContentView(R.layout.db_question);
        TextView alertTitle = (TextView) alertdialog.findViewById(R.id.textView2);
        alertTitle.setText(getString(R.string.app_name));
        TextView alertText = (TextView) alertdialog.findViewById(R.id.textView);
        alertText.setText(getString(R.string.error_msg));
        Button buttonOne = (Button) alertdialog.findViewById(R.id.button);
        buttonOne.setText(getString(R.string.no));
        Button buttonTwo = (Button) alertdialog.findViewById(R.id.button2);
        buttonTwo.setText(getString(R.string.yes));
        alertText.setTextColor(Color.WHITE);
        buttonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(ERRORFILE).delete();
                alertdialog.dismiss();
            }
        });
        buttonTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendErrorReportMail(report);
                alertdialog.dismiss();
            }
        });

        alertdialog.show();

    }

}
