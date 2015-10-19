package whi.ucla.erlab.gimbal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

//import android.util.Log;


public class HomeScreen extends Activity {

    public TextView RSSI = null;
    public TextView RSSI_AVG = null;
    public TextView RSSI_STDEV = null;
    public TextView LAT_AVG = null;
    public TextView LAT_STDEV = null;
    public Button button = null;
    public EditText field = null;
    private Timer timer = new Timer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

     //   if(Constants.DEVICE_LOCK)
      //  { ERROR_UI(); return;}

     //   if(Constants.GyroServiceRunning)
        LOGGING_UI();
        Constants.HomeScreenRunning = true;

        //if(Constants.UploadServiceRunning)
           // UPLOAD_UI();

      // else
       //     LOCK_UI();
//
    }


private void LOGGING_UI(){
    setContentView(R.layout.activity_rssi_screen);
    final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
    stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
        @Override
        public void onLayoutInflated(WatchViewStub stub) {
            RSSI = (TextView)findViewById(R.id.RSSI);
            RSSI_AVG = (TextView)findViewById(R.id.RSSI_AVG);
            RSSI_STDEV = (TextView)findViewById(R.id.RSSI_STDEV);
            LAT_AVG = (TextView)findViewById(R.id.LAT_AVG);
            LAT_STDEV = (TextView)findViewById(R.id.LAT_STDEV);
           /* button= (Button) findViewById(R.id.button);
            field= (EditText) findViewById(R.id.field);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                  //  Gimbal.DataCollectionMode();
                   String val = field.getText().toString();
                    if(!val.equals(""))
                    {int number = Integer.valueOf(val);
                    if(number>1 && number <155)
                        Constants.WALDO = number;}
                }
            });*/
            timer.schedule(new MyTimerTask(), 0,2000);

        }});
    }

    private class MyTimerTask extends TimerTask{

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (RSSI != null) {
                        long[] metric = metrics();
                        RSSI.setText(String.valueOf(metric[0])+" dB");
                        RSSI_AVG.setText("Frequency : "+String.valueOf(metric[1])+" Hz");
                        RSSI_STDEV.setText("STDEV: ± "+String.valueOf(metric[2])+" dB");
                        LAT_AVG.setText("Latency : --"+/*String.valueOf(metric[3])*/" sec");
                        LAT_STDEV.setText("± --"+/*String.valueOf(metric[4])+*/" sec");
                    }
                }
            });
        }
    }

    long[] metrics(){

        int number = Constants.iterator;
        long[][] rssiArray = Constants.rssiArray.clone();
        Constants.iterator = 0;

        long mean = 0;long stdev = 0;
        long lat_mean = 0; long lat_stdev = 0;

        for(int i = 0; i<=number;i++)
            mean += rssiArray[1][i];

        mean = mean/(number+1);

        for(int i = 0; i<=number;i++)
            stdev += ((rssiArray[1][i] - mean) * (rssiArray[1][i] - mean));

        stdev = (long) Math.sqrt((stdev)/(number+1));

        /* Mean Latency in milliseconds */
        lat_mean = rssiArray[0][number] - rssiArray[0][0];
        lat_mean = (lat_mean/(number+1));


        for(int i = 0; i<=number;i++)
            lat_stdev += ((rssiArray[0][i] - lat_mean) * (rssiArray[0][i] - lat_mean));


        lat_stdev = (long) Math.sqrt((lat_stdev)/(number+1));

        return new long[]{mean,(number+1)/2,stdev,lat_mean/1000,lat_stdev/1000};
    }

private void LOCK_UI(){
        setContentView(R.layout.activity_lock_screen);
      // seven time presser listener
    }
    private void ERROR_UI(){
        setContentView(R.layout.activity_lock_screen);
        // seven time presser listener
    }

    private void UPLOAD_UI(){
     //   setContentView(R.layout.activity_lock_screen);
        // seven time presser listener
    }

public void ResumeBox(int rssi, int avg, int std_dev, long latency, long lat_set_dev){
    if(RSSI!=null) {
        RSSI.setText(String.valueOf(rssi));
        RSSI_AVG.setText(String.valueOf(avg));
        RSSI_STDEV.setText(String.valueOf(std_dev));
        LAT_AVG.setText(String.valueOf(latency));
        LAT_AVG.setText(String.valueOf(lat_set_dev));
    }
}

    public void onButtonClicked(View view){
    //    Constants.bodyPosition = 5;

        initTurbo();
  /*      if (view.getId()!=R.id.radio_lazy)
            Gimbal.DataCollectionMode();
        else
            Gimbal.DataUploadMode(); */

       // android.provider.Settings.System.putInt(getContentResolver(),
       //         Settings.System.SCREEN_OFF_TIMEOUT, 8000);
        // Check which radio button was clicked
      /*  switch(view.getId()) {
            case R.id.radio_standing:
                Constants.bodyPosition = 1;
                Constants.TURBO = 0;
                 break;
            case R.id.radio_sitting:
                Constants.bodyPosition = 2;
                Constants.TURBO = 0;
                break;
            case R.id.radio_laying:
                Constants.bodyPosition = 3;
                Constants.TURBO = 0;
                break;
            case R.id.radio_walking:
                Constants.bodyPosition = 4;
                Constants.TURBO = 0;
                break;
            case R.id.radio_strip:
                Constants.bodyPosition = 5;
                initTurbo();
                break;
            case R.id.radio_lazy:
                Constants.bodyPosition = 6;
                Constants.TURBO = 0;
                break;*/
       // }
  //      Log.d("HomeScreen ", "State = "+Constants.bodyPosition);
        //Intent i = new Intent(this, Logger_GroundTruthTest.class);
        //startService(i);

    /*    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String res = which == DialogInterface.BUTTON_POSITIVE ? "1" : "0";
                try {
                    File file = new File(Constants.debug_directory_file, Constants.FilenameFormat.format(new Date()));
                    FileWriter f = new FileWriter(file, true);
                    f.write(System.currentTimeMillis() + "," + res + "\n");
                    f.close();
                }
                catch(IOException e) {

                }
            };
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setMessage("Confirm PREVIOUS setting?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();*/

     //   android.provider.Settings.System.putInt(getContentResolver(),
     //           Settings.System.SCREEN_OFF_TIMEOUT, 0);
    }
    @Override
    protected void onStart(){
        super.onStart();
   //     Log.d("view", "Started");
    }
    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onDestroy(){
        Constants.HomeScreenRunning = false;
        super.onDestroy();

    //    Log.d("view", "Destroyed");
    }
    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop(){
        Constants.HomeScreenRunning = false;
        super.onStop();
        timer.cancel();

        //     Log.d("view", "stopped");
    }


    private void initTurbo() {
        if (Constants.TURBO == 1)
            return;

        Constants.TURBO = 1;

        Toast.makeText(Gimbal.context, "TURBO STARTED", Toast.LENGTH_SHORT).show();

        Runnable timeout = new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Constants.TURBO = 0;
                     //   Constants.bodyPosition = 6;
                        Toast.makeText(Gimbal.context, "TURBO STOPPED", Toast.LENGTH_SHORT).show();
                    }
                }, 2 * 60 * 1000); //Change this back. Changed Turbo to 10 seconds for testing
            }
        };
        timeout.run();
    }

}
