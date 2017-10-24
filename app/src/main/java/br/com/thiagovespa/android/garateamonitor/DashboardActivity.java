package br.com.thiagovespa.android.garateamonitor;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int GARATEA_PERMISSIONS_REQUEST_LOCATION = 10;
    public final String ACTION_USB_PERMISSION = "br.com.thiagovespa.android.garateamonitor.USB_PERMISSION";

    /**
     * View components
     */
    private Button startButton, sendButton, clearButton, stopButton;
    private EditText editText;
    private ScrollView svDash;

    private boolean consoleFull = false;

    /**
     * Fragments and map
     */
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private ConsoleFragment consoleFragment;
    private String lastLine;
    private StringBuffer dataString = new StringBuffer();
    /**
     * USB Callback that triggers whenever data is read
     */
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                toLastLine(data);
                consoleAppend(data);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };
    /**
     * USB stuff
     */
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;
    /**
     * USB Broadcast Receiver
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            consoleAppend("Conexão serial estabelecida!\n");
                            consoleAppend("----------------------------\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            consoleAppend("ERRO: Porta serial fechada!\n");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        consoleAppend("ERRO: Porta seria não obtida!\n");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    consoleAppend("ERRO: Sem permissão pra acessar porta serial!\n");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

        ;
    };

    /**
     * Fill lastLine to parse
     *
     * @param data data read
     */
    private void toLastLine(String data) {
        dataString.append(data);
        int lineBrakeIdx = dataString.indexOf("\n", 1);
        if (lineBrakeIdx > 0) {
            lastLine = dataString.substring(0, lineBrakeIdx);
            dataString.delete(0, lineBrakeIdx);
            parseData(lastLine);
        }
    }

    /**
     * Parse data
     *
     * @param data data to parse. Ex: U;201;-220010.17;-479020.44;10;822.90;81017.00;0.28;0;19.50;a;143;-220008.78;-479022.28;6;799.00;86.80;0.39;0;76.24;0.00;0.00;807.47;91996;27.70;308;0; 70; -32
     */
    private void parseData(final String data) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] dat = data.split(";");
                if (dat.length > 14) {
                    try {
                        double gps1Lat = Double.parseDouble(dat[2]);
                        double gps1Lng = Double.parseDouble(dat[3]);
                        double gps2Lat = Double.parseDouble(dat[12]);
                        double gps2Lng = Double.parseDouble(dat[13]);
                        updateMap(gps1Lat / 10000.0, gps1Lng / 10000.0, gps2Lat / 10000.0, gps2Lng / 10000.0);
                    } catch (Exception e) {
                        consoleAppend("Erro ao formatar latitude e longitude: " + e.getMessage() + ":" + e.getCause() + ": " + dat[2] + "," + dat[3] + "," + dat[12] + "," + dat[13]);
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);


        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        consoleFragment = (ConsoleFragment) getSupportFragmentManager()
                .findFragmentById(R.id.consoleFragment);

        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        svDash = (ScrollView) findViewById(R.id.svDash);
        svDash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleMap();
            }
        });

        svDash.setOnTouchListener(new View.OnTouchListener() {
            private int CLICK_ACTION_THRESHOLD = 200;
            private float startX;
            private float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        if (isAClick(startX, endX, startY, endY)) {
                            toggleMap();
                        }
                        break;
                }
                return true;
            }

            private boolean isAClick(float startX, float endX, float startY, float endY) {
                float differenceX = Math.abs(startX - endX);
                float differenceY = Math.abs(startY - endY);
                return !(differenceX > CLICK_ACTION_THRESHOLD/* =5 */ || differenceY > CLICK_ACTION_THRESHOLD);
            }
        });

        setUiEnabled(false);


        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    /**
     * Toogle map view
     */
    private void toggleMap() {

        if (consoleFull) {
            consoleFull = false;
            //show Map
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .show(mapFragment)
                    .commit();

            ViewGroup.LayoutParams params = consoleFragment.getView().getLayoutParams();

            params.height = Utils.pxFromDp(this.getApplicationContext(), 60);
        } else {
            consoleFull = true;

            //hide Map
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .hide(mapFragment)
                    .commit();


            ViewGroup.LayoutParams params = consoleFragment.getView().getLayoutParams();

            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
        }
    }

    /**
     * Get current location
     *
     * @return location
     */
    private Location getMyLocation() {
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)) {
            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    GARATEA_PERMISSIONS_REQUEST_LOCATION);

        } else {
                LocationManager locationManager = (LocationManager)
                        getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();

                return locationManager.getLastKnownLocation(locationManager
                        .getBestProvider(criteria, false));


        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case GARATEA_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    consoleAppend("Sem permissão à localização");
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    /**
     * Control Button behavior
     *
     * @param bool true if it is enabled, false otherwise
     */
    private void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);

    }


    /**
     * Method that verifies if an device vendor ID is from an Arduino
     * <p>
     * Vendor
     * ID 	Product
     * ID 	Remarks
     * <p>
     * 0x10c4	0xea60	CP2102
     * 0x10c4	0xea70	CP2105
     * 0x10c4	0xea71	CP2108
     * 0x10c4	0xea80	CP2110
     * 0x067b	0x2303	Prolific PL2303
     * 0x0403	0x0601	FTDI FT232R
     * 0x0403	0x6015	FTDI FT231X
     * 0x2341	0x0001	Arduino UNO
     * 0x2341	0x0010	Arduino Mega 2560
     * 0x2341	0x003b	Arduino Serial Adapter
     * 0x2341	0x003f	Arduino Mega ADK
     * 0x2341	0x0042	Arduino Mega 2560 R3
     * 0x2341	0x0043	Arduino UNO R3
     * 0x2341	0x0044	Arduino Mega ADK R3
     * 0x2341	0x8036	Arduino Leonardo
     * 0x16c0	0x0483	TeensyDuino
     * 0x03eb	0x2044	ATMEL LUFA CDC Demo Application
     * 0x1eaf	0x0004	Leaflabs Maple
     * 0x1a86	0x7523	CH 34x
     * 0x1a86	0x5523	CH 34x
     * 0x4348	0x5523	CH 34x
     *
     * @param deviceVID Arduino Vendor ID
     * @return true if it is an arduino, false otherwise
     */
    public boolean isArduino(int deviceVID) {
        switch (deviceVID) {
            case 0x10c4:
            case 0x067b:
            case 0x0403:
            case 0x2341:
            case 0x16c0:
            case 0x03eb:
            case 0x1eaf:
            case 0x1a86:
            case 0x4348:
                return true;

        }
        return false;
    }

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();

                if (isArduino(deviceVID)) {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    consoleAppend("Você não conectou um arduino. Device ID: " + Integer.toHexString(deviceVID) + "\n");
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        } else {
            consoleAppend("Não há dispositivo USB conectado!\n");
        }


    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        serialPort.write(string.getBytes());
        consoleAppend("\nDado Enviado : " + string + "\n");

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        if (serialPort != null) {
            serialPort.close();
        }
        consoleAppend("\n----------------------------\n");
        consoleAppend("Conexão serial fechada! \n");
        consoleAppend("Aguardando...\n");

    }

    public void onClickClear(View view) {
        consoleFragment.clear();
    }

    private void updateMap(double gps1Lat, double gps1Lng, double gps2Lat, double gps2Lng) {

        Location location = getMyLocation();
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();


            final LatLng garatea = new LatLng(gps1Lat, gps1Lng);
            Location garateaLoc = new Location(LocationManager.GPS_PROVIDER);
            garateaLoc.setLatitude(garatea.latitude);
            garateaLoc.setLongitude(garatea.longitude);

            final LatLng garatea2 = new LatLng(gps2Lat, gps2Lng);
            Location garatea2Loc = new Location(LocationManager.GPS_PROVIDER);
            garatea2Loc.setLatitude(garatea2.latitude);
            garatea2Loc.setLongitude(garatea2.longitude);


            final LatLng myLocation = new LatLng(latitude, longitude);

            final float distance = location.distanceTo(garateaLoc);
            final float distance2 = location.distanceTo(garatea2Loc);


            double midlat = (garatea.latitude + myLocation.latitude) / 2;
            double midlng = (garatea.longitude + myLocation.longitude) / 2;

            final LatLng center = new LatLng(midlat, midlng);

            double midlat2 = (garatea2.latitude + myLocation.latitude) / 2;
            double midlng2 = (garatea2.longitude + myLocation.longitude) / 2;

            final LatLng center2 = new LatLng(midlat2, midlng2);


            final LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(myLocation).include(garatea).include(garatea2);

            if (mMap != null) {
                mMap.clear();

                mMap.addMarker(new MarkerOptions().position(garatea).title("GPS1 Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                mMap.addMarker(new MarkerOptions().position(garatea2).title("GPS2 Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                mMap.addMarker(new MarkerOptions().position(myLocation).title("My Location"));
                Utils.addText(DashboardActivity.this.getApplicationContext(), mMap, center, distance + "", 0, 12, Color.BLUE);
                Utils.addText(DashboardActivity.this.getApplicationContext(), mMap, center2, distance + "", 0, 12, Color.GREEN);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
            }


        } else {
            consoleAppend("Não foi possível obter localização atual!\n");
        }


    }

    private void consoleAppend(CharSequence text) {
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                consoleFragment.updateConsole(ftext);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);

    }


}
