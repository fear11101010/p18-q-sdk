package com.dtca.busvalidator.busvalidatorsdk;

import static com.google.gson.internal.$Gson$Types.arrayOf;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dtca.busvalidator.busvalidatorsdk.db.DatabaseHelper;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_BLUETOOTH_CONNECT = 1001;
    DatabaseHelper databaseHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
//        setContentView(R.layout.busvalidatorsdk_main);
        databaseHelper = DatabaseHelper.getInstance(this.getApplicationContext());

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_CODE_BLUETOOTH_CONNECT);
            }
        }*/


        /*Utils utils = Utils.getInstance();

        Sam sam = Sam.getInstance(3,this);
        try {
            sam.initSam();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FelicaCard felicaCard = new FelicaCard(sam);
                    while (true){
                        try{
                            felicaCard.detectFelicaCard();
                            break;
                        }catch (CardNotFoundException e){
                            Log.d("card not found exception", Objects.requireNonNull(e.getMessage()));
                        }
                    }
                    try {
                        RideAndAlight rideAndAlight = new RideAndAlight(felicaCard);
                        rideAndAlight.setType(RideAndAlight.Type.RIDE);
                        rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
                        rideAndAlight.setStation(utils.fareMatrix.getStations().get(0));
                        rideAndAlight.writeData(MainActivity.this);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException |
                 IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }*/

    }

    /*@Override
    public void receiveTransactionData(TransactionData transactionData) {

    }*/

    /*public void sdkTest(View view) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                testFelica();
            }
        });
        thread.start();
    }*/

    /*private void testFelica() {

        try{
            Sam sam = new Sam(3,getApplicationContext());
            sam.initSam();

            FelicaCard felicaCard = new FelicaCard(sam);
            felicaCard.detectFelicaCard();

            RideAndAlight rideAndAlight = new RideAndAlight(felicaCard);
            rideAndAlight.setType(RideAndAlight.Type.ALIGHT);
            rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
            rideAndAlight.setFareMatrix(Utils.fareMatrix);
            rideAndAlight.setStation(Utils.fareMatrix.getStations().get(5));

            int i = rideAndAlight.writeData();
            */
    /*if(i!=0) Toast.makeText(getApplicationContext(),"ride data write successful",Toast.LENGTH_LONG).show();
            else Toast.makeText(getApplicationContext(),"ride data can not write successful",Toast.LENGTH_LONG).show();*//*
        }catch (Exception e){
            e.printStackTrace();
//            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }*/
}