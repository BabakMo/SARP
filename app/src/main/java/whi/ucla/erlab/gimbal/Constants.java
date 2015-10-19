package whi.ucla.erlab.gimbal;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by BabakMo on 3/3/15.
 *
 * All Constants and common functions are declared here.
 */
public class Constants {

    public static final boolean DEBUG = true;

    public static final File root = android.os.Environment.getExternalStorageDirectory();
    public static final String rootDirectory = root.getAbsolutePath();
    public static final String beacon_directory = rootDirectory+"/Gimbal/Beacons";
    public static final String test_directory = rootDirectory+"/Gimbal/Tests";
    public static final String battery_directory = rootDirectory+"/Gimbal/Battery";
    public static final String gyro_directory = rootDirectory+"/Gimbal/Gyro";
    public static final String compressed_directory = rootDirectory+"/Gimbal/Compressed";
    public static final String debug_directory = rootDirectory+"/Gimbal/Debug";
    public static final File beacon_directory_file = new File(beacon_directory);
    public static final File test_directory_file = new File(test_directory);
    public static final File battery_directory_file = new File(battery_directory);
    public static final File gyro_directory_file = new File(gyro_directory);
    public static final File compressed_directory_file = new File(compressed_directory);
    public static final File debug_directory_file = new File(debug_directory);
    private static final File TIME_OFFSET_FILE = new File(Constants.debug_directory_file, "timeoffset.txt");

    /* Create Folder Structure if it does not already exist */

    public static boolean initDB(){
        return (battery_directory_file.mkdirs() || beacon_directory_file.mkdirs()||
                gyro_directory_file.mkdirs()|| compressed_directory_file.mkdirs() ||
                test_directory_file.mkdirs() ||
                debug_directory_file.mkdirs());
    }

    public static int SEARCH_BEACON_ID = 8;

    public static int TURBO = 0;
    public static final long[][] rssiArray = new long[2][128];
    public static int iterator = 0;
    public static boolean DEVICE_LOCK = false;
    public static boolean AlarmBell_1 = true;
    public static boolean AlarmBell_2 = true;
    public static boolean BeaconScanRunning = false;
    public static boolean UploadServiceRunning = false;
    public static boolean GyroServiceRunning = false;
    public static boolean HomeScreenRunning = false;

    /* Read last file saved servertime offset */
    public static long SERVER_TIME_OFFSET = readOffsetfromFile(TIME_OFFSET_FILE);

/*{1,  1,   1,  1, 1, 1,   1,  1,  1,   1,   1,    1, 1, 1,   1,   1,   1,  1, 0, 0, 0, 0,    1}*/;

    public static final long[] vibration_pattern = {0, 120, 100, 120, 600, 120, 100, 120, 600};

    public static final byte[] Beacon_Mask =       {2, 21, -21, 19, 0, 0, 127, -6, 17, -28, -68, -109, 0, 2, -91, -43, -59, 27, -1, -1, 0, 0, 0};
    public static final byte[] UCLA_manufac_data = {2, 21, -21, 19, 0, 0, 127, -6, 17, -28, -68, -109, 0, 2, -91, -43, -59, 27, 0, 1, 0, 0, 0};

    public static final byte[] Beacon_Mask2 =       {0,  0,   0,  0, 0, 0,   0,  0,  0,   0,   0,    0, 0, 0,   0,   0,   0,  0, 0, 0, 0, 0,    0};
    public static final byte[] Beacon_Mask3 =       {0,  0,   0,  0, 0, 0,   0,  0,  0,   0,   0,    0, 0, 0,   0,   0,   0,  0, 0, 0, -1, -1,    -1};
    public static final byte[] UCLA_manufac_data2 = {2, 21, -21, 19, 0, 0, 127, -6, 17, -28, -68, -109, 0, 2, -91, -43, -59, 27, 0, 0, 0, 8, -62};

    public static final int UCLA_manID = 76;

    public static WifiConfiguration configWiFi(String sSSID,String sPassphrase){

    WifiConfiguration new_config = new WifiConfiguration();
    new_config.SSID = "\"".concat(sSSID).concat("\"");
    new_config.status = WifiConfiguration.Status.DISABLED;
    new_config.priority = 400;

    new_config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    new_config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
    new_config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
    new_config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
    new_config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
    new_config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    new_config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
    new_config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    new_config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

    new_config.preSharedKey = "\"".concat(sPassphrase).concat("\"");

    return new_config;
}

