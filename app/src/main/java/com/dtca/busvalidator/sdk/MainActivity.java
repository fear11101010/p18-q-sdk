package com.dtca.busvalidator.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;
import com.dtca.busvalidator.busvalidatorsdk.Sam;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.model.RideAndAlight;
import com.dtca.busvalidator.busvalidatorsdk.model.TransactionData;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.SamSyntaxError;
import com.dtca.busvalidator.busvalidatorsdk.model.interfac.DataInterface;
import com.google.gson.Gson;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class MainActivity extends Activity {

    private Button poll, read, ride, alight;
    FelicaCard felicaCard;
    Utils utils;

    String routeStr = "";
    String FARE_MATRIX = "";
    String ROUTE = "";
    String DEVICE_INFO = "";
    String MASTER_CONFIG_DATA = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Utils.initializeReader(this);
        Utils.openSerialReader();
        utils = Utils.getInstance();

        Utils.initAppDatabase(this);
        utils.initMasterConfig(MASTER_CONFIG_DATA);
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        initUI();
        Sam sam = Sam.getInstance(2, this);
        try {
            sam.initSam();
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SamSyntaxError e) {
            throw new RuntimeException(e);
        }
        felicaCard = FelicaCard.getInstance(sam);
    }

    void initUI() {
        poll = findViewById(R.id.poll);
        read = findViewById(R.id.read);
        ride = findViewById(R.id.ride);
        alight = findViewById(R.id.alight);
        // set listener
        poll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Executors.newSingleThreadExecutor().execute(() -> {

                    while (true) {
                        try {
                            System.out.println("Start Time :" + LocalDateTime.now());
                            felicaCard.detectFelicaCard();

                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                });
            }
        });
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        System.out.println("Start Time :" + LocalDateTime.now());

                        // Do database & card work on background thread
                        felicaCard.detectFelicaCard();

                        felicaCard.readCard(new DataInterface() {
                            @Override
                            public void receiveTransactionData(TransactionData transactionData) {
                                // You can update UI here if needed
//                                System.out.println("Card balance: "+transactionData.getSvBalance());
//                                new Handler(Looper.getMainLooper()).post(() -> {
//                                    // update UI with transactionData if needed
//                                });
                            }
                        });
                        System.out.println("Card balance: " + felicaCard.getBalance());


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        ride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {

                        // Do database & card work on background thread
                        felicaCard.detectFelicaCard();

                        felicaCard.readCard(new DataInterface() {
                            @Override
                            public void receiveTransactionData(TransactionData transactionData) {
                                // You can update UI here if needed
//                                System.out.println("Card balance: "+transactionData.getSvBalance());
//                                new Handler(Looper.getMainLooper()).post(() -> {
//                                    // update UI with transactionData if needed
//                                });
                            }


                        });
                        long st1 = System.currentTimeMillis();
                        RideAndAlight rideAndAlight = new RideAndAlight(felicaCard);
                        rideAndAlight.setType(RideAndAlight.Type.RIDE);
                        rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
                        rideAndAlight.setFareMatrix(utils.fareMatrix);
                        rideAndAlight.setStation(new Gson().toJson(utils.fareMatrix.getStations().get(0)));

                        rideAndAlight.writeData(new DataInterface() {
                            @Override
                            public void receiveTransactionData(TransactionData paramTransactionData) {
//                                new Handler(Looper.getMainLooper()).post(() -> {
//                                    // update UI with paramTransactionData if needed
//                                });
                            }


                        });
                        System.out.println("#RD>>> Card Write Time: " + (System.currentTimeMillis() - st1));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
        alight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                System.out.println("Alight");

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
//                        System.out.println("Start Time :" + LocalDateTime.now());

                        // Do database & card work on background thread
                        felicaCard.detectFelicaCard();

                        felicaCard.readCard(new DataInterface() {
                            @Override
                            public void receiveTransactionData(TransactionData transactionData) {
                                // You can update UI here if needed
//                                System.out.println("Card balance: "+transactionData.getSvBalance());
//                                new Handler(Looper.getMainLooper()).post(() -> {
//                                    // update UI with transactionData if needed
//                                });
                            }


                        });
                        RideAndAlight rideAndAlight = new RideAndAlight(felicaCard);
                        rideAndAlight.setType(RideAndAlight.Type.ALIGHT);
                        rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
                        rideAndAlight.setFareMatrix(utils.fareMatrix);
                        rideAndAlight.setStation(new Gson().toJson(utils.fareMatrix.getStations().get(3)));

                        rideAndAlight.writeData(new DataInterface() {
                            @Override
                            public void receiveTransactionData(TransactionData paramTransactionData) {
//                                new Handler(Looper.getMainLooper()).post(() -> {
//                                    // update UI with paramTransactionData if needed
//                                });
                            }


                        });
//                        System.out.println("Card balance: "+felicaCard.getBalance());


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }


   /* private void rideOrAlight() throws InterruptedException {
        Log.d("looperLog", "rideOrAlight: ");

            Log.d("looper", "run: ");
            try {
                MyApplication myApplication = (MyApplication) requireActivity().getApplication();
                RideAndAlight rideAndAlight = new RideAndAlight(myApplication.getFelicaCard());
                if (myApplication.getFelicaCard().isStatusAlight()) {
                    rideAndAlight.setType(RideAndAlight.Type.RIDE);
                    myApplication.setType(RideAndAlight.Type.RIDE);
                } else {
                    rideAndAlight.setType(RideAndAlight.Type.ALIGHT);
                    myApplication.setType(RideAndAlight.Type.ALIGHT);
                }
                rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
                rideAndAlight.setFareMatrix(myApplication.getUtils().fareMatrix);
                if (rideAndAlight.getType() == RideAndAlight.Type.RIDE) {
                    rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(0)));
                } else if (rideAndAlight.getType() == RideAndAlight.Type.ALIGHT) {
                    rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(5)));
                }
                try {
                    rideAndAlight.writeData((transactionData) -> {
                        Log.d("transaction", new Gson().toJson(transactionData));
                        requireActivity().runOnUiThread(() -> {
                            NavHostFragment.findNavController(FirstFragment.this)
                                    .navigate(R.id.action_FirstFragment_to_SecondFragment);
                        });
                    });
                } catch (Exception e) {
                    Log.d("child error", Objects.requireNonNull(e.getMessage()));
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.d("parent error", Objects.requireNonNull(e.getMessage()));
                e.printStackTrace();
            }





        try {
            MyApplication myApplication = (MyApplication) requireActivity().getApplication();
            RideAndAlight rideAndAlight = new RideAndAlight(myApplication.getFelicaCard());
            if (myApplication.getFelicaCard().isStatusAlight()) {
                rideAndAlight.setType(RideAndAlight.Type.RIDE);
                myApplication.setType(RideAndAlight.Type.RIDE);
            } else {
                rideAndAlight.setType(RideAndAlight.Type.ALIGHT);
                myApplication.setType(RideAndAlight.Type.ALIGHT);
            }
            rideAndAlight.setDirection(RideAndAlight.Direction.UPSTREAM);
            rideAndAlight.setFareMatrix(myApplication.getUtils().fareMatrix);
            if (rideAndAlight.getType() == RideAndAlight.Type.RIDE) {
                rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(0)));
            } else if (rideAndAlight.getType() == RideAndAlight.Type.ALIGHT) {
                rideAndAlight.setStation(new Gson().toJson(myApplication.getUtils().fareMatrix.getStations().get(5)));
            }
            try {
                rideAndAlight.writeData((transactionData) -> {
                    Log.d("transaction", new Gson().toJson(transactionData));
                    if (rideAndAlight.getType() == RideAndAlight.Type.ALIGHT) {
                        Log.d("fare", rideAndAlight.getFare().toString());
                    }
                    requireActivity().runOnUiThread(() -> {
                        NavHostFragment.findNavController(FirstFragment.this)
                                .navigate(R.id.action_FirstFragment_to_SecondFragment);
                    });
                });
            } catch (Exception e) {
                Log.d("child error", Objects.requireNonNull(e.getMessage()));
                if (e instanceof SamSyntaxError) {
                    myApplication.getSam().initSam();
                }
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.d("parent error", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
        }
    }*/


}