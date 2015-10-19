package whi.ucla.erlab.gimbal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


/**
 * Created by Babak on 3/2/15.

Simple state machine to handle events based on power state. Manages all services including
BeaconScan, GyroScan, UploadService, Logger, and UI behaviour.
 */
public class BootReceiver extends BroadcastReceiver
{

    public BootReceiver(){
        Constants.initDB();
    }

/* Upon receiving CONNECTED,DISCONNECTED, or BOOT_COMPLETED : The following code is executed. */
    @Override
    public void onReceive(Context context, Intent intent) {

        if(Constants.DEVICE_LOCK)
          return;

        switch (intent.getAction()) {

            case Intent.ACTION_POWER_DISCONNECTED:
                Gimbal.DataCollectionMode();
                Toast.makeText(Gimbal.context, "DISCONNECTED", Toast.LENGTH_SHORT).show();
                break;

            case Intent.ACTION_POWER_CONNECTED:
                Toast.makeText(Gimbal.context, "CONNECTED", Toast.LENGTH_SHORT).show();
                Gimbal.DataUploadMode();
                break;

            case Intent.ACTION_SHUTDOWN:
            case Intent.ACTION_BATTERY_LOW:
                Toast.makeText(Gimbal.context, "SHUTDOWN", Toast.LENGTH_SHORT).show();
                Gimbal.ShutDownMode();
                break;

            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_BATTERY_OKAY:
                Toast.makeText(Gimbal.context, "RESUMING", Toast.LENGTH_SHORT).show();
                Gimbal.resumeState();
                break;
        }
    }

}