    public static  WifiConfiguration configWiFi(String sSSID){
        WifiConfiguration new_config = new WifiConfiguration();
        new_config.SSID = "\"".concat(sSSID).concat("\"");
        new_config.status = WifiConfiguration.Status.DISABLED;
        new_config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        new_config.priority = 400;
        return new_config;
    }

    public static final SimpleDateFormat FilenameFormat = new SimpleDateFormat("'_'yyyy-MM-dd-HH", Locale.US);


    /**
     * Create a directory if it does not exist
     * @param path Relative path (respect to SD card)
     * @return true if dir is created false otherwise
     */
    public static boolean createDirIfNotExists(String path) {
        boolean ret = true;
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
           //     Log.e("Constants", "Problem creating Image folder");
                ret = false;
            }
        }
        return ret;
    }

    public static File getUniqueFile(File srcDirectory,boolean appendLast) {

        //          MAC_YYYY-MM-DD-HH.runN.format.csv
        String MAC = BluetoothAdapter.getDefaultAdapter().getAddress().replace(":", "");
        String TimeStamp = Constants.FilenameFormat.format(new Date());

        String extension = srcDirectory.getPath().contains("Gyro") ? ".gyro.csv" : ".csv";
        extension = srcDirectory.getPath().contains("Battery") ? ".battery.csv" : extension;

        int numberFiles = getListFileswithExtension(srcDirectory,TimeStamp).size();

        int fileNumber = appendLast ? numberFiles : (numberFiles + 1);

        String runN = ".run" + ((numberFiles > 0) ? String.valueOf(fileNumber) : "1" );

        return new File(srcDirectory,MAC + TimeStamp + runN + extension);
    }
    private static long readOffsetfromFile(File srcFile){

        long offset = 0;

        if(DEBUG) Log.d("ReadOffset from ", srcFile.getAbsolutePath());

        if (!srcFile.exists())
            return 0;
        try {
            BufferedReader lineRead = new BufferedReader(new FileReader(srcFile));
            String parsed = lineRead.readLine();
           if(DEBUG) Log.d("ReadOffset Parsed", parsed);
            offset = Long.valueOf(parsed);
          } catch (Exception e) {
            //
        }
        if(DEBUG) Log.d("ReadOffset Long ", String.valueOf(offset));
        return offset;
    }
    public static boolean updateOffset(long offset){

        try {
            BufferedWriter offset_writer = new BufferedWriter(new FileWriter(TIME_OFFSET_FILE, false), 64);
            offset_writer.write(String.valueOf(offset));
            offset_writer.close();
            if(DEBUG) Log.d("WriteOffset", "OFFSET WRITTEN");
        } catch (Exception e) {
            return false;
        }

        SERVER_TIME_OFFSET = offset;

        return true;
    }
    public static int getBatteryLevel() {
        Intent batteryIntent = Gimbal.context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (batteryIntent == null)
            return -1 ;

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1)
            return -1;

        return 100*level/scale;
    }

    public static int getDiskSpace(){
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long free = statFs.getAvailableBytes();
        long total = statFs.getTotalBytes();
        long busy = 100*free/total;

        return (int)busy;
    }

    public static int getPowerState(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = Gimbal.context.registerReceiver(null, ifilter);

        if(batteryStatus == null)
            return -1;

        return batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    }

    public static String getWatchState(){

       boolean plugged_in = !(getPowerState() < 1);
       boolean servicesRunning = Constants.BeaconScanRunning & Constants.GyroServiceRunning;
       boolean uploadPending = !getListFiles(Constants.compressed_directory_file).isEmpty();

        if (!plugged_in & (getBatteryLevel() < 6))
            return "LOW%20BATT";

       return  plugged_in ? (uploadPending ? "Uploading" : "Charging") : (servicesRunning ? "Operational" : "ERROR");
    }

    public static List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                inFiles.addAll(getListFiles(file));
            else
                inFiles.add(file);
        }
        return inFiles;
    }
    public static List<File> getListFiles(File parentDir,String ignore_extension) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                inFiles.addAll(getListFiles(file,ignore_extension));
            else if (!file.getPath().contains(ignore_extension))
                inFiles.add(file);
        }
        return inFiles;
    }
    public static List<File> getListFileswithExtension(File parentDir,String extension) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                inFiles.addAll(getListFileswithExtension(file,extension));
            else if (file.getPath().contains(extension))
                inFiles.add(file);
        }
        return inFiles;
    }
}
