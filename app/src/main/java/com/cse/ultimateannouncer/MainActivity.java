package com.cse.ultimateannouncer;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {
    private final int CHECK_CODE = 0x1;
    private final int LONG_DURATION = 5000;
    private final int SHORT_DURATION = 1200;

    private Speaker speaker;

    private ToggleButton toggle;
    private CompoundButton.OnCheckedChangeListener toggleListener;

    private TextView smsText;
    private TextView smsSender;

    private BroadcastReceiver smsReceiver;
    protected ContextWrapper context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        toggle = findViewById(R.id.speechToggle);
        smsText = findViewById(R.id.sms_text);
        smsSender = findViewById(R.id.sms_sender);
        Intent i0 = new Intent();
        i0.setAction("com.androidexample.screenonoff.ScreenOnOffService");
        startService(i0);
        PhoneStateListener callStateListener =new PhoneStateListener(){
            @SuppressLint("Range")
            public void onCallStateChanged(int state , String incomingNumber){
                if(state==TelephonyManager.CALL_STATE_RINGING){
                    String newSender = incomingNumber;
                    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                            Uri.encode(newSender));
                    Cursor cursor = getContentResolver().query(uri,new
                            String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},newSender,null,null);
                    if (cursor.moveToFirst()){
                        newSender  = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                        speaker.speak("call from" + newSender+"!");
                        speaker.pause(LONG_DURATION);
                        speaker.speak("call from" + newSender+"!");
                    }
                    else{
                        incomingNumber ="unknown number";
                        speaker.speak("call from " +incomingNumber +"!");
                        speaker.pause(LONG_DURATION);
                        speaker.speak("call from " +incomingNumber +"!");
                    }
                    cursor.close();
                    Toast.makeText(getApplicationContext(),"Phone is ringing"+incomingNumber,Toast.LENGTH_LONG).show();
                }
                if(state==TelephonyManager.CALL_STATE_OFFHOOK){
                    Toast.makeText(getApplicationContext(),"Phone is in a call or call picked",Toast.LENGTH_LONG).show();
                }
                if(state==TelephonyManager.CALL_STATE_IDLE){
                   // Toast.makeText(getApplicationContext(),"Phone is in a call or call picked",Toast.LENGTH_LONG).show();
                }
            }
        };
        telephonyManager.listen(callStateListener,PhoneStateListener.LISTEN_CALL_STATE);


        toggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if(isChecked){
                    speaker.allow(true);
                    speaker.speak(getString(R.string.start_speaking));
                }else{
                    speaker.speak(getString(R.string.stop_speaking));
                    speaker.allow(false);
                }
            }
        };
        toggle.setOnCheckedChangeListener(toggleListener);

        checkTTS();
        initializeSMSReceiver();
        registerSMSReceiver();
    }
    private void checkTTS(){
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                speaker = new Speaker(this);
            } else {
                Intent install = new Intent();
                install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);
            }
        }
    }
    private void initializeSMSReceiver(){
        smsReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();
                if(bundle!=null){
                    Object[] pdus = (Object[])bundle.get("pdus");
                    for(int i=0;i<pdus.length;i++){
                        byte[] pdu = (byte[])pdus[i];
                        SmsMessage message = SmsMessage.createFromPdu(pdu);
                        String text = message.getDisplayMessageBody();
                        String sender = getContactName(message.getOriginatingAddress());
                        speaker.pause(LONG_DURATION);
                        speaker.speak("You have a new message from" + sender + "!");
                        speaker.pause(SHORT_DURATION);
                        speaker.speak(text);
                        smsSender.setText("Message from " + sender);
                        smsText.setText(text);
                        Toast.makeText(getApplicationContext(),
                                "Message received",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                }

            }
        };
    }
    private String getContactName(String phone){
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
        String projection[] = new String[]{ContactsContract.Data.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if(cursor.moveToFirst()){
            return cursor.getString(0);
        }else {
            return "unknown number";
        }
    }
    private void registerSMSReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, intentFilter);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsReceiver);
        speaker.destroy();
    }
}