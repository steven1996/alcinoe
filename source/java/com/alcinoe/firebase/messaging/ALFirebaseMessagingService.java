package com.alcinoe.firebase.messaging;

import com.embarcadero.firemonkey.FMXNativeActivity;
import me.leolin.shortcutbadger.ShortcutBadger;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationCompat;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.URL;
import java.util.Map;

public class ALFirebaseMessagingService extends FirebaseMessagingService {
        
  public static final String ACTION_MESSAGERECEIVED = "com.alcinoe.firebase.messaging.messageReceived";
  private static int currentTimeMillis() {
    return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
  }
  private final static AtomicInteger c = new AtomicInteger(currentTimeMillis());
  private static int getUniqueID() {
    return c.incrementAndGet();
  }
  
  /**
   * Called when message is received.
   * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
   */
  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
        
    // There are two types of messages data messages and notification messages. Data messages are handled
    // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
    // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
    // is in the foreground. When the app is in the background an automatically generated notification is displayed.
    // When the user taps on the notification they are returned to the app. Messages containing both notification
    // and data payloads are treated as notification messages. The Firebase console always sends notification
    // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options

    //Log
    Log.v("ALFirebaseMessagingService", "onMessageReceived");
    
    //init data
    Map<String, String> data = remoteMessage.getData();

    /* build the intent */
    Intent intent = new Intent(ACTION_MESSAGERECEIVED);
    for (Map.Entry<String, String> entry : data.entrySet()) {
      intent.putExtra(entry.getKey(), entry.getValue()); /* String */
    }    
    intent.putExtra("gcm.from", remoteMessage.getFrom()); /* String */
    intent.putExtra("gcm.message_id", remoteMessage.getMessageId()); /* String */
    intent.putExtra("gcm.message_type", remoteMessage.getMessageType()); /* String */
    intent.putExtra("gcm.sent_time", remoteMessage.getSentTime()); /* long */
    intent.putExtra("gcm.to", remoteMessage.getTo()); /* String */
    intent.putExtra("gcm.ttl", remoteMessage.getTtl()); /* int */
    
    /* send the data to registered receivers */
    /* sendBroadcast() returns true if there was 1+ receivers, false otherwise */
    boolean receveirIsPresent;
    try{

      receveirIsPresent = LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    } catch (Throwable e){ Log.e("ALFirebaseMessagingService", "Exception", e); receveirIsPresent = false; }  
                   

