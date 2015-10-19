package whi.ucla.erlab.gimbal;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/* Background IntentService that scans for UCLA beacons using UCLA beacon UUID (in constants).
 * Saves the timestamp, beacon ID, RSSI in a CSV file. Writes a new CSV file every hour. File
 * directory is declared in Constants */

/* Note on RawBuffer size. Experimental evidence shows that current file size is around 3.1 MB for
 * one hour of data. If buffer is flushed every 60 seconds, then the buffer size should be ::
 *
 *                  ( 3.1 * 1024 * 1024 ) bytes
 *                  ---------------------        =   54,177 (bytes/min)  = 53 kB
 *                           60           (second)
 *
 * To account for worst-case performance, this has been rounded upto 128 kB. Experimentation shows
 * that the OS controls flushing based on it's own parameters so playing with this value may have no
 * significant effect on performance.
 */
public class BeaconScan extends Service {

    public static final String BLESERVICE_TAG = BeaconScan.class.getSimpleName();
    private static final int RawBufferSize = (64*1024);      // Buffer size is 32 kB
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static final boolean append = true;
    private static boolean isScanning = false;
    public static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothLeScanner mBluetoothLeScanner;
    private  BufferedWriter Raw_log = null;
    private static ArrayList<ScanFilter> filters;
    private static ScanSettings settings;


    /* Preliminary check to ensure that device Bluetooth is enabled */
    public BeaconScan(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter!= null && !mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();


    }


/* Executed when service is closed. Stops the beacon scan, clears out all buffers into the files */
    @Override
    public void onDestroy() {
        Constants.BeaconScanRunning = false;
        stopBLEscan();
        closeLogFiles();
       // Log.d(BLESERVICE_TAG, "BeaconScan Closed");
        Toast.makeText(Gimbal.context, "Beacon Off",Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /* Runs once when an intent to start the BeaconScan service is passed. Ensures that there is only
     * one instance running, creates the log files, and begins BLE beacon scan using setupBLE. One can
     * choose to run this function less frequently at the cost of inaccuracies in writing data to the
     * correct files
    */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Constants.BeaconScanRunning) {
            // Log.d(BLESERVICE_TAG, "Another Instance of BeaconScan already running, stopping");
            stopSelf();
            return 0;
        }

        Constants.BeaconScanRunning = true;
        Toast.makeText(Gimbal.context, "Beacon On", Toast.LENGTH_SHORT).show();

        setupBLE();

        return START_STICKY;
    }

/* Build Bluetooth LE Scan settings using UCLA_ID & beacon specific data. Set up and build filter.
 * Scan in HIGH_POWER_MODE same as LOW_LATENCY_MODE in order to get maximum resolution. Then set up
 * a scheduler that runs @ 50% Duty Cycle for a time period T = 2000ms, configurable to save battery
 * */

public void setupBLE(){

    ScanFilter beaconFilter = new ScanFilter.Builder()
      .setManufacturerData(Constants.UCLA_manID, Constants.UCLA_manufac_data, Constants.Beacon_Mask)
      .build();

    filters = new ArrayList<ScanFilter>();
    filters.add(beaconFilter);

    settings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();

    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

       /*  - 10% Duty Cycle - T = 2 second */
      //  DutyCycle(200,1800);
startBLEscan();
   // DutyCycle(2000,0);
    /*  - 30% Duty Cycle - T = 2 second */
  // DutyCycle(600,1400);

}
    public  void startBLEscan(){
        if(isScanning)
            return;
       // Log.d(BLESERVICE_TAG, "ON");
     if(mBluetoothLeScanner==null)
            return;

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
         isScanning = true;

    }

    /* Stops the BLE Scan and returns. */
    public void stopBLEscan(){
        if(!isScanning)
        return;
       // Log.d(BLESERVICE_TAG, "OFF");
        mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
        mBluetoothLeScanner.stopScan(mScanCallback);
        mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
        isScanning = false;
    }

/* Called every time a UCLA beacon is detected. For each beacon, scans the results and writes to
* file only if the RSSI value is lesser than -25. This is so that beacons that are relatively closer
* do not magnify and distort the overall readings. In general, if the beacon is extremely close to
* the watch, the RSSI may even become positive. */

     private  ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
          long timestamp = System.currentTimeMillis() + Constants.SERVER_TIME_OFFSET;
          int RSSI = result.getRssi();

