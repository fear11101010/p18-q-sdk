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

    String routeStr = "[{\"id\":2,\"numberOfRoute\":1,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"routeName\":\"HR Transport\",\"numberOfStoppage\":9,\"isActive\":true,\"lastUpdateAt\":\"2024-09-15\",\"effectiveAt\":\"2024-09-15\",\"expirayAt\":\"2024-12-31\",\"isFlatFare\":false,\"isCircularRoute\":true,\"stations\":[{\"id\":18,\"stationName\":\"Sonargaon Railway Crossing\",\"stationNameBng\":\"সোনারগাঁও রেল ক্রসিং\",\"stationCode\":\"8C1E\",\"routeId\":2,\"stationOrderNo\":30,\"latitude\":23.75166667,\"longitude\":90.39777778},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438}]}]";
    String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
    String ROUTE = "[{\"id\":2,\"numberOfRoute\":1,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"routeName\":\"HR Transport\",\"numberOfStoppage\":9,\"isActive\":true,\"lastUpdateAt\":\"2024-09-15\",\"effectiveAt\":\"2024-09-15\",\"expiryAt\":\"2030-12-31\",\"isFlatFare\":false,\"isCircularRoute\":true,\"circularDirection\":\"DESC\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}]}]";
    String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
    String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"15\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

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