    try{

      /* if no receveir and not a notification message then show a custom notification if present in data payload */
      if ((!receveirIsPresent) && 
          (remoteMessage.getNotification() == null) && 
          (data.containsKey("notification") && data.get("notification").equals("1"))) {
            
        // actually i support these params, but nothing forbid to extends them
        // notification - Must be equal to 1 to activate showing of custom notification when no receiver
        // notification_tag - A string identifier for this notification. 
        // notification_color - The accent color to use
        // notification_text - Set the second line of text in the platform notification template.
        // notification_title - Set the first line of text in the platform notification template.
        // notification_largeicon - url of the large icon to use - Add a large icon to the notification content view
        // notification_number - must equal to "auto" to increase the number of items this notification represents.
        // notification_onlyalertonce - Set this flag if you would only like the sound, vibrate and ticker to be played if the notification is not already showing.      
        // notification_smallicon - The name of the desired resource. - Set the small icon resource, which will be used to represent the notification in the status bar.
        // notification_ticker - Set the "ticker" text which is sent to accessibility services (The pop-up Text in Status Bar when the Notification is Called)
        // notification_vibrate - must equal to 1 to activate the default vibration pattern (0, 1200)
        // notification_visibility - Specify the value of visibility - One of VISIBILITY_PRIVATE (the default), VISIBILITY_SECRET, or VISIBILITY_PUBLIC.
        // notification_badgecount - update the shortcut badge count with this number 
                     
        intent.setClass(this, FMXNativeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, /* context	Context: The Context in which this PendingIntent should start the activity. */
                                                                getUniqueID(), /* requestCode	int: Private request code for the sender */ 
                                                                intent, /* intents	Intent: Array of Intents of the activities to be launched. */
                                                                PendingIntent.FLAG_UPDATE_CURRENT); /* flags	int: May be FLAG_ONE_SHOT, - Flag indicating that this PendingIntent can be used only once. 
                                                                                                                          FLAG_NO_CREATE, - Flag indicating that if the described PendingIntent does not already exist, then simply return null instead of creating it.
                                                                                                                          FLAG_CANCEL_CURRENT, - Flag indicating that if the described PendingIntent already exists, the current one should be canceled before generating a new one. 
                                                                                                                          FLAG_UPDATE_CURRENT, - Flag indicating that if the described PendingIntent already exists, then keep it but replace its extra data with what is in this new Intent.
                                                                                                                          FLAG_IMMUTABLE - Flag indicating that the created PendingIntent should be immutable.
                                                                                                       or any of the flags as supported by Intent.fillIn() to 
                                                                                                       control which unspecified parts of the intent that can 
                                                                                                       be supplied when the actual send happens. */
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        if (data.containsKey("notification_color")) { 
          notificationBuilder = notificationBuilder.setColor(Integer.parseInt(data.get("notification_color")));
        }
        if (data.containsKey("notification_text")) { 
          notificationBuilder = notificationBuilder.setContentText(data.get("notification_text"));
        }
        if (data.containsKey("notification_title")) { 
          notificationBuilder = notificationBuilder.setContentTitle(data.get("notification_title"));
        }
        if (data.containsKey("notification_largeicon")) { 
          try {
          
            URL url = new URL(data.get("notification_largeicon"));
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            if (bitmap != null) {

              int w;
              if (bitmap.getWidth() < bitmap.getHeight()) { w = bitmap.getWidth(); }
              else { w = bitmap.getHeight(); }

              Bitmap bitmapCropped = Bitmap.createBitmap(bitmap/*src*/, (bitmap.getWidth() - w) / 2/*X*/, (bitmap.getHeight() - w) / 2/*Y*/, w/*Width*/, w/*height*/, null/*m*/, true/*filter*/);
              if (!bitmap.sameAs(bitmapCropped)) { bitmap.recycle(); }
              
              notificationBuilder = notificationBuilder.setLargeIcon(bitmapCropped); 

            }
          
          } catch(Throwable e) { Log.e("ALFirebaseMessagingService", "Exception", e); }
        }
        if (data.containsKey("notification_number") && 
            data.containsKey("notification_tag") && 
            data.get("notification_number").equals("auto")) { 
          SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
          int currentCount = sp.getInt("notification_count_" + data.get("notification_tag"), 0);
          SharedPreferences.Editor editor = sp.edit();
          editor.putInt("notification_count_" + data.get("notification_tag"), currentCount + 1);
          editor.commit();
          if (currentCount > 0) { notificationBuilder = notificationBuilder.setNumber(currentCount + 1); }
        }
        else if (data.containsKey("notification_number")) {
          notificationBuilder = notificationBuilder.setNumber(Integer.parseInt(data.get("notification_number")));  
        }
        if (data.containsKey("notification_onlyalertonce") && data.get("notification_onlyalertonce").equals("1")) { 
          notificationBuilder = notificationBuilder.setOnlyAlertOnce(true);
        } 
        if (data.containsKey("notification_smallicon")) { 
          notificationBuilder = notificationBuilder.setSmallIcon(
            this.getApplicationContext().getResources().getIdentifier(
              data.get("notification_smallicon"), // name	String: The name of the desired resource.
              "drawable", // String: Optional default resource type to find, if "type/" is not included in the name. Can be null to require an explicit type.
              this.getApplicationContext().getPackageName())); // String: Optional default package to find, if "package:" is not included in the name. Can be null to require an explicit package.
        }                  
        if (data.containsKey("notification_ticker")) { 
          notificationBuilder = notificationBuilder.setTicker(data.get("notification_ticker"));
        }
        if (data.containsKey("notification_vibrate") && data.get("notification_vibrate").equals("1")) { 
          notificationBuilder = notificationBuilder.setVibrate(new long[] { 0, 1200 });
        } 
        if (data.containsKey("notification_visibility")) { 
          notificationBuilder = notificationBuilder.setVisibility(Integer.parseInt(data.get("notification_visibility")));
        }
        notificationBuilder = notificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS);
        notificationBuilder = notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder = notificationBuilder.setShowWhen(true);
        notificationBuilder = notificationBuilder.setAutoCancel(true);
        notificationBuilder = notificationBuilder.setContentIntent(pendingIntent);
        
        if (data.containsKey("notification_badgecount")) { 
          ShortcutBadger.applyCount(this.getApplicationContext(), Integer.parseInt(data.get("notification_badgecount")));
        } 
    
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);    
        notificationManager.notify(data.get("notification_tag"), /* tag	String: A string identifier for this notification. May be null. */ 
                                   0, /* id	int: An identifier for this notification. The pair (tag, id) must be unique within your application. */  
                                   notificationBuilder.build()); /* notification	Notification: A Notification object describing what to show the user. Must not be null. */  
      
      }
    
    } 
    catch (Throwable e){ Log.e("ALFirebaseMessagingService", "Exception", e); }  
        
  }

}
