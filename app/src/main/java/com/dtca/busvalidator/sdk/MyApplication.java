package com.dtca.busvalidator.sdk;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;
import com.dtca.busvalidator.busvalidatorsdk.Sam;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.model.RideAndAlight;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;
import lombok.Setter;

@Getter
public class MyApplication extends Application {
    private final String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
    private final String ROUTE = "[{\"id\":2,\"numberOfRoute\":1,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"routeName\":\"HR Transport\",\"numberOfStoppage\":9,\"isActive\":true,\"lastUpdateAt\":\"2024-09-15\",\"effectiveAt\":\"2024-09-15\",\"expiryAt\":\"2030-12-31\",\"isFlatFare\":false,\"isCircularRoute\":true,\"circularDirection\":\"DESC\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}]}]";
    private final String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
    private Sam sam;
    private FelicaCard felicaCard;
    @Setter
    private RideAndAlight.Type type;
    private Utils utils;

    @lombok.SneakyThrows
    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Utils.openSerialReader();
//                // set type
//                type = RideAndAlight.Type.RIDE;
//                // init sam
//                sam = Sam.getInstance(3, getApplicationContext());
//                long sTime = System.currentTimeMillis();
//                try {
//                    sam.initSam();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                long eTime = System.currentTimeMillis();
//                Log.d("sam_init_time", ((eTime - sTime)) + "");
//                // init felica card
//                felicaCard = FelicaCard.getInstance(sam);
//        this.felicaCard.detectFelicaCard();
//        this.felicaCard.recharge();

                // init utils;
                utils = Utils.getInstance();
//                utils.initializeFareMatrix(FARE_MATRIX);
//                utils.initializeRouteList(ROUTE);
//                utils.setDeviceInfo(DEVICE_INFO);
//                insertBlackListData();
            }
        }).start();

    }

    public void insertBlackListData() {
        // Context of the app under test.
        Context appContext = this;
        File outputFile = new File(appContext.getFilesDir(), "black_list.dat");
        try (InputStream inputStream = appContext.getResources().openRawResource(R.raw.blacklist_local_obj);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Utils.readBlackListFile(appContext, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
