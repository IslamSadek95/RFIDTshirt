package guc.islam.rfidt_shirt;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.content.Context;
import android.Manifest;
import android.view.View;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Listener{

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CALL = 1;
    private static final int REQUEST_SMS = 1;
    private static final int REQUEST_Location= 1;
    private EditText mEtMessage;
    private Button mBtWrite;
    private Button mBtRead;
    private static String message;
    private int taps = 0;
    private int fireTaps = 0;
    private String prevMessage = "";
    private  String ambulanceNumber,ambulanceMessage,policeNumber, emergencyContact, fireDepartment;
    private NFCWriteFragment mNfcWriteFragment;
    private NFCReadFragment mNfcReadFragment;
    private IDFragment idFragment;
    private String knownLocation;


    private boolean isDialogDisplayed = false;
    private boolean isWrite = false;

    private NfcAdapter mNfcAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initNFC();



        ambulanceNumber = "01060784477";
        policeNumber  = "01060784477";
        emergencyContact = "01060784477";
        fireDepartment = "01060784477";


    }
    private void initViews() {

        mEtMessage = (EditText) findViewById(R.id.et_message);
        mBtWrite = (Button) findViewById(R.id.btn_write);
        mBtRead = (Button) findViewById(R.id.btn_read);

        mBtWrite.setOnClickListener(view -> showWriteFragment());
        mBtRead.setOnClickListener(view -> showReadFragment());
    }

    private void initNFC(){

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private void showWriteFragment() {

        isWrite = true;

        mNfcWriteFragment = (NFCWriteFragment) getFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);

        if (mNfcWriteFragment == null) {

            mNfcWriteFragment = NFCWriteFragment.newInstance();
        }
        mNfcWriteFragment.show(getFragmentManager(),NFCWriteFragment.TAG);

    }


    private void showIDFragment() {



        idFragment = (IDFragment) getFragmentManager().findFragmentByTag(idFragment.TAG);

        if (idFragment == null) {

            idFragment = IDFragment.newInstance();
        }
        idFragment.show(getFragmentManager(),idFragment.TAG);

    }


    private void showReadFragment() {


        mNfcReadFragment = (NFCReadFragment) getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);

        if (mNfcReadFragment == null) {

            mNfcReadFragment = NFCReadFragment.newInstance();
        }
        mNfcReadFragment.show(getFragmentManager(),NFCReadFragment.TAG);

    }

    @Override
    public void onDialogDisplayed() {

        isDialogDisplayed = true;
    }

    @Override
    public void onDialogDismissed() {

        isDialogDisplayed = false;
        isWrite = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected,tagDetected,ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if(mNfcAdapter!= null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mNfcAdapter!= null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        //Log.d(TAG, "onNewIntent: "+intent.getAction());

        if(tag != null) {
            Toast.makeText(this, getString(R.string.message_tag_detected), Toast.LENGTH_SHORT).show();
            Ndef ndef = Ndef.get(tag);

            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                message = new String(ndefMessage.getRecords()[0].getPayload());

                ndef.close();

            } catch (IOException | FormatException e) {
                e.printStackTrace();

            }



            if (!isDialogDisplayed)
            {
                taps++;
                checkSequence();
            }


            // Checks on the message read from the RFID

            if(message.equals("Call Police") && !isDialogDisplayed && taps == 2) {
                CallPolice();
                taps =0;
            }

            if(message.equals("Call Ambulance") && !isDialogDisplayed && taps == 2) {
                CallAmbulance();
                taps =0;
            }

            if (message.equals("Call Emergency Contact")  && !isDialogDisplayed && taps == 2)
            {
                CallEmergency();
                taps=0;
            }

            if (fireTaps==2)
            {
                CallFire();
                taps=0;
                fireTaps=0;
            }

            if(message.equals("Heart Attack") || message.equals("Diabetic Coma") || message.equals("Going into Labor") && !isDialogDisplayed && taps == 1)
            {
                Log.d("hii","helloooo");
                textAmbulance();
                taps=0;
            }






            if(message.equals("ID") && !isDialogDisplayed && taps == 1)
            {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (message.equals("ID"))
                            showIDFragment();

                    }
                }, 3000);

                prevMessage = "ID";

                taps = 0;

            }

            if (message.equals("Send location to emergency contacts") && !isDialogDisplayed && taps == 1)
            {
                sendCurrentLocationToEmergencyContacts();
                taps = 0;
            }

            if (message.equals("Kidnapped") && !isDialogDisplayed && taps == 1)
            {
                kidnapped();
                taps = 0;
            }



            if (isDialogDisplayed) {

                if (isWrite) {

                    String messageToWrite = mEtMessage.getText().toString();
                    mNfcWriteFragment = (NFCWriteFragment) getFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);
                    mNfcWriteFragment.onNfcDetected(ndef,messageToWrite);

                } else {

                    mNfcReadFragment = (NFCReadFragment)getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);
                    mNfcReadFragment.onNfcDetected(ndef);


                }
            }



        }
    }

    // This method is responsible for the sequence of the path from the ID tag to the Fire department tag
    private void checkSequence()
    {
        if (message.equals("Call Ambulance") && prevMessage.equals("ID") && taps ==1)
        {
            Log.d("PREV Message:",prevMessage);
            fireTaps ++;
            prevMessage = "Call Ambulance";
            //taps = 0;
        }

        if (message.equals("Call Fire Department") && prevMessage.equals("Call Ambulance") && taps ==2 && fireTaps==1)
        {
            Log.d("PREV Message:",prevMessage);
            fireTaps ++;
            //taps = 0;
        }



        Log.d("Current Message:",message);
        Log.d("Fire TAPS",""+ fireTaps);
    }

    private void CallPolice() {
        String number = policeNumber;;


            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
            } else {
                String dial = "tel:" + number;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }


    }

    private void CallAmbulance() {
        String number = ambulanceNumber;


            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
            } else {
                String dial = "tel:" + number;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
            }


    }

    private void CallFire() {
        String number = fireDepartment;


        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {
            String dial = "tel:" + number;
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }


    }

    private void CallEmergency() {
        String number = emergencyContact;


        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {
            String dial = "tel:" + number;
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }


    }

    public void sendCurrentLocationToEmergencyContacts()
    {
        String location  = sendLocation();
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
        }

        else
        {

            String number = emergencyContact;

           String message =  "I need help, My current location is "+ location;
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);
        }
    }

    public void kidnapped()
    {
        String location = sendLocation();

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
        }

        else
        {

            String number = policeNumber;

            String message =  "I need help, My current location is "+ location;
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);

            CallPolice();
        }

    }

    public void textAmbulance()
    {

        String location  = sendLocation();
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
        }

        else
        {


            ambulanceMessage = "Case: " + message + ". Location: "+location+".. Age: 33 years old. Blood Type: B+.";
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(ambulanceNumber, null, ambulanceMessage, null, null);
        }

    }


    private String sendLocation() {

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_Location);

            return null ;
        } else {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


            List<String> providers = locationManager.getProviders(true);

            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider, 1000, 0,
                        new LocationListener() {

                            public void onLocationChanged(Location location) {
                            }

                            public void onProviderDisabled(String provider) {
                            }

                            public void onProviderEnabled(String provider) {
                            }

                            public void onStatusChanged(String provider, int status,
                                                        Bundle extras) {
                            }
                        });

                Location location = locationManager.getLastKnownLocation(provider);
                Log.d("hiiii","hhhhh1");
                Log.d("hiiii",""+providers);

                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    knownLocation = "https://google.com/maps/place/" + longitude + ',' + latitude;
                    Log.d("hiiii","hhhhh2");
                    Log.d("hiiii",knownLocation);
                    return knownLocation;
                }

                else {
                    return sendLocation();
                }



            }


        }

        return null;

    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && message.equals("Call Police")) {
                CallPolice();
            }
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && message.equals("Call Ambulance")) {
                CallAmbulance();
            }

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && message.equals("Call Fire Department")) {
                CallFire();
            }
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && message.equals("Call Emergency Contact")) {
                CallEmergency();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }

//        if (requestCode == REQUEST_SMS) {
//            if (grantResults.length > 0
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                SmsManager smsManager = SmsManager.getDefault();
//                smsManager.sendTextMessage(ambulanceNumber, null, ambulanceMessage, null, null);
//                Toast.makeText(getApplicationContext(), "SMS sent.",
//                        Toast.LENGTH_LONG).show();
//            } else {
//                Toast.makeText(getApplicationContext(),
//                        "SMS faild, please try again.", Toast.LENGTH_LONG).show();
//                return;
//            }
//
//
//        }
    }
}
