package whi.ucla.erlab.gimbal;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Babak on 4/7/2015. This is the base class for this entire App. Maintains the app state
 * during the lifecycle of the application. All BroadcastReceivers call functions from this class.
 *
 * BroadcastReceivers form the inputs for this app. They have been summarized below ::
 *
 *      * BootReceiver (recevies power state)
 *      * ScreenReceiver (receives power button toggling events)
 *      * AlarmReceiver (recevies hourly broadcasts to change file name)
 *
 * The Outputs are Services and Activities that run based on broadcasts received. These are ::
 *
 *      * GyroScan      - Gyro & Accelerometer scan @ 10Hz
 *      * BeaconScan    - BLE Scan in Intermittent HIGH_POWER_MODE
 *      * UploadService - uploads data to BerkleyMonitor server over WiFi Connection
 *
 *      * HomeScreen    - State of app when scans are running (mostly for testing)
 *      * BeaconScreen  - For testing Only. Beacon Scan statistics
 *      * UploadScreen  - Status of uploads to phone
 *      * ErrorScreen   - Time error or System failure.
 *
 */
public class Gimbal extends Application {
    public static Gimbal singleton;
    public static Context context;
    private static Intent WiFi_UploadService = null, BeaconScan = null, GyroService = null,    HomeScreen = null;
    private BroadcastReceiver mPowerKeyReceiver = null;
    private static Timer AppClock = null;/*,BattClock=null*/
    private static IntentFilter theFilter;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        context = getApplicationContext();


        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, 0);

        theFilter = new IntentFilter();
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mPowerKeyReceiver = new ScreenReceiver();
        Gimbal.context.registerReceiver(mPowerKeyReceiver, theFilter);

        resumeState();
        //Gimbal.DataCollectionMode();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();

    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        Gimbal.context.unregisterReceiver(mPowerKeyReceiver);
    }



    public static void DataUploadMode() {
        StopLogger();
        StopScan();
        OpenUI();
        StartUploadService();
    }



    public static void DataCollectionMode() {
        StopUploadService();
        StartLogger();
        OpenUI();
        StartScan();
    }

    public static void ShutDownMode() {
        StopUploadService();
        StopScan();
        StopLogger();
        CloseUI();
        StartUploadService();
    }
    public static void resumeState(){
        if(Constants.getPowerState()==0)
        {

            return;
        }
            Gimbal.DataUploadMode();
    }


    private static void runAppClock() {
        final Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        final int MILLISECONDS_IN_ONE_HOUR =  60 * 60 * 1000;
        final int MILLISECONDS_SINCE_LAST_FULL_HOUR = (rightNow.get(Calendar.MINUTE)*60*1000)+(rightNow.get(Calendar.SECOND)*1000)+(rightNow.get(Calendar.MILLISECOND));
        final int MILLISECONDS_TO_NEXT_HOUR = MILLISECONDS_IN_ONE_HOUR - MILLISECONDS_SINCE_LAST_FULL_HOUR;

        AppClock = new Timer();
        AppClock.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                Constants.AlarmBell_1 = Constants.AlarmBell_2 = true;
                try {
                    //Constants.getUniqueFile(Constants.battery_directory_file, true)
                   BufferedWriter battery_log = new BufferedWriter(new FileWriter(new File(Constants.battery_directory_file,"battery_stat_WiFi_optimized.txt"), true), 64);
                   battery_log.write((System.currentTimeMillis() + Constants.SERVER_TIME_OFFSET)+ "," + Constants.getBatteryLevel() + "," + Constants.TURBO);
                   battery_log.newLine();
                   battery_log.close();
                    StartUploadService();
                } catch (IOException e) {
                    //
                }
                }
        }, MILLISECONDS_TO_NEXT_HOUR, MILLISECONDS_IN_ONE_HOUR); //  replace with   ;
    }


    private static void StopLogger() {

        if(AppClock!=null)
        {
            AppClock.cancel();
            AppClock = null;
        }

     /*   if(BattClock!=null)
        {
            BattClock.cancel();
            BattClock = null;
        }*/
    }

    private static void StartLogger() {

    Constants.initDB();

        if(AppClock == null)
            runAppClock();


        /* if(AppClock!=null || BattClock !=null)
        return;



        BattClock = new Timer();
        BattClock.schedule(new TimerTask() {
            @Override
            public void run() {

            }
        }, 0, 15 * 60 * 1000);*/

    }


    public static void OpenUI(){
        HomeScreen = new Intent(Gimbal.context,HomeScreen.class);
        HomeScreen.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Gimbal.context.startActivity(HomeScreen);

    }
    private static void CloseUI(){

    }
    private static void StartScan(){
        if(!Constants.BeaconScanRunning)
        {
           BeaconScan = new Intent(Gimbal.context,BeaconScan.class);
           Gimbal.context.startService(BeaconScan);
        }
        if(!Constants.GyroServiceRunning)
        {
            GyroService = new Intent(Gimbal.context,GyroScan.class);
            Gimbal.context.startService(GyroService);
        }
    }

    public static void StartUploadService() {
        if (!Constants.UploadServiceRunning) {
            WiFi_UploadService = new Intent(Gimbal.context, UploadService.class);
            Gimbal.context.startService(WiFi_UploadService);
        }
    }
    private static void StopUploadService(){
        if (Constants.UploadServiceRunning) {
            WiFi_UploadService = new Intent(Gimbal.context, UploadService.class);
            Gimbal.context.stopService(WiFi_UploadService);
        }
    }

    private static void StopScan() {
        if (Constants.BeaconScanRunning) {
            BeaconScan = new Intent(Gimbal.context, BeaconScan.class);
            Gimbal.context.stopService(BeaconScan);
        }
        if (Constants.GyroServiceRunning) {
            GyroService = new Intent(Gimbal.context, GyroScan.class);
            Gimbal.context.stopService(GyroService);
        }
    }


}
