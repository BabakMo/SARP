package whi.ucla.erlab.gimbal;

/**
 * Created by BabakMo on 3/3/15.
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;

public class GyroScan extends Service implements SensorEventListener {

    private static final String TAG = GyroScan.class.getSimpleName();
    private static final int SAMPLE_RATE = 10;
    private static final int sSensorDelay = 1000000 / SAMPLE_RATE;
    private static final int RawBufferSize = 64 * 1024;      // Buffer size is 64 kB
    private static final int VIBRATION_DELAY_SEC = 300 * 1000;
    private  BufferedWriter mRawLog;
    private  SensorManager mSensorManager;
    private  Sensor mAccelerometer;
    private  Sensor mGyroscope;
    private  float[] mLastGyrReadings = new float[3];
    private PowerManager.WakeLock mWakeLock = null;
    private static final char[] comma = ",".toCharArray();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Constants.GyroServiceRunning)
            stopSelf();
        // Create the log directory
        //initializing the sensors
        // Register this class as a listener for the sensors we want
        // Prevent the CPU from sleeping
        Constants.gyro_directory_file.mkdirs();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GimbalServices");
    }
     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         if (Constants.GyroServiceRunning)
             stopSelf();
         mSensorManager.registerListener(this, mAccelerometer, sSensorDelay);
         mSensorManager.registerListener(this, mGyroscope, sSensorDelay);

         Constants.GyroServiceRunning = true;
         //Toast.makeText(Gimbal.context, "Scanning",Toast.LENGTH_SHORT).show();
         mWakeLock.acquire();


        @SuppressWarnings("deprecation")
         Notification note = new Notification( 0, null, (System.currentTimeMillis() + Constants.SERVER_TIME_OFFSET) );
         note.flags |= Notification.FLAG_NO_CLEAR;
         startForeground( 42, note );
     return START_STICKY;
 }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop gathering sensor data
        mSensorManager.flush(this);
        mSensorManager.unregisterListener(this);

        if (mWakeLock != null) {
            mWakeLock.release();
        }
        closeLogFile();
        Constants.GyroServiceRunning = false;
        //Toast.makeText(Gimbal.context, "Stop Scan",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Write out data every hour and start over
       if (Constants.AlarmBell_2)
            resetFiles();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            try {
                mRawLog.write(String.valueOf((System.currentTimeMillis() + Constants.SERVER_TIME_OFFSET)));
                mRawLog.write(comma);
                mRawLog.write(String.valueOf(event.values[0]));
                mRawLog.write(comma);
                mRawLog.write(String.valueOf(event.values[1]));
                mRawLog.write(comma);
                mRawLog.write(String.valueOf(event.values[2]));
                mRawLog.write(comma);
                mRawLog.write(String.valueOf(mLastGyrReadings[0]));
                mRawLog.write(comma);
                mRawLog.write(String.valueOf(mLastGyrReadings[1]));
                mRawLog.write(comma);
                mRawLog.write(String.valueOf(mLastGyrReadings[2]));
                mRawLog.write(comma);
                mRawLog.write("0"); /*String.valueOf(Constants.bodyPosition));*/
                mRawLog.newLine();
                /*
                              event.values[0]+","+
                              event.values[1]+","+
                              event.values[2]+","+
                              mLastGyrReadings[0]+","+
                              mLastGyrReadings[1]+","+
                              mLastGyrReadings[2]+","+
                              Constants.bodyPosition+"\n");*/
            } catch (IOException e) {
               // e.printStackTrace();
            }catch(NullPointerException e){
                resetFiles();
            }

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            mLastGyrReadings[0] = event.values[0];
            mLastGyrReadings[1] = event.values[1];
            mLastGyrReadings[2] = event.values[2];
        }

    }

    private  void resetFiles(){
        Constants.AlarmBell_2 = false;
        closeLogFile();
        writeNewFile();
    }
    private void writeNewFile() {

        File gyroFile = Constants.getUniqueFile(Constants.gyro_directory_file,false);
        try {
            mRawLog = new BufferedWriter(new FileWriter(gyroFile, true),RawBufferSize);
        } catch (IOException e) {
        }
    }
private  void closeLogFile(){
    try {
        mRawLog.flush();
        mRawLog.close();
    } catch (IOException e) {
       // e.printStackTrace();
    }
    catch (NullPointerException e) {
        // e.printStackTrace();
    }
}
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No implementation necessary
    }

    @Override
    public IBinder onBind(Intent intent) {
        // No implementation necessary
        return null;
    }
}
