package com.m.bot;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;

import com.m.bot.model.Message;
import com.m.bot.service.ChatService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "channel_id";
    private static final String CHANNEL_NAME = "channel_name";
    private static final String CHANNEL_DESC = "channel_description";
    private MessageAdapter adapter;
    private BroadcastReceiver receiver;
    RecyclerView rv_messages;
    Button btn_send;
    EditText et_message;
    List<Message> messagesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rv_messages = (RecyclerView)findViewById(R.id.recyclerView);
        btn_send = (Button)findViewById(R.id.btn_send);
        et_message = (EditText)findViewById(R.id.editText_message);
        setRecyclerView();
        setClickEvents();
        receiveBroadCast();
        messagesList.add(new Message("RECEIVE","Hello, how are you? How could I help you today?"));
        rv_messages.scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.stop_service) {
            Intent intent = new Intent(this, ChatService.class);
            stopService(intent);
            sendNotification("“ChatBot Stopped: 67”",(int)System.currentTimeMillis());
        }

        return super.onOptionsItemSelected(item);
    }

    public void setRecyclerView(){
        adapter = new MessageAdapter(this,messagesList);
        rv_messages.setAdapter(adapter);
        rv_messages.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    public void setClickEvents(){
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    public void sendMessage(){
        String message = et_message.getText().toString();
        if(!message.isEmpty()) {
            //send broadcast
            Bundle data = new Bundle();
            data.putInt(ChatService.CMD, ChatService.CMD_SEND_MESSAGE);
            data.putString(ChatService.KEY_MESSAGE_TEXT, message);
            messagesList.add(new Message("SEND", message));
            et_message.setText("");
            rv_messages.scrollToPosition(adapter.getItemCount() - 1);

            Intent intent = new Intent(this, ChatService.class);
            intent.putExtras(data);
            startService(intent);
            //send notification
            sendNotification(message,(int)System.currentTimeMillis());
        }
    }

    private void receiveBroadCast(){
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_NEW_MESSAGE)) {
                    String message = intent.getStringExtra(Constants.CHAT_MESSAGE);
                    botResponse(message);
                }
            }
        };

        // Register the BroadcastReceiver to receive the MY_BROADCAST action
        IntentFilter filter = new IntentFilter(Constants.BROADCAST_NEW_MESSAGE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver
        unregisterReceiver(receiver);
    }

    public void sendNotification(String message, int id){
        NotificationManager notificationManager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);

        // Create a notification channel (required for Android Oreo and higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }

        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.customize_notification);
        Date date = new Date();
        remoteViews.setTextViewText(R.id.dateAndName, date.toString());
        remoteViews.setTextViewText(R.id.notificationContent, message);
        remoteViews.setTextViewText(R.id.notificationSender, "Sended by Mingyuan");

        try {
            Notification noti = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(date.toString())
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setCustomBigContentView(remoteViews)
                    .setLights(Color.BLUE, 1000, 1000)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();
            notificationManager.notify(id, noti);
        } catch (IllegalArgumentException e) {
        }
    }

    public void botResponse(String message){
        Log.v("botResponse", "in botResponse???");
        new Thread(() -> {
            //Fake response delay
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                //Gets the response
                String response = "";
                String otherResponse = "";
                if(message.toLowerCase(Locale.ROOT).contains("delivery") || message.toLowerCase(Locale.ROOT).contains("time")){
                    //What are the delivery timelines and pickup options?
                    response = "There are a lot of reasons to love being a Ford owner. Ford Pickup & Delivery* is one more. Just tell your dealer when and where you’d like to have your Ford picked up — home or work — and we’ll come get it, service it at the dealership and return it to you.";
                    messagesList.add(new Message("RECEIVE",response));
                }else if(message.toLowerCase(Locale.ROOT).contains("how much") || message.toLowerCase(Locale.ROOT).contains("pick up")){
                    //How much does Ford Pickup & Delivery cost?
                    response = "Ford Pickup & Delivery is complimentary*. When you need service, your Ford dealer will pick up and return your vehicle.\n" +
                            "* Ford Pickup & Delivery is offered by participating dealers and may be limited based on availability, distance, or other dealer-specified criteria. Does not include parts or repair charges. A nonoperational vehicle is not eligible and will require a Roadside event.";
                    messagesList.add(new Message("RECEIVE",response));
                }else if(message.toLowerCase(Locale.ROOT).contains("financing") || message.toLowerCase(Locale.ROOT).contains("leasing") ||  message.toLowerCase(Locale.ROOT).contains("options")){
                    //What financing or leasing options are available?
                    response = "We offer a variety of financing options for you to purchase or lease your next Ford. Explore and compare them below and find the one that fits your auto financing needs."
                    + "\n"
                    + "1.Standard Purchase.Equal monthly payments on a variety of terms allow you build equity on the purchase of your new or used vehicle. There are no kilometre limitations and you're free to customize your vehicle."
                    + "\n"
                    + "2.Red Carpet Lease:Choose from a range of kilometre options and lease-end choices, and enjoy many other benefits exclusive to leasing.";
                    messagesList.add(new Message("RECEIVE",response));
                }else{
                    otherResponse = "Sorry, I don't understand.";
                    messagesList.add(new Message("RECEIVE",otherResponse));

                }
                //Scrolls us to the position of the latest message
                rv_messages.scrollToPosition(adapter.getItemCount() - 1);

            });
        }).start();
    }
}