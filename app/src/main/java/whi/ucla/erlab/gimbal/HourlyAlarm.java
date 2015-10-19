package whi.ucla.erlab.gimbal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by Arjun on 4/7/2015.
 */
public class HourlyAlarm {
    private AlarmManager alarmMgr = null;
    private PendingIntent alarmIntent = null;


    public HourlyAlarm(){
       // alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    }

    public void set(){
        //Intent intent = new Intent(context, BootReceiver.class);
        //alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

    // Set the alarm to start at next hour.
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        calendar.setTimeInMillis((System.currentTimeMillis() + Constants.SERVER_TIME_OFFSET));
        calendar.set(Calendar.HOUR_OF_DAY, getCurrentHour()+1);

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_HOUR, alarmIntent);
    }

/* Get Current Hour */
    private static int getCurrentHour(){
        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        return  rightNow.get(Calendar.HOUR_OF_DAY);
    }

/* Cancels the hourly alarm and resets AlarmMgr and PendingIntents to null */
public void cancel(){
    if(alarmMgr!=null) {
        alarmMgr.cancel(alarmIntent);
        alarmIntent = null;
        alarmMgr = null;
    }

}
}
