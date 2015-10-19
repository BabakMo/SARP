package whi.ucla.erlab.gimbal;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;


//import android.util.Log;


/**
 * Created by BabakMo on 3/3/15.
 *
 * Scans the given directory to invoke BluetoothService for each file in directory. BluetoothService
 * then uploads each file at a time.
 * To Do :: Implement a function to stop transfer in the middle successfully.
 * To Do ::
 */
public class UploadService extends Service {
    private static final String BLESERVICE_TAG = UploadService.class.getSimpleName();
    private static Timer syncTimer = null;
    private static final int PERIOD = 60 * 1000; //Sync every minute;
    private static final int INIT_DELAY =  1000;
    private static ConnectivityManager Connection = null;
    private static WifiManager wifiManager = null;
    private static WifiManager.WifiLock lock = null;
    private static StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

    /* To close transfers in the middle, onHandleIntent sets FILE_NUMBER to -1. This breaks the upload
    * for loop. All remaining files stay in the source directory. All uploaded files are transferred to
    * a compressed directory. */
    @Override
    public void onCreate() {
        super.onCreate();
        Connection = (ConnectivityManager)Gimbal.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager)Gimbal.context.getSystemService(Context.WIFI_SERVICE);
        lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LockTag");

        if(Constants.DEBUG) Log.d(BLESERVICE_TAG, " Created !");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if(syncTimer!=null)
            syncTimer.cancel();

        stopWiFi();


        Constants.UploadServiceRunning = false;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Constants.UploadServiceRunning) {
            if(Constants.DEBUG) Log.d(BLESERVICE_TAG, "Service already running, stopping");
            stopSelf();
            return 0;
        }
        if(Constants.DEBUG) Log.d(BLESERVICE_TAG, " started !");
        Constants.UploadServiceRunning = true;

        if(Constants.getPowerState() == 0)
        {

            StrictMode.setThreadPolicy(policy);

            connectWiFi();
            statusUpdate();
            stopWiFi();

            Constants.UploadServiceRunning = false;
            stopSelf();
            return 0;
        }

        syncTimer = new Timer();
        syncTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                if (Constants.getPowerState() == 0) {
                    stopWiFi();
                    this.cancel();
                    if(Constants.DEBUG) Log.d(BLESERVICE_TAG, "Not Connected to Power. Closing WiFi");
                } else
                    startSync();
            }
        }, INIT_DELAY, PERIOD);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static void startSync(){

        compressPending();

        StrictMode.setThreadPolicy(policy);
        if(connectWiFi())
        {
            statusUpdate();
            transferFiles();
        }
    }
    private static boolean connectWiFi(){

        lock.acquire();
        WifiConfiguration HOME_NETWORK = Constants.configWiFi("Cave of Wonders","diamondintherough");
        WifiConfiguration UCLA_NETWORK = Constants.configWiFi("UCLA_WEB");
        WifiConfiguration ERLAB_NETWORK = Constants.configWiFi("watchWiFi","Gimbal2015");

        int networkID = wifiManager.addNetwork(UCLA_NETWORK);


        if(Constants.DEBUG) Log.d(BLESERVICE_TAG, " connecting WiFi ....");
        wifiManager.setWifiEnabled(true);

        wifiManager.enableNetwork(networkID,true);
        wifiManager.reassociate();

     // Connect to preferred network using Wifi builder. Preferred network is last connected network

        while(Connection.getActiveNetworkInfo() == null)
            SystemClock.sleep(100);

        while(!Connection.getActiveNetworkInfo().isConnected() || wifiManager.getWifiState() < 3)
            SystemClock.sleep(100);

        if(Constants.DEBUG) Log.d(BLESERVICE_TAG, "Connected");
        return true;
    }

    private static void compressPending() {

        Kompressor.kompressFiles(Constants.beacon_directory_file, ".gz");
        Kompressor.kompressFiles(Constants.gyro_directory_file, ".gz");

    }

    private static long fetchServerTime() {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://berkleymonitor.com/time");

        try {
            Constants.updateOffset(Long.valueOf(readStream(httpclient.execute(httpget).getEntity().getContent())) - System.currentTimeMillis());
        } catch (Exception e) {
           //
        }
        return System.currentTimeMillis() + Constants.SERVER_TIME_OFFSET;
    }

    private static void statusUpdate(){

        DefaultHttpClient httpclient = new DefaultHttpClient();

        String health2 = "http://berkleymonitor.com/update/watch/node/Node123/"
                + BluetoothAdapter.getDefaultAdapter().getAddress().replace(":","_")+"/"+
                fetchServerTime() + "/" + Constants.getBatteryLevel() + "/" +
                Constants.getDiskSpace();

        String health = "http://berkleymonitor.com/update/watch/node/Node123/"
                + BluetoothAdapter.getDefaultAdapter().getAddress().replace(":","_")+"/"+
                fetchServerTime() + "/" + Constants.getBatteryLevel() + "/" +
                Constants.getDiskSpace()+"/"+Constants.getWatchState();

        HttpGet httpget = new HttpGet(health);
        try {
            if(readStream(httpclient.execute(httpget).getEntity().getContent()).equals("OK"))
                if(Constants.DEBUG) Log.d("Status","Updated");
        } catch (Exception e) {
            if(Constants.DEBUG) Log.d("Status", "Not Updated");
        }
    }

    private static boolean archive(File inputFile){

        String fileName = inputFile.getName();
        File destination = new File(Constants.beacon_directory+"/"+fileName);

        if(fileName.contains("gyro.csv.gz"))
            destination = new File(Constants.gyro_directory+"/"+fileName);

        return inputFile.renameTo(destination);
    }

    private static void transferFiles(){

        List<File> compressedFiles = Constants.getListFiles(Constants.compressed_directory_file);
        for(File file : compressedFiles)
            if(upload(file))
                archive(file);
    }


    private static boolean upload(File inputFile) {

        if(Constants.DEBUG) Log.d("Status","Uploading ... "+inputFile.getName());
        DefaultHttpClient httpclient = new DefaultHttpClient();


        HttpPost httpost = new HttpPost("http://www.berkleymonitor.com:8080/upload.php");

        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart("password", new StringBody("watch2015", ContentType.TEXT_PLAIN));
        reqEntity.addPart("file",new FileBody(inputFile));

        httpost.setEntity(reqEntity);

        String response = "";
        try {
            response = readStream(httpclient.execute(httpost).getEntity().getContent());
            if(Constants.DEBUG) Log.d("UPLOAD RESULT ",response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response.equalsIgnoreCase("OK");
    }

    private static String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private static boolean stopWiFi(){

        wifiManager.disconnect();
        wifiManager.setWifiEnabled(false);

        while(wifiManager.getWifiState() > 2)
            SystemClock.sleep(100);

        if(lock.isHeld())
            lock.release();

        if(Constants.DEBUG) Log.d(BLESERVICE_TAG, "Stopped WiFi");
        return true;
    }
}


