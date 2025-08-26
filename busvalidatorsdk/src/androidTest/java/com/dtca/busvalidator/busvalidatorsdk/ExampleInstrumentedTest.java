package com.dtca.busvalidator.busvalidatorsdk;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.dtca.busvalidator.busvalidatorsdk.db.DatabaseHelper;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.BlackListEntity;
import com.dtca.busvalidator.busvalidatorsdk.helper.ServiceCode;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.helper.ValidateCard;
import com.dtca.busvalidator.busvalidatorsdk.model.FelicaCardDetail;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.MasterConfigName;
import com.dtca.busvalidator.busvalidatorsdk.model.RideAndAlight;
import com.dtca.busvalidator.busvalidatorsdk.model.StoredLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.TransactionData;
import com.dtca.busvalidator.busvalidatorsdk.model.TransactionHistory;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardReadException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.FelicaMutualAuthException;
import com.dtca.busvalidator.busvalidatorsdk.model.extra.Entry;
import com.dtca.busvalidator.busvalidatorsdk.model.extra.Exit;
import com.dtca.busvalidator.busvalidatorsdk.model.interfac.DataInterface;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    TextToSpeech tts = null;
    private boolean isReady = false;

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.dtca.busvalidator.busvalidatorsdk", appContext.getPackageName());
    }

    @Test
    public void insertBlackListData() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        appContext.getResources().openRawResource(R.raw.blacklist_obj);
        File outputFile = new File(appContext.getFilesDir(), "black_list.dat");
        try (InputStream inputStream = appContext.getResources().openRawResource(R.raw.blacklist_obj);
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

    //    @Test
    /*public void parseBlackListFile() throws IOException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try {
            // Example InputStream (could be from any source, like a network or file)
            InputStream inputStream = appContext.getResources().openRawResource(R.blacklist_obj);

            // Destination File
            File file = new File(appContext.getFilesDir().getAbsolutePath(),"blacklist_file.dat");

            if(file.exists() && file.delete()){
                System.out.println("file deleted");
            }

            // Convert InputStream to File
            try(FileOutputStream fileOutputStream = new FileOutputStream(file)){
                byte[] bytes = new byte[1096];
                int byteReads;

                while ((byteReads = inputStream.read(bytes)) != -1){
                    fileOutputStream.write(bytes,0,byteReads);
                }
            }

            System.out.println("InputStream has been written to file successfully!");

            Utils.readBlackListFile(appContext,file);

        } catch (IOException e) {
            e.printStackTrace();
        }

//        appContext.getResources().openRawResource(R.raw.blacklist_obj);
    }*/
    @Test
    public void fetchAllBlackListFromDB() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(appContext.getFilesDir().getAbsolutePath(), "blacklist_file.dat");

        if (!file.exists()) {
            System.out.println("file does not exists");
            return;
        }
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(appContext);
        List<BlackListEntity> blackListEntities = databaseHelper.fetchAllBlacklistData();
        for (BlackListEntity blackListEntity : blackListEntities) {
            System.out.println(new Gson().toJson(blackListEntity));
        }
    }

    @Test
    public void getUID() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        int i = Utils.openSerialReader();
        Log.d("UID", Objects.requireNonNull(Utils.getDeviceSerialNo()));
        ;
    }

    @Test
    public void bitsToYear() {
        // Example binary representation of a year (for 2024 in binary)
        String binaryString = "0011000"; // 2024 in binary

        // Convert binary string to integer (base 2)
        int year = Integer.parseInt(binaryString, 2);

        // Print the result
        System.out.println("The year is: " + year);
    }

    @Test
    public void serviceCodeCheck() {

        // Print the result
        System.out.println("ride service codes: " + Arrays.toString(ServiceCode.RIDE.getCodes()));
        System.out.println("alight service codes: " + Arrays.toString(ServiceCode.ALIGHT.getCodes()));
        System.out.println("entry service codes: " + Arrays.toString(ServiceCode.ENTRY.getCodes()));
        System.out.println("exit service codes: " + Arrays.toString(ServiceCode.EXIT.getCodes()));
    }

    @Test
    public void getTranHistory() throws Exception {

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Print the result
        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        felicaCard.detectFelicaCard();

        List<TransactionHistory> transactionHistories = felicaCard.getTransactionHistory();
        System.out.println("transaction history" + transactionHistories.size());
    }

    @Test
    public void checkRideValidation() throws Exception {
        String FARE_MATRIX = "{\"routeId\":4,\"routeName\":\"Dhaka Line\",\"isFlatFare\":false,\"isCircular\":false,\"circularDirection\":null,\"numberOfStoppage\":8,\"operatorCode\":\"0A0B\",\"routeOrderNo\":3,\"tripCountStationCode\":\"0A01\",\"stations\":[{\"id\":45,\"stationName\":\"Shibbari\",\"stationNameBng\":\"শিববাড়ি\",\"stationCode\":\"0A01\",\"isTripCountStation\":true,\"routeId\":4,\"stationOrderNo\":1,\"latitude\":23.99753862504199,\"longitude\":90.4185770932535,\"audioTrackName\":\"Shibbari.mp3\",\"audioTrackNameBng\":\"Shibbari.mp3\"},{\"id\":46,\"stationName\":\"Gazipur Chowrasta\",\"stationNameBng\":\"গাজিপুর চৌরাস্তা \",\"stationCode\":\"0A05\",\"isTripCountStation\":false,\"routeId\":4,\"stationOrderNo\":5,\"latitude\":23.990323426272532,\"longitude\":90.38574517791083,\"audioTrackName\":\"GazipurChowrasta.mp3\",\"audioTrackNameBng\":\"GazipurChowrasta.mp3\"},{\"id\":47,\"stationName\":\"Board Bazar\",\"stationNameBng\":\"বোর্ড বাজার\",\"stationCode\":\"0A0A\",\"isTripCountStation\":false,\"routeId\":4,\"stationOrderNo\":10,\"latitude\":23.945245887215563,\"longitude\":90.38283482208917,\"audioTrackName\":\"BoardBazar.mp3\",\"audioTrackNameBng\":\"BoardBazar.mp3\"},{\"id\":48,\"stationName\":\"College Gate\",\"stationNameBng\":\"কলেজ গেট\",\"stationCode\":\"0A0E\",\"isTripCountStation\":false,\"routeId\":4,\"stationOrderNo\":15,\"latitude\":23.910286313474224,\"longitude\":90.3975030932535,\"audioTrackName\":\"CollegeGate.mp3\",\"audioTrackNameBng\":\"CollegeGate.mp3\"},{\"id\":49,\"stationName\":\"Airport\",\"stationNameBng\":\"বিমানবন্দর\",\"stationCode\":\"0A14\",\"isTripCountStation\":false,\"routeId\":4,\"stationOrderNo\":20,\"latitude\":23.85103688528258,\"longitude\":90.40812045964259,\"audioTrackName\":\"Airport.mp3\",\"audioTrackNameBng\":\"Airport.mp3\"},{\"id\":50,\"stationName\":\"Farmgate\",\"stationNameBng\":\"ফার্মগেট\",\"stationCode\":\"0A19\",\"isTripCountStation\":false,\"routeId\":4,\"stationOrderNo\":25,\"latitude\":23.757275565718352,\"longitude\":90.39010472883585,\"audioTrackName\":\"Farmgate.mp3\",\"audioTrackNameBng\":\"Farmgate.mp3\"},{\"id\":51,\"stationName\":\"Shahbag\",\"stationNameBng\":\"শাহবাগ\",\"stationCode\":\"0A1E\",\"isTripCountStation\":false,\"routeId\":4,\"stationOrderNo\":30,\"latitude\":23.739156136802507,\"longitude\":90.39572309325352,\"audioTrackName\":\"Shahbag.mp3\",\"audioTrackNameBng\":\"Shahbag.mp3\"},{\"id\":52,\"stationName\":\"Gulistan\",\"stationNameBng\":\"গুলিস্তান\",\"stationCode\":\"0A24\",\"isTripCountStation\":false,\"routeId\":4,\"stationOrderNo\":35,\"latitude\":23.72277638912029,\"longitude\":90.41026160674649,\"audioTrackName\":\"Gulistan.mp3\",\"audioTrackNameBng\":\"Gulistan.mp3\"}],\"fareMatrix\":{\"0A01\":{\"0A01\":0,\"0A05\":15,\"0A0A\":30,\"0A0E\":45,\"0A14\":70,\"0A19\":115,\"0A1E\":130,\"0A24\":140,\"maxFareUpStream\":140,\"maxFareDownStream\":0},\"0A05\":{\"0A01\":15,\"0A05\":0,\"0A0A\":20,\"0A0E\":30,\"0A14\":55,\"0A19\":105,\"0A1E\":115,\"0A24\":130,\"maxFareUpStream\":130,\"maxFareDownStream\":15},\"0A0A\":{\"0A01\":30,\"0A05\":20,\"0A0A\":0,\"0A0E\":15,\"0A14\":40,\"0A19\":85,\"0A1E\":95,\"0A24\":110,\"maxFareUpStream\":110,\"maxFareDownStream\":30},\"0A0E\":{\"0A01\":45,\"0A05\":30,\"0A0A\":15,\"0A0E\":0,\"0A14\":25,\"0A19\":70,\"0A1E\":80,\"0A24\":95,\"maxFareUpStream\":95,\"maxFareDownStream\":45},\"0A14\":{\"0A01\":70,\"0A05\":55,\"0A0A\":40,\"0A0E\":25,\"0A14\":0,\"0A19\":45,\"0A1E\":55,\"0A24\":70,\"maxFareUpStream\":70,\"maxFareDownStream\":70},\"0A19\":{\"0A01\":115,\"0A05\":105,\"0A0A\":85,\"0A0E\":70,\"0A14\":45,\"0A19\":0,\"0A1E\":15,\"0A24\":25,\"maxFareUpStream\":25,\"maxFareDownStream\":115},\"0A1E\":{\"0A01\":130,\"0A05\":115,\"0A0A\":95,\"0A0E\":80,\"0A14\":55,\"0A19\":10,\"0A1E\":0,\"0A24\":15,\"maxFareUpStream\":15,\"maxFareDownStream\":130},\"0A24\":{\"0A01\":140,\"0A05\":130,\"0A0A\":110,\"0A0E\":95,\"0A14\":70,\"0A19\":25,\"0A1E\":15,\"0A24\":0,\"maxFareUpStream\":0,\"maxFareDownStream\":140}}}";

        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":13,\"deviceSerialNumber\":\"00012700202501000016\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"8C1C\",\"equipmentLocationNumber\":\"0111\",\"passkey\":\"$2a$10$CH5zk2aV8k5mqMFN43KeKOXkR5VrQ6szsk1RZGlhTC5SD7/evvcEO\",\"pairedId\":null,\"pairedEquipmentLocationNumber\":[\"0201\"],\"ipAddress\":null,\"port\":null,\"downloadPath\":null,\"uploadPath\":null}";

        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"15\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Utils.openSerialReader();
        // Print the result
        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        felicaCard.detectFelicaCard();
        felicaCard.mutualAuthWithFelicaCard();
        FelicaCard felicaCard1 = felicaCard.readData();
        FelicaCardDetail f = felicaCard1.getFelicaCardDetail();
        boolean b = ValidateCard.isSameDate(f.getStoredLogInformation());
        /*if (ValidateCard.checkServiceIdIsEligibleForRide(f.getGateAccessLogInformation()) ||
                (ValidateCard.isStatusRide(f.getGateAccessLogInformation().getStatusFlag()) &&
                ValidateCard.isSameBus(f.getGateAccessLogInformation().getCurrentEquipmentLocationNumber()) &&
                ValidateCard.isSameDate(f.getStoredLogInformation(),Utils.byteToHex()) && ValidateCard.isSameRoute("") &&
                        ValidateCard.isSameStation(f.getStoredLogInformation().getPlace1(),"") &&
                        ValidateCard.isCircularRoute(""))
        ) {
            System.out.println("Eligible for ride");
        } */
        if (
//                !ValidateCard.isSameRoute(station.getStationCode(),Utils.byteToHex(this.storedLogInformation.getPlace1())) ||
                !ValidateCard.isSameRoute("0A24", Utils.byteToHex(f.getGateAccessLogInformation().getCurrentStationCode())) ||
                        !ValidateCard.isSameBus(f.getGateAccessLogInformation().getCurrentEquipmentLocationNumber()) ||
//                !ValidateCard.isSameDate(this.storedLogInformation) ||
                        !ValidateCard.isSameDate(f.getGateAccessLogInformation()) ||
                        ValidateCard.isStatusAlight(f.getGateAccessLogInformation().getStatusFlag()) ||
                        ValidateCard.isGreaterThenTime(f.getGateAccessLogInformation(), MasterConfigName.ALIGHT_EXPIRY_TIME)
        ) {
            System.out.println("Eligible for ride");
        } else {
            System.out.println("Not eligible for ride");
        }
    }

    @Test
    public void playVoice() throws Exception {

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Print the result
        /*String s = "Hello"; // UTF-8 encoded input

        // Convert the UTF-8 encoded string to a byte array
//        byte[] utf8Bytes = utf8String.getBytes(StandardCharsets.UTF_8);

        // Convert the UTF-8 byte array back to a String (decoding UTF-8)
//        String decodedString = new String(utf8Bytes, StandardCharsets.UTF_8);

        // Convert the decoded string to UCS2 (UTF-16BE encoding)
        byte[] ucs2Bytes = s.getBytes(StandardCharsets.UTF_16BE);

        Sam sam = Sam.getInstance(3,appContext);
        sam.initSam();
        String[] result = BasicOper.dc_TtsVoiceConfig(0x01,0x80).split("\\|");
        if(result[0].equals("0000")){
            System.out.println("config successfully");
        } else{
            System.out.println("result------"+result[0]);
        }

        result = BasicOper.dc_TtsVoicePlay(0x01,ucs2Bytes).split("\\|");
        if(result[0].equals("0000")){
            System.out.println("speaker working properly");
        } else{
            System.out.println("result------"+result[0]);
        }*/
        tts = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set the language (you can change the locale as per your need)
                int result = tts.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported or missing");
                } else {
                    isReady = true;
                    Log.d("TTS", "TTS is ready");
                    speakOut("Hello World");
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
//        tts.speak("你好世界", TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void speakOut(String text) {
        tts.speak("Set Language and Speak: Ensure that you set the language correctly for the custom TTS engine. Some engines may not support all languages or voices, so it’s essential to verify that the selected language is supported.", TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Test
    public void readFare() throws Exception {
        String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"10\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        File outputFile = new File(appContext.getFilesDir(), "black_list.dat");
//        try (InputStream inputStream = appContext.getResources().openRawResource(R.raw.blacklist_obj);
//             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
//
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            Utils.readBlackListFile(appContext,outputFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Utils.openSerialReader();
//        Sam sam = Sam.getInstance(3,appContext);
//        sam.initSam();
//        FelicaCard felicaCard = FelicaCard.getInstance(sam);
//        felicaCard.detectFelicaCard();
//        felicaCard.readCard(transactionData -> Log.d("TransactionData",new Gson().toJson(transactionData)));
//        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        long sT = System.currentTimeMillis();

        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        long sT = System.currentTimeMillis();
        felicaCard.detectFelicaCard();
        long eT = System.currentTimeMillis();
        System.out.println("time---->" + (eT - sT));
//        felicaCard.readCardForTransactionHistory();
        felicaCard.readCard(transactionData -> {
            // nothing to do
        });
        GateAccessLogInformation accessLogInformation = felicaCard.getFelicaCardDetail().getGateAccessLogInformation();
        String s = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag());

//        Entry entry = new Entry(felicaCard);
//        entry.executeEntry();
//        felicaCard.detectFelicaCard();
//        List<TransactionHistory> transactionHistories = felicaCard.getTransactionHistoryWithoutAuth();
//        System.out.println(new Gson().toJson(transactionHistories));
//        long eT = System.currentTimeMillis();
//        System.out.println("time---->"+(eT-sT));
//          boolean b = ValidateCard.isSameRoute("0a05", Utils.byteToHex(felicaCard.getFelicaCardDetail().getStoredLogInformation().getPlace1())) &&
//                  !ValidateCard.isSameStation(felicaCard.getFelicaCardDetail().getStoredLogInformation().getPlace1(), "0a05");
//        int v = Utils.convertTwosComplementByteArrayToLittleIndian(felicaCard.getFelicaCardDetail().getStoredLogInformation().getCardBalance(),3);
//        boolean b = ValidateCard.isGreaterThenTime(felicaCard.getFelicaCardDetail().getGateAccessLogInformation(), MasterConfigName.ALIGHT_EXPIRY_TIME);
//        ValidateCard.isFirstIssue(felicaCard.getFelicaCardDetail().getStoredLogInformation().getStoredValueLogId());
//        ValidateCard.isCardBlacklisted(felicaCard.getFelicaCardDetail().getAttributeInfo().getCardControlCode());
//        System.out.println("FARE----->"+Utils.getInstance().getFare(felicaCard.getFelicaCardDetail().getStoredLogInformation()));
//        System.out.println("validation----->"+b);
    }

    @Test
    public void mrtExit() throws Exception {
        String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"10\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Utils.openSerialReader();

        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        long sT = System.currentTimeMillis();
        felicaCard.detectFelicaCard();
        long eT = System.currentTimeMillis();
        System.out.println("time---->" + (eT - sT));
        felicaCard.readCard(transactionData -> {
            // nothing to do
        });
        String s = Utils.convertByteArrayToBit(felicaCard.getFelicaCardDetail().getGateAccessLogInformation().getStatusFlag());
        System.out.println("s------>" + s);
        Exit exit = new Exit(felicaCard);
        exit.executeExit();
        GateAccessLogInformation accessLogInformation = felicaCard.getFelicaCardDetail().getGateAccessLogInformation();
        String s1 = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag());

        System.out.println("s1------>" + s1);

    }

    @Test
    public void mrtEntry() throws Exception {
        String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"10\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Utils.openSerialReader();

        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        long sT = System.currentTimeMillis();
        felicaCard.detectFelicaCard();
        long eT = System.currentTimeMillis();
        System.out.println("time---->" + (eT - sT));
        felicaCard.readCard(transactionData -> {
            // nothing to do
        });
        String s = Utils.convertByteArrayToBit(felicaCard.getFelicaCardDetail().getGateAccessLogInformation().getStatusFlag());
        System.out.println("s------>" + s);

        Entry entry = new Entry(felicaCard);
        entry.executeEntry();
        GateAccessLogInformation accessLogInformation = felicaCard.getFelicaCardDetail().getGateAccessLogInformation();
        String s1 = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag());

        System.out.println("s1------>" + s1);

    }

    @Test
    public void readTransaction() throws Exception {
        String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000016\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"10\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Utils.initAppDatabase(appContext);
        long st = System.currentTimeMillis();
        Utils.openSerialReader();
        long et = System.currentTimeMillis();
        System.out.println("openSerialReader_time------>" + ((double) (et - st)) / 1000);

        Sam sam = Sam.getInstance(3, appContext);
        st = System.currentTimeMillis();
        sam.initSam();
        et = System.currentTimeMillis();
        System.out.println("sam_init_time------>" + ((double) (et - st)) / 1000);

        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        st = System.currentTimeMillis();
        felicaCard.detectFelicaCard();
        et = System.currentTimeMillis();
        System.out.println("polling_time---->" + ((double) (et - st)) / 1000);

        st = System.currentTimeMillis();
        felicaCard.readCard(transactionData -> {
            // nothing
        });
        et = System.currentTimeMillis();
        System.out.println("card_read_time---->" + ((double) (et - st)) / 1000);
        RideAndAlight rideAndAlight = new RideAndAlight(felicaCard);
        rideAndAlight.setFareMatrix(Utils.getInstance().fareMatrix);
        rideAndAlight.setDirection( RideAndAlight.Direction.UPSTREAM);
        rideAndAlight.setStation(new Gson().toJson(Utils.getInstance().fareMatrix.getStations().get(0)));
        rideAndAlight.writeData(transactionData -> {
            Log.d("ride alight: ",new Gson().toJson(transactionData));
        });
        /*felicaCard.readCardForTransactionHistory(transactionData -> {
            // nothing
        });
        felicaCard.detectFelicaCard();
        List<TransactionHistory> transactionHistories = felicaCard.getTransactionHistoryWithoutAuth();
        System.out.println(new Gson().toJson(transactionHistories));*/
    }

    @Test
    public void validationCheck() throws Exception {
        String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"15\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long st = System.currentTimeMillis();
        Utils.openSerialReader();
        long et = System.currentTimeMillis();
        System.out.println("openSerialReader_time------>" + ((double) (et - st)) / 1000);

        Sam sam = Sam.getInstance(3, appContext);
        st = System.currentTimeMillis();
        sam.initSam();
        et = System.currentTimeMillis();
        System.out.println("sam_init_time------>" + ((double) (et - st)) / 1000);

        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        st = System.currentTimeMillis();
        felicaCard.detectFelicaCard();
        et = System.currentTimeMillis();
        System.out.println("polling_time---->" + ((double) (et - st)) / 1000);

        st = System.currentTimeMillis();
        felicaCard.readCard(transactionData -> {
            // nothing
        });

//        boolean b = ValidateCard.checkCardDirection(Utils.byteToHex(this.felicaCard.getIdi()), this.direction) && ValidateCard.isSameRoute(station.getStationCode(), Utils.byteToHex(this.storedLogInformation.getPlace1())) && ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber()) && ValidateCard.isSameDate(this.storedLogInformation) && !ValidateCard.isStatusAlight(this.gateAccessLogInformation.getStatusFlag()) && ValidateCard.checkCardDirection(Utils.byteToHex(this.felicaCard.getIdi()), this.direction) && !ValidateCard.isGreaterThenTime(this.gateAccessLogInformation, MasterConfigName.ALIGHT_EXPIRY_TIME);
//        System.out.println(new Gson().toJson(transactionHistories));


    }

    @Test
    public void writeWithoutMutualAuth() throws Exception {
        String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"15\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long st = System.currentTimeMillis();
        Utils.openSerialReader();
        long et = System.currentTimeMillis();
        System.out.println("openSerialReader_time------>" + ((double) (et - st)) / 1000);

        Sam sam = Sam.getInstance(3, appContext);
        st = System.currentTimeMillis();
        sam.initSam();
        et = System.currentTimeMillis();
        System.out.println("sam_init_time------>" + ((double) (et - st)) / 1000);

        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        st = System.currentTimeMillis();
        felicaCard.detectFelicaCard();
        et = System.currentTimeMillis();
        System.out.println("polling_time---->" + ((double) (et - st)) / 1000);

        st = System.currentTimeMillis();
        int i = felicaCard.readCard(transactionData -> {
            // nothing
        });
        if (i == 0) {
            throw new Exception("can not read");
        }
//        felicaCard.recharge();


//        boolean b = ValidateCard.checkCardDirection(Utils.byteToHex(this.felicaCard.getIdi()), this.direction) && ValidateCard.isSameRoute(station.getStationCode(), Utils.byteToHex(this.storedLogInformation.getPlace1())) && ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber()) && ValidateCard.isSameDate(this.storedLogInformation) && !ValidateCard.isStatusAlight(this.gateAccessLogInformation.getStatusFlag()) && ValidateCard.checkCardDirection(Utils.byteToHex(this.felicaCard.getIdi()), this.direction) && !ValidateCard.isGreaterThenTime(this.gateAccessLogInformation, MasterConfigName.ALIGHT_EXPIRY_TIME);
//        System.out.println(new Gson().toJson(transactionHistories));


    }

    @Test
    public void qrCodeReader() throws Exception {
        /*String FARE_MATRIX = "{\"routeId\":2,\"routeName\":\"HR Transport\",\"isFlatFare\":false,\"isCircular\":true,\"circularDirection\":\"DESC\",\"numberOfStoppage\":9,\"operatorCode\":\"0A0B\",\"routeOrderNo\":1,\"tripCountStationCode\":\"8C10\",\"stations\":[{\"id\":26,\"stationName\":\"Modhubag\",\"stationNameBng\":\"মধুবাগ\",\"stationCode\":\"8C0A\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":10,\"latitude\":23.76006692822424,\"longitude\":90.41051938497438},{\"id\":25,\"stationName\":\"Mohanagor\",\"stationNameBng\":\"মহানগর\",\"stationCode\":\"8C0D\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":13,\"latitude\":23.766170790911985,\"longitude\":90.41247259077545},{\"id\":24,\"stationName\":\"Rampura\",\"stationNameBng\":\"রামপুরা\",\"stationCode\":\"8C10\",\"isTripCountStation\":true,\"routeId\":2,\"stationOrderNo\":16,\"latitude\":23.767990002202243,\"longitude\":90.42181220076523},{\"id\":23,\"stationName\":\"Badda\",\"stationNameBng\":\"বাড্ডা\",\"stationCode\":\"8C11\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":17,\"latitude\":23.770341600170017,\"longitude\":90.42293872855763},{\"id\":22,\"stationName\":\"Police Plaza/Shooting Club\",\"stationNameBng\":\"পুলিশ প্লাজা/শুটিং ক্লাব\",\"stationCode\":\"8C13\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":19,\"latitude\":23.772594722358928,\"longitude\":90.41540156595897},{\"id\":21,\"stationName\":\"Kuni Para/Happy Homes\",\"stationNameBng\":\"কুনি পাড়া/হ্যাপি হোমস\",\"stationCode\":\"8C16\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":22,\"latitude\":23.767246995198775,\"longitude\":90.40919172485185},{\"id\":20,\"stationName\":\"Bou Bazar\",\"stationNameBng\":\"বউ বাজার\",\"stationCode\":\"8C19\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":25,\"latitude\":23.76179514298692,\"longitude\":90.4079015489616},{\"id\":19,\"stationName\":\"FDC\",\"stationNameBng\":\"এফডিসি\",\"stationCode\":\"8C1C\",\"isTripCountStation\":false,\"routeId\":2,\"stationOrderNo\":28,\"latitude\":23.755307238149342,\"longitude\":90.40194564575447}],\"fareMatrix\":{\"8C1E\":{\"8C1E\":0,\"8C1C\":20,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C1C\":{\"8C1E\":40,\"8C1C\":0,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C19\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":0,\"8C16\":20,\"8C13\":20,\"8C11\":25,\"8C10\":25,\"8C0D\":30,\"8C0A\":30,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C16\":{\"8C1E\":30,\"8C1C\":30,\"8C19\":40,\"8C16\":0,\"8C13\":20,\"8C11\":20,\"8C10\":20,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C13\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":25,\"8C16\":25,\"8C13\":0,\"8C11\":15,\"8C10\":15,\"8C0D\":25,\"8C0A\":25,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C11\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":0,\"8C10\":20,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C10\":{\"8C1E\":25,\"8C1C\":25,\"8C19\":20,\"8C16\":20,\"8C13\":20,\"8C11\":40,\"8C10\":0,\"8C0D\":20,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0D\":{\"8C1E\":20,\"8C1C\":20,\"8C19\":25,\"8C16\":25,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":0,\"8C0A\":20,\"maxFareUpStream\":40,\"maxFareDownStream\":40},\"8C0A\":{\"8C1E\":15,\"8C1C\":15,\"8C19\":20,\"8C16\":20,\"8C13\":25,\"8C11\":25,\"8C10\":25,\"8C0D\":40,\"8C0A\":0,\"maxFareUpStream\":40,\"maxFareDownStream\":40}}}";
        String ROUTE = "[\n" +
                "  {\n" +
                "    \"id\": 2,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 1,\n" +
                "    \"routeName\": \"HR Transport\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": true,\n" +
                "    \"circularDirection\": \"DESC\",\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 26,\n" +
                "        \"stationName\": \"Modhubag\",\n" +
                "        \"stationNameBng\": \"মধুবাগ\",\n" +
                "        \"stationCode\": \"8C0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.76006692822424,\n" +
                "        \"longitude\": 90.41051938497438,\n" +
                "        \"audioTrackName\": \"Modhubagh.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Modhubagh.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 25,\n" +
                "        \"stationName\": \"Mohanagor\",\n" +
                "        \"stationNameBng\": \"মহানগর\",\n" +
                "        \"stationCode\": \"8C0D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.765351113827776,\n" +
                "        \"longitude\": 90.41248137031155,\n" +
                "        \"audioTrackName\": \"Mohanogor.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Mohanogor.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 24,\n" +
                "        \"stationName\": \"Rampura\",\n" +
                "        \"stationNameBng\": \"রামপুরা\",\n" +
                "        \"stationCode\": \"8C10\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.767990002202243,\n" +
                "        \"longitude\": 90.42181220076523,\n" +
                "        \"audioTrackName\": \"Rampura.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Rampura.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 23,\n" +
                "        \"stationName\": \"Badda\",\n" +
                "        \"stationNameBng\": \"বাড্ডা\",\n" +
                "        \"stationCode\": \"8C11\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 17,\n" +
                "        \"latitude\": 23.770341600170017,\n" +
                "        \"longitude\": 90.42293872855763,\n" +
                "        \"audioTrackName\": \"Badda.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Badda.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 22,\n" +
                "        \"stationName\": \"Police Plaza/Shooting Club\",\n" +
                "        \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\",\n" +
                "        \"stationCode\": \"8C13\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 19,\n" +
                "        \"latitude\": 23.772594722358928,\n" +
                "        \"longitude\": 90.41540156595897,\n" +
                "        \"audioTrackName\": \"PolicePlaza.mp3\",\n" +
                "        \"audioTrackNameBng\": \"PolicePlaza.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 21,\n" +
                "        \"stationName\": \"Kuni Para/Happy Homes\",\n" +
                "        \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\",\n" +
                "        \"stationCode\": \"8C16\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 22,\n" +
                "        \"latitude\": 23.767246995198775,\n" +
                "        \"longitude\": 90.40919172485185,\n" +
                "        \"audioTrackName\": \"Kunipara.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Kunipara.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 20,\n" +
                "        \"stationName\": \"Bou Bazar\",\n" +
                "        \"stationNameBng\": \"বউ বাজার\",\n" +
                "        \"stationCode\": \"8C19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.76179514298692,\n" +
                "        \"longitude\": 90.4079015489616,\n" +
                "        \"audioTrackName\": \"BouBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BouBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 19,\n" +
                "        \"stationName\": \"FDC\",\n" +
                "        \"stationNameBng\": \"এফডিসি\",\n" +
                "        \"stationCode\": \"8C1C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 2,\n" +
                "        \"stationOrderNo\": 28,\n" +
                "        \"latitude\": 23.755307238149342,\n" +
                "        \"longitude\": 90.40194564575447,\n" +
                "        \"audioTrackName\": \"FDC.mp3\",\n" +
                "        \"audioTrackNameBng\": \"FDC.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 2,\n" +
                "    \"routeName\": \"Test Routes\",\n" +
                "    \"numberOfStoppage\": 16,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-09-15\",\n" +
                "    \"effectiveAt\": \"2024-09-15\",\n" +
                "    \"expiryAt\": \"2030-12-31\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 29,\n" +
                "        \"stationName\": \"Kudabo\",\n" +
                "        \"stationNameBng\": \"কুদাবো মাস্টার ফাইল আপডেট করার পরে\",\n" +
                "        \"stationCode\": \"9901\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.933893291665083,\n" +
                "        \"longitude\": 90.4369686254039,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 30,\n" +
                "        \"stationName\": \"Mazu khan Bazar\",\n" +
                "        \"stationNameBng\": \"1\",\n" +
                "        \"stationCode\": \"9902\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.922501279375602,\n" +
                "        \"longitude\": 90.43483156936317,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 31,\n" +
                "        \"stationName\": \"Niltoli Bridge\",\n" +
                "        \"stationNameBng\": \"2\",\n" +
                "        \"stationCode\": \"9903\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 3,\n" +
                "        \"latitude\": 23.91506637734127,\n" +
                "        \"longitude\": 90.42495945016276,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 32,\n" +
                "        \"stationName\": \"Silmun Catharsis Hospital\",\n" +
                "        \"stationNameBng\": \"3\",\n" +
                "        \"stationCode\": \"9904\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 4,\n" +
                "        \"latitude\": 23.910469377148864,\n" +
                "        \"longitude\": 90.4203611612448,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 33,\n" +
                "        \"stationName\": \"T & T Bazar\",\n" +
                "        \"stationNameBng\": \"4\",\n" +
                "        \"stationCode\": \"9905\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.90221438283074,\n" +
                "        \"longitude\": 90.41259616612712,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 34,\n" +
                "        \"stationName\": \"Station Road\",\n" +
                "        \"stationNameBng\": \"5\",\n" +
                "        \"stationCode\": \"9906\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 6,\n" +
                "        \"latitude\": 23.892614849584152,\n" +
                "        \"longitude\": 90.40187721482411,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 35,\n" +
                "        \"stationName\": \"Tongi Bazaar\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9907\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 7,\n" +
                "        \"latitude\": 23.884659148310973,\n" +
                "        \"longitude\": 90.40030007591837,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 36,\n" +
                "        \"stationName\": \"Housebuilding\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9908\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 8,\n" +
                "        \"latitude\": 23.874161975232333,\n" +
                "        \"longitude\": 90.40035372010313,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 37,\n" +
                "        \"stationName\": \"Azampur\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9909\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 9,\n" +
                "        \"latitude\": 23.868726655788983,\n" +
                "        \"longitude\": 90.40046100846507,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 38,\n" +
                "        \"stationName\": \"Rajlakshmi Bus Stop\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.863997546008818,\n" +
                "        \"longitude\": 90.4001391433799,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 39,\n" +
                "        \"stationName\": \"Jashimuddin\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990B\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 11,\n" +
                "        \"latitude\": 23.859160331868583,\n" +
                "        \"longitude\": 90.40116911163439,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 40,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990C\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 12,\n" +
                "        \"latitude\": 23.85122219963833,\n" +
                "        \"longitude\": 90.40762787093686,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 41,\n" +
                "        \"stationName\": \"Khilkhet\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990D\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 13,\n" +
                "        \"latitude\": 23.82859242037788,\n" +
                "        \"longitude\": 90.42028789748862,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 42,\n" +
                "        \"stationName\": \"Banani bus terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 14,\n" +
                "        \"latitude\": 23.7952629134371,\n" +
                "        \"longitude\": 90.40131777624502,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 43,\n" +
                "        \"stationName\": \"Mohakhali Bus Terminal\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"990F\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.77221561269043,\n" +
                "        \"longitude\": 90.40119412547811,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44,\n" +
                "        \"stationName\": \"DTCA\",\n" +
                "        \"stationNameBng\": \"\",\n" +
                "        \"stationCode\": \"9910\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 3,\n" +
                "        \"stationOrderNo\": 16,\n" +
                "        \"latitude\": 23.76545368901438,\n" +
                "        \"longitude\": 90.40713879847819,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 4,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 3,\n" +
                "    \"routeName\": \"Dhaka Line\",\n" +
                "    \"numberOfStoppage\": 8,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 45,\n" +
                "        \"stationName\": \"Shibbari\",\n" +
                "        \"stationNameBng\": \"শিববাড়ি\",\n" +
                "        \"stationCode\": \"0A01\",\n" +
                "        \"isTripCountStation\": true,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.99687615132476,\n" +
                "        \"longitude\": 90.41736904861601,\n" +
                "        \"audioTrackName\": \"Shibbari.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Shibbari.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 46,\n" +
                "        \"stationName\": \"Gazipur Chowrasta\",\n" +
                "        \"stationNameBng\": \"গাজিপুর চৌরাস্তা \",\n" +
                "        \"stationCode\": \"0A05\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 5,\n" +
                "        \"latitude\": 23.983848023251504,\n" +
                "        \"longitude\": 90.38143723889775,\n" +
                "        \"audioTrackName\": \"GazipurChowrasta.mp3\",\n" +
                "        \"audioTrackNameBng\": \"GazipurChowrasta.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 47,\n" +
                "        \"stationName\": \"Board Bazar\",\n" +
                "        \"stationNameBng\": \"বোর্ড বাজার\",\n" +
                "        \"stationCode\": \"0A0A\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 10,\n" +
                "        \"latitude\": 23.94284549038609,\n" +
                "        \"longitude\": 90.38359042256998,\n" +
                "        \"audioTrackName\": \"BoardBazar.mp3\",\n" +
                "        \"audioTrackNameBng\": \"BoardBazar.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 48,\n" +
                "        \"stationName\": \"College Gate\",\n" +
                "        \"stationNameBng\": \"কলেজ গেট\",\n" +
                "        \"stationCode\": \"0A0E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 15,\n" +
                "        \"latitude\": 23.91094212639669,\n" +
                "        \"longitude\": 90.39737459569268,\n" +
                "        \"audioTrackName\": \"CollegeGate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"CollegeGate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 49,\n" +
                "        \"stationName\": \"Airport\",\n" +
                "        \"stationNameBng\": \"বিমানবন্দর\",\n" +
                "        \"stationCode\": \"0A14\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 20,\n" +
                "        \"latitude\": 23.85124869632902,\n" +
                "        \"longitude\": 90.40765948957299,\n" +
                "        \"audioTrackName\": \"Airport.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Airport.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 50,\n" +
                "        \"stationName\": \"Farmgate\",\n" +
                "        \"stationNameBng\": \"ফার্মগেট\",\n" +
                "        \"stationCode\": \"0A19\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 25,\n" +
                "        \"latitude\": 23.75821329829975,\n" +
                "        \"longitude\": 90.39057058805568,\n" +
                "        \"audioTrackName\": \"Farmgate.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Farmgate.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 51,\n" +
                "        \"stationName\": \"Shahbag\",\n" +
                "        \"stationNameBng\": \"শাহবাগ\",\n" +
                "        \"stationCode\": \"0A1E\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 30,\n" +
                "        \"latitude\": 23.739538228393677,\n" +
                "        \"longitude\": 90.39607329047048,\n" +
                "        \"audioTrackName\": \"ShootingClub.mp3\",\n" +
                "        \"audioTrackNameBng\": \"ShootingClub.mp3\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 52,\n" +
                "        \"stationName\": \"Gulistan\",\n" +
                "        \"stationNameBng\": \"গুলিস্তান\",\n" +
                "        \"stationCode\": \"0A24\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 4,\n" +
                "        \"stationOrderNo\": 35,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": \"Gulistan.mp3\",\n" +
                "        \"audioTrackNameBng\": \"Gulistan.mp3\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 5,\n" +
                "    \"numberOfRoute\": 4,\n" +
                "    \"operatorCode\": \"0A0B\",\n" +
                "    \"routeOrderNo\": 4,\n" +
                "    \"routeName\": \"Raja Route\",\n" +
                "    \"numberOfStoppage\": 2,\n" +
                "    \"isActive\": true,\n" +
                "    \"lastUpdateAt\": \"2024-12-17\",\n" +
                "    \"effectiveAt\": \"2024-12-16\",\n" +
                "    \"expiryAt\": \"2030-12-30\",\n" +
                "    \"isFlatFare\": false,\n" +
                "    \"isCircularRoute\": false,\n" +
                "    \"circularDirection\": null,\n" +
                "    \"stations\": [\n" +
                "      {\n" +
                "        \"id\": 56,\n" +
                "        \"stationName\": \"BASA\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6601\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 1,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 57,\n" +
                "        \"stationName\": \"OFFICE\",\n" +
                "        \"stationNameBng\": null,\n" +
                "        \"stationCode\": \"6602\",\n" +
                "        \"isTripCountStation\": false,\n" +
                "        \"routeId\": 5,\n" +
                "        \"stationOrderNo\": 2,\n" +
                "        \"latitude\": 23.724593547206577,\n" +
                "        \"longitude\": 90.41188289833066,\n" +
                "        \"audioTrackName\": null,\n" +
                "        \"audioTrackNameBng\": null\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";
        String DEVICE_INFO = "{\"id\":1,\"deviceSerialNumber\":\"00012700202401000009\",\"activeFlag\":true,\"operatorCode\":\"0A0B\",\"equipmentClassificationCode\":\"42\",\"stationCode\":\"1011\",\"equipmentLocationNumber\":\"0101\",\"passkey\":\"$2a$10$Z9U.xysztj6Mi8e..z8V4exk74ocJqS7n6jA4nUawb.MsFo2wnw72\",\"pairedEquipmentLocationNumber\":[\"0102\"],\"ipAddress\":\"192.168.191.168\",\"port\":5000,\"downloadPath\":\"/data/export\",\"uploadPath\":\"/data\"}";
        String MASTER_CONFIG_DATA = "[{\"id\":1,\"configName\":\"MINIMUM_RIDE_BALANCE\",\"value\":\"10\",\"remarks\":\"minimum amount validations for ride in taka\"},{\"id\":2,\"configName\":\"MINIMUM_CANCEL_OF_ENTRY_TIME\",\"value\":\"15\",\"remarks\":\"time in minutes for valid cancel of entry \"},{\"id\":3,\"configName\":\"ALIGHT_EXPIRY_TIME\",\"value\":\"240\",\"remarks\":\"Time in minutes after this time; once a ride occurs, it won't wait for alight, and the next tap will be considered a ride instead of waiting for alight.\"}]";

        Utils utils = Utils.getInstance();
        utils.initializeFareMatrix(FARE_MATRIX);
        utils.initializeRouteList(ROUTE);
        utils.setDeviceInfo(DEVICE_INFO);
        utils.initMasterConfig(MASTER_CONFIG_DATA);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long st = System.currentTimeMillis();
        Utils.openSerialReader();
        long et = System.currentTimeMillis();
        System.out.println("openSerialReader_time------>"+((double)(et-st))/1000);

        Sam sam = Sam.getInstance(3,appContext);
        st = System.currentTimeMillis();
        sam.initSam();
        et = System.currentTimeMillis();
        System.out.println("sam_init_time------>"+((double)(et-st))/1000);

        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        st = System.currentTimeMillis();
        felicaCard.detectFelicaCard();
        et = System.currentTimeMillis();
        System.out.println("polling_time---->"+((double)(et-st))/1000);

        st = System.currentTimeMillis();
        felicaCard.readCard(new DataInterface() {
            @Override
            public void receiveTransactionData(TransactionData transactionData) {
                // nothing
            }
        });*/

        Utils.openSerialReader();
        QRCodeReader qrCodeReader = QRCodeReader.getInstance();
        qrCodeReader.startQrCodeScaner();
        String s = qrCodeReader.readQrCode();
        if (s == null) {
            System.out.println("Qrcodedata error");
        } else {
            System.out.println("q----->" + s);
        }

//        boolean b = ValidateCard.checkCardDirection(Utils.byteToHex(this.felicaCard.getIdi()), this.direction) && ValidateCard.isSameRoute(station.getStationCode(), Utils.byteToHex(this.storedLogInformation.getPlace1())) && ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber()) && ValidateCard.isSameDate(this.storedLogInformation) && !ValidateCard.isStatusAlight(this.gateAccessLogInformation.getStatusFlag()) && ValidateCard.checkCardDirection(Utils.byteToHex(this.felicaCard.getIdi()), this.direction) && !ValidateCard.isGreaterThenTime(this.gateAccessLogInformation, MasterConfigName.ALIGHT_EXPIRY_TIME);
//        System.out.println(new Gson().toJson(transactionHistories));


    }

    @Test
    public void getFare() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Utils.openSerialReader();
        // Print the result
        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        felicaCard.detectFelicaCard();
        felicaCard.mutualAuthWithFelicaCard();
        FelicaCard felicaCard1 = felicaCard.readData();
        FelicaCardDetail f = felicaCard1.getFelicaCardDetail();

        byte[] serviceCodes = new byte[4];
        serviceCodes[0] = 0x0C;
        serviceCodes[1] = 0x25;
        serviceCodes[2] = 0x01;
        serviceCodes[3] = 0x00;

        byte[] blocks = new byte[4];
        blocks[0] = (byte) 0x80;
        blocks[1] = 0x00;
        blocks[2] = (byte) 0x80;
        blocks[3] = 0x01;

        int ret = felicaCard.mutualAuthV2WithFeliCa((byte) 0x01, serviceCodes);
        if (ret == 0) {
            throw new FelicaMutualAuthException("unable to mutual auth");
        }
        byte[] data = new byte[256];
        int[] readLen = new int[1];
        ret = felicaCard.readDataBlock((byte) 2, blocks, readLen, data);
        if (ret == 0) {
            throw new CardReadException("unable to read card");
        }
        int len = readLen[0] - 3;
        byte[] b = Arrays.copyOfRange(data, 3, data.length);
        byte[] bytes = Arrays.copyOfRange(b, 0, len - 2);
        GateAccessLogInformation[] gateAccessLogInformations = new GateAccessLogInformation[2];
        int j = 0;
        for (int i = 0; i < bytes.length; i += 16) {
            if (j > 1) break;
            gateAccessLogInformations[j] = GateAccessLogInformation.generateData(
                    Arrays.copyOfRange(bytes, i, i + 16)
            );
            System.out.println("alight station: " + Utils.byteToHex(gateAccessLogInformations[j].getCurrentStationCode()));
            j++;
        }
    }

    @Test
    public void checkBalance() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Utils.openSerialReader();
        // Print the result
        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        felicaCard.detectFelicaCard();
        felicaCard.mutualAuthWithFelicaCard();
        FelicaCard felicaCard1 = felicaCard.readData();
        FelicaCardDetail f = felicaCard1.getFelicaCardDetail();

        int eBalance = Utils.convertTwosComplementByteArrayToLittleIndian(f.getEPurseInfo().getBinRemainingSV(), 4);
        System.out.println("e-balance: " + eBalance);
    }

    @Test
    public void tttt() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        byte[] bytes = Utils.hexToByte("08D22000329E880A010000860B000119");
        StoredLogInformation storedLogInformation = StoredLogInformation.generateData(bytes);
        String classificationCode = String.format("%02X", storedLogInformation.getServiceClassificationCode());
        String contextCode = String.format("%02X", storedLogInformation.getContextCode());
        int b = Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformation.getCardBalance(), 3);
        int i = Utils.byteArrayToInt(storedLogInformation.getStoredValueLogId());
        Log.d("length", "tttt: " + bytes.length);
    }

    @Test
    public void getTranHistoryWithoutAuth() throws Exception {


        Utils.openSerialReader();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Print the result
        Sam sam = Sam.getInstance(3, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);
        felicaCard.detectFelicaCard();
        LocalDateTime start = LocalDateTime.now();
        List<TransactionHistory> transactionHistories = felicaCard.getTransactionHistoryWithoutAuth();
        LocalDateTime end = LocalDateTime.now();

        System.out.println("time : " + Duration.between(start,end).toMillis());
        System.out.println("transaction history : " + transactionHistories.size());
    }
    @Test
    public void getLastRide() throws Exception {


        Utils.openSerialReader();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Print the result
        Sam sam = Sam.getInstance(2, appContext);
        sam.initSam();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);

        felicaCard.detectFelicaCard();
        felicaCard.readCard(transactionData -> {

        });
        StoredLogInformation storedLogInformation = felicaCard.getFelicaCardDetail().getStoredLogInformation();
        List<StoredLogInformation> storedLogInformationList = felicaCard.getFelicaCardDetail().getStoredLogInformationList();
        LocalDateTime start = LocalDateTime.now();
        int rechargeAmount = felicaCard.getCashbackAmount();
        LocalDateTime end = LocalDateTime.now();

        System.out.println("time : " + Duration.between(start,end).toMillis());
        System.out.println("recharge amount : " + rechargeAmount);
    }


    @Test
    public void readCard() throws Exception {



        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Print the result
        Sam sam = Sam.getInstance(2, appContext);
        Utils.openSerialReader();
        sam.initSam();
        /*FelicaCard felicaCard = FelicaCard.getInstance(sam);

        felicaCard.detectFelicaCard();
        felicaCard.readCard(transactionData -> {

        });*/
        LocalDateTime start = LocalDateTime.now();
//        int rechargeAmount = felicaCard.getCashbackAmount();
        FelicaCard felicaCard = FelicaCard.getInstance(sam);

        felicaCard.detectFelicaCard();
        felicaCard.readCard(transactionData -> {

        });
        LocalDateTime end = LocalDateTime.now();

        System.out.println("total read time : " + Duration.between(start,end).toMillis());
//        System.out.println("recharge amount : " + rechargeAmount);
    }


}