          if(RSSI>=-25)
             return;

          if(Constants.AlarmBell_1 && (Constants.TURBO == 0))
                resetFiles();

            if(result.getScanRecord().getManufacturerSpecificData() != null)
            Log.d("DATA :  ", result.getScanRecord().getManufacturerSpecificData()+"");

          byte[] bytes = {result.getScanRecord().getManufacturerSpecificData().valueAt(0)[20],
                          result.getScanRecord().getManufacturerSpecificData().valueAt(0)[21]};

            String beaconID = bytesToHexOptimized(bytes);

            if(hex2decimal(beaconID)==Constants.SEARCH_BEACON_ID)
            {
                Constants.rssiArray[0][Constants.iterator] = timestamp;
                Constants.rssiArray[1][Constants.iterator] = RSSI;

                Constants.iterator+=1;
            }

            String data = timestamp+","+beaconID+","+RSSI+","+Constants.TURBO+"\n";

            try {
                  Raw_log.write(data);
            } catch (IOException e) {

            }
            catch (NullPointerException e) {
                resetFiles();
        }
     }
        @Override
        public void onScanFailed(int errorCode) {
            isScanning = false;
         //   Log.w(BLESERVICE_TAG, "LE Scan Failed: "+errorCode);
        }
    };

    private static void updateScreen(int RSSI,long timestamp){

    }
    /**
     * Convert raw data to a string in hexadecimal values. Optimized to work for UCLA beacon strings
     * @param bytes  Raw data buffer
     * @return String containing the hex representation
     *
     *  hexChars[j * 2] = (char) hexArray[v >>> 4];
        hexChars[j * 2 + 1] =  (char) hexArray[v & 0x0F];
     */
    public static String bytesToHex(byte[] bytes) {
        final int StringSize = 22;
        char[] hexChars = new char[StringSize * 2];
        for ( int j = 20; j < StringSize; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] =  hexArray[v & 0x0F];
    }
        return new String(new char[]{hexChars[41], hexChars[42], hexChars[43]});
    }
    public static int hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
    public static String bytesToHexOptimized(byte[] bytes) {
        char[] hexChars = new char[3];
        int v = bytes[0] & 0xFF;
        //hexChars[0] = hexArray[v >>> 4];
        hexChars[0] = hexArray[v & 0x0F];
        v = bytes[1] & 0xFF;
        hexChars[1] = hexArray[v >>> 4];
        hexChars[2] = hexArray[v & 0x0F];
        return new String(new char[]{hexChars[0], hexChars[1], hexChars[2]});
    }

    /**
     * Create file to log data, the Output buffer is initialized and file header is written
     * to write data in files. for each new file, writes the filename as the first line. This is
     * needed for communicating the filename to the phone.
     */

    private  void resetFiles(){
        Constants.AlarmBell_1 = false;
        closeLogFiles();
        WriteNewFiles();
    }
    private void WriteNewFiles() {

        File BeaconFile = Constants.getUniqueFile(Constants.beacon_directory_file,false);

        try {
            Raw_log = new BufferedWriter(new FileWriter(BeaconFile,append),RawBufferSize);
            } catch (IOException e) {

        }
    }

/* If a BufferedWriter is open then close it. Ensures that the currentFile is not being written into
 * anymore. If currentFile is empty then there is no need to close it. Moreover, this may create a
 * null pointer exception.
*/
private  void closeLogFiles(){
    try {
        Raw_log.flush();
        Raw_log.close();
    } catch (IOException e) {/*  e.printStackTrace(); */}
    catch (NullPointerException e){

    }
 }

private void DutyCycle(int ON_TIME,int OFF_TIME){

    Timer t1 = new Timer();
    Timer t2 = new Timer();

    t1.scheduleAtFixedRate(new TimerTask() {

        @Override
        public void run() {
            if (Constants.BeaconScanRunning)
                startBLEscan();
            else
                this.cancel();
        }
    }, 0, ON_TIME+OFF_TIME);

    t2.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            if(Constants.TURBO == 0)
                stopBLEscan();

            if (!Constants.BeaconScanRunning)
               this.cancel();
        }
    }, ON_TIME, ON_TIME+OFF_TIME);
 }
}
/*if (!Constants.BeaconScanRunning)
                this.cancel();

            else if(Constants.TURBO == 0)
                 stopBLEscan();
        */
