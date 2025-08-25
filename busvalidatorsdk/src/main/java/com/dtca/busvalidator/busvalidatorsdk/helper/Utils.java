package com.dtca.busvalidator.busvalidatorsdk.helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.decard.NDKMethod.BasicOper;
import com.dtca.busvalidator.busvalidatorsdk.db.DatabaseHelper;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.TripsEntity;
import com.dtca.busvalidator.busvalidatorsdk.db.repository.TripsRepository;
import com.dtca.busvalidator.busvalidatorsdk.model.DeviceInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.FareMatrix;
import com.dtca.busvalidator.busvalidatorsdk.model.MasterConfig;
import com.dtca.busvalidator.busvalidatorsdk.model.MasterConfigName;
import com.dtca.busvalidator.busvalidatorsdk.model.RideAndAlight;
import com.dtca.busvalidator.busvalidatorsdk.model.Route;
import com.dtca.busvalidator.busvalidatorsdk.model.StoredLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.RouteNotFoundException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

import lombok.Getter;

public class Utils {
    public static final byte[] AUTH_KEY = {0x6C, (byte) 0xF9, (byte) 0xB1, (byte) 0xC8, 0x44, (byte) 0xC2, 0x6D, (byte) 0x9D, (byte) 0xA3, 0x0E, (byte) 0xF0, 0x62, 0x13, (byte) 0xC9, 0x75, (byte) 0xD1};
    public FareMatrix fareMatrix;
    public List<Route> routes;
    private static Utils utils;
    private static List<MasterConfig> masterConfigs;
    private static DatabaseHelper appDatabase;

    @Getter
    private List<String> cardList;

    @Getter
    private DeviceInfo deviceInfo;

    private Utils() {
        cardList = new ArrayList<>();
        File cacheDir = new File("disk_cache");
    }

    public static synchronized Utils getInstance() {
        if (utils == null) {
            utils = new Utils();
        }
        return utils;
    }

    public static String byteToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                stringBuilder.append("0");
            }
            stringBuilder.append(hex);
        }
        return stringBuilder.toString();
    }

    public static byte[] hexToByte(@NonNull String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) +
                    Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    public static int charArrayToIntLE(byte[] data, int len) {
        int result = 0;

        for (int i = 0; i < len; i++) {
            result |= (data[i] & 0xFF) << (8 * i); // Shift according to byte position
        }

        /*return result;
        int result = 0;
        for(int i=len-1;i>=0;i--){
            result += data[i];
            if(i!=0){
                result = result << 8;
            }
        }*/
        Log.d("charArrayToIntLE", result + "");
        if ((data[len - 1] & 0x80) != 0) return -result;
        return result;

        /*ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        if (len >= 4) {
            return byteBuffer.getInt();
        } else {
            return byteBuffer.get() ;
        }*/
    }

    public static void intToCharArrayLE(int in, byte[] out) {
        Arrays.fill(out, (byte) 0x00);
        out[0] = (byte) in;
        in = in >> 8;
        out[1] = (byte) in;
        in = in >> 8;
        out[2] = (byte) in;
        in = in >> 8;
        out[3] = (byte) in;
    }

    public static byte[] intToCharArrayLE(int in) {
        Log.d("intToCharArrayLE", in + "");
        byte[] out = new byte[4];
        Arrays.fill(out, (byte) 0x00);
        out[0] = (byte) in;
        in = in >> 8;
        out[1] = (byte) in;
        in = in >> 8;
        out[2] = (byte) in;
        in = in >> 8;
        out[3] = (byte) in;
        return out;
    }
    public static byte[] intToCharArrayLE(int input,int len) {
        byte[] out = new byte[len];
        out[0] = (byte) input;
        for (int i = 1;i<len;i++){
            input = input >> 8;
            out[i] = (byte) input;
        }
        return out;
    }
    public static byte[] convertShortToCharArray(int in) {
//        Log.d("intToCharArrayLE", in + "");
        byte[] out = new byte[2];
        out[0] = (byte) (in >> 8);
        out[1] = (byte) in;
        return out;
    }

    /*public static byte[] convertToTwosComplementLE(int in, int len) {
        int onesComplement = ~in;
        int twosComplement = onesComplement + 1;
        byte[] bytes = intToCharArrayLE(twosComplement);
        return Arrays.copyOfRange(bytes, 0, Math.min(4, len));
    }*/

    public static byte[] convertToTwosComplementLE(int in, int len) {
        in = Math.abs(in);
        int onesComplement = ~in;
        int twosComplement = onesComplement + 1;
        return intToCharArrayLE(twosComplement,len);
    }
    public static byte[] reverseArray(byte[] array) {
        byte[] reversed = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            reversed[i] = array[array.length - 1 - i];
        }
        return reversed;
    }
    public static int convertTwosComplementByteArrayToLittleIndian(byte[] in, int len) {
        int value = 0;
        in = reverseArray(in);
        for (byte b : in) {
            value = (value << 8) | (b & 0xFF); // Combine bytes
        }
        // If MSB is set, adjust for negative value
        if ((in[0] & 0x80) != 0) { // Check if the most significant bit is 1
            value -= (1 << (in.length * 8)); // Subtract 2^(number of bits)
        }
        return value;
    }

    public static Map<String, String> getYearMonthDateHourMinute() {
        Calendar calendar = Calendar.getInstance();
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Dhaka");
        calendar.setTimeZone(timeZone);
        Log.d("calenderYear", calendar.get(Calendar.YEAR) + "");
        Log.d("calenderMonth", (calendar.get(Calendar.MONTH) + 1) + "");
        Log.d("calenderDay", calendar.get(Calendar.DATE) + "");
        Log.d("calenderMinute", calendar.get(Calendar.MINUTE) + "");
        Map<String, String> map = new HashMap<>();
        map.put("year", String.format("%7s", Integer.toBinaryString(calendar.get(Calendar.YEAR) % 100)).replace(" ", "0"));
        map.put("month", String.format("%4s", Integer.toBinaryString(calendar.get(Calendar.MONTH) + 1)).replace(" ", "0"));
        map.put("day", String.format("%5s", Integer.toBinaryString(calendar.get(Calendar.DATE))).replace(" ", "0"));
        map.put("hour", String.format("%5s", Integer.toBinaryString(calendar.get(Calendar.HOUR_OF_DAY))).replace(" ", "0"));
        map.put("minute", String.format("%6s", Integer.toBinaryString(calendar.get(Calendar.MINUTE))).replace(" ", "0"));


        return map;
    }

    public static void readBlackListFile(Context context, File file) throws IOException {
        DatabaseHelper helper = DatabaseHelper.getInstance(context.getApplicationContext());
        helper.truncateTable();
        InputStream inputStream = Files.newInputStream(file.toPath());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[1096];

        String line;
        int byteRead;
        StringBuilder builder = new StringBuilder();
        while ((byteRead = inputStream.read(bytes)) != -1) {
            byteArrayOutputStream.write(bytes, 0, byteRead);
        }
        byte[] data = byteArrayOutputStream.toByteArray();
        for (int i = 0; i < data.length; i += 2) {
            String hex = Utils.byteToHex(Arrays.copyOfRange(data, i, Math.min(i + 2, data.length))).toUpperCase();
            builder.append(hex);
        }

//        System.out.println(builder);
//        System.out.println("length: " + builder.toString().length());
        String s = builder.toString();
        for (int i = 40; i < s.length(); i += 40) {
            String block = s.substring(i, Math.min(i + 40, s.length()));
            String cardId = block.substring(0, 16).toUpperCase(); // card id + recycle counter
            String reason = block.substring(block.length() - 2).toUpperCase();
            System.out.println("card id: " + cardId);
            System.out.println("reason: " + reason);
            helper.insertIntoBlacklistTable(cardId, reason);
        }
    }

    public static String getServiceId(StoredLogInformation storedLogInformation) {
        String classificationCode = String.format("%02X", storedLogInformation.getServiceClassificationCode());
        String contextCode = String.format("%02X", storedLogInformation.getContextCode());
        return classificationCode + contextCode;
    }

    public void initializeFareMatrix(String json) {
//        String json = "{ \"routeId\": 2, \"routeName\": \"HR Transport\", \"isFlatFare\": false, \"isCircular\": true, \"numberOfStoppage\": 10, \"stations\": [{ \"id\": 18, \"stationName\": \"Sonargaon Railway Crossing\", \"stationNameBng\": \"সোনারগাঁও রেল ক্রসিং\", \"stationCode\": \"8C1E\", \"stationOrderNo\": 30, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 19, \"stationName\": \"FDC\", \"stationNameBng\": \"এফডিসি\", \"stationCode\": \"8C1C\", \"stationOrderNo\": 28, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 20, \"stationName\": \"Bou Bazar\", \"stationNameBng\": \"বউ বাজার\", \"stationCode\": \"8C19\", \"stationOrderNo\": 25, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 21, \"stationName\": \"Kuni Para/Happy Homes\", \"stationNameBng\": \"কুনি পাড়া/হ্যাপি হোমস\", \"stationCode\": \"8C16\", \"stationOrderNo\": 22, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 22, \"stationName\": \"Police Plaza/Shooting Club\", \"stationNameBng\": \"পুলিশ প্লাজা/শুটিং ক্লাব\", \"stationCode\": \"8C13\", \"stationOrderNo\": 19, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 23, \"stationName\": \"Badda\", \"stationNameBng\": \"বাড্ডা\", \"stationCode\": \"8C11\", \"stationOrderNo\": 17, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 24, \"stationName\": \"Rampura\", \"stationNameBng\": \"রামপুরা\", \"stationCode\": \"8C10\", \"stationOrderNo\": 16, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 25, \"stationName\": \"Mohanagor\", \"stationNameBng\": \"মহানগর\", \"stationCode\": \"8C0D\", \"stationOrderNo\": 13, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }, { \"id\": 26, \"stationName\": \"Modhubag\", \"stationNameBng\": \"মধুবাগ\", \"stationCode\": \"8C0A\", \"stationOrderNo\": 10, \"lattitude\": null, \"longitude\": null, \"mrouteId\": 2 }], \"fareMatrix\": { \"8C1E\": { \"8C1E\": 0, \"8C1C\": 20, \"8C19\": 20, \"8C16\": 20, \"8C13\": 20, \"8C11\": 25, \"8C10\": 25, \"8C0D\": 25, \"8C0A\": 25, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C1C\": { \"8C1E\": 40, \"8C1C\": 0, \"8C19\": 20, \"8C16\": 20, \"8C13\": 20, \"8C11\": 25, \"8C10\": 25, \"8C0D\": 25, \"8C0A\": 25, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C19\": { \"8C1E\": 30, \"8C1C\": 30, \"8C19\": 0, \"8C16\": 20, \"8C13\": 20, \"8C11\": 25, \"8C10\": 25, \"8C0D\": 30, \"8C0A\": 30, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C16\": { \"8C1E\": 30, \"8C1C\": 30, \"8C19\": 40, \"8C16\": 0, \"8C13\": 20, \"8C11\": 20, \"8C10\": 20, \"8C0D\": 25, \"8C0A\": 25, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C13\": { \"8C1E\": 25, \"8C1C\": 25, \"8C19\": 25, \"8C16\": 25, \"8C13\": 0, \"8C11\": 15, \"8C10\": 15, \"8C0D\": 25, \"8C0A\": 25, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C11\": { \"8C1E\": 25, \"8C1C\": 25, \"8C19\": 20, \"8C16\": 20, \"8C13\": 20, \"8C11\": 0, \"8C10\": 20, \"8C0D\": 20, \"8C0A\": 20, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C10\": { \"8C1E\": 25, \"8C1C\": 25, \"8C19\": 20, \"8C16\": 20, \"8C13\": 20, \"8C11\": 40, \"8C10\": 0, \"8C0D\": 20, \"8C0A\": 20, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C0D\": { \"8C1E\": 20, \"8C1C\": 20, \"8C19\": 25, \"8C16\": 25, \"8C13\": 25, \"8C11\": 25, \"8C10\": 25, \"8C0D\": 0, \"8C0A\": 20, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }, \"8C0A\": { \"8C1E\": 15, \"8C1C\": 15, \"8C19\": 20, \"8C16\": 20, \"8C13\": 25, \"8C11\": 25, \"8C10\": 25, \"8C0D\": 40, \"8C0A\": 0, \"maxFareUpStream\": 40, \"maxFareDownStream\": 40 }}}";
        Gson gson = new Gson();
        fareMatrix = gson.fromJson(json, FareMatrix.class);
        System.out.println(fareMatrix.getRouteName());
        System.out.println(fareMatrix.getFareMatrix());
    }

    public void initializeRouteList(String json) {
        routes = new Gson().fromJson(json, new TypeToken<List<Route>>() {
        }.getType());
    }

    public static int byteToInteger(byte[] bytes) {
        String hex = byteToHex(bytes);
        return Integer.parseInt(hex, 16);
    }

    public static String convertByteArrayToBit(byte[] bytes) {
        StringBuilder bitString = new StringBuilder();
        for (byte b : bytes) {
            bitString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return bitString.toString().trim();
    }

    public static int byteArrayToInt(byte[] byteArray) {
        int result = 0;
        int length = byteArray.length;
        if (length > 4) {
            throw new IllegalArgumentException("Byte array too large to convert to int");
        }

        // Combine each byte into the int
        for (int i = 0; i < length; i++) {
            result |= (byteArray[i] & 0xFF) << (8 * (length - 1 - i));
        }

        return result;
    }

    public static String getDeviceSerialNo() {

        String[] data = BasicOper.dc_GetDeviceUid().split("\\|");
        if (data.length > 0 && data[0].equals("0000")) {
            return data[1];
        }
        return null;
    }

    public void setDeviceInfo(String json) {
        this.deviceInfo = new Gson().fromJson(json, DeviceInfo.class);
    }

    public void clearCardList() {
        this.cardList.clear();
    }

    public Integer getFare(StoredLogInformation storedLogInformation,byte[] idi) {
        String serviceId = String.format("%02X%02X",storedLogInformation.getServiceClassificationCode(), storedLogInformation.getContextCode());
        if(!Arrays.asList("D220","D320").contains(serviceId.toUpperCase())){
            TripsEntity tripsEntity = Utils.getTripsByCardId(Utils.byteToHex(idi));
            if(tripsEntity!=null && tripsEntity.fromStation!=null){
                storedLogInformation.setPlace1(Utils.hexToByte(tripsEntity.fromStation));
            }
        }
        String station1 = Utils.byteToHex(storedLogInformation.getPlace1()).toUpperCase();
        String station2 = Utils.byteToHex(storedLogInformation.getPlace2()).toUpperCase();
        try {
            if(ValidateCard.isCircularRoute(station1) && station1.equalsIgnoreCase(station2)) {
                return Objects.requireNonNull(Utils.getInstance().fareMatrix.getFareMatrix().get(station1)).get("maxFareUpStream");
            }
        } catch (RouteNotFoundException e) {
            throw new RuntimeException(e);
        }
        return Objects.requireNonNull(Utils.getInstance().fareMatrix.getFareMatrix().get(station1)).get(station2);
    }
    public Integer getFare(StoredLogInformation storedLogInformation) {
        String station1 = Utils.byteToHex(storedLogInformation.getPlace1()).toUpperCase();
        String station2 = Utils.byteToHex(storedLogInformation.getPlace2()).toUpperCase();
        try {
            if(ValidateCard.isCircularRoute(station1) && station1.equalsIgnoreCase(station2)) {
                return Objects.requireNonNull(Utils.getInstance().fareMatrix.getFareMatrix().get(station1)).get("maxFareUpStream");
            }
        } catch (RouteNotFoundException e) {
            throw new RuntimeException(e);
        }
        return Objects.requireNonNull(Utils.getInstance().fareMatrix.getFareMatrix().get(station1)).get(station2);
    }
    public Integer getFare(String cardId) {
        TripsEntity entity = appDatabase.getTripsByCardId(cardId);
        String station1 = entity.fromStation;
        String station2 = entity.toStation;
        try {
            if(ValidateCard.isCircularRoute(station1) && station1.equalsIgnoreCase(station2)) {
                return Objects.requireNonNull(Utils.getInstance().fareMatrix.getFareMatrix().get(station1)).get("maxFareUpStream");
            }
        } catch (RouteNotFoundException e) {
            throw new RuntimeException(e);
        }
        return Objects.requireNonNull(Utils.getInstance().fareMatrix.getFareMatrix().get(station1)).get(station2);
    }

    public static int openSerialReader() {
        long st = System.currentTimeMillis();
        String port = "/dev/dc_spi32765.0";
        String portUart = "/dev/ttyUSB0";
        BasicOper.dc_setLanguageEnv(1);
        int devHandle = BasicOper.dc_open("COM", null, port, 115200);
        if (devHandle < 0) {
            port = portUart;
            devHandle = BasicOper.dc_open("COM", null, port, 115200);
        }
        long et = System.currentTimeMillis();
        Log.d("openSerialReader", String.valueOf((((double) (et - st) / 1000))));
        if (devHandle > 0) {
            return 0;
        } else {
            return -2;
        }


    }
    public  void initMasterConfig(String json) {
        if(json!=null){
            masterConfigs = new Gson().fromJson(json, new TypeToken<List<MasterConfig>>(){}.getType());
        }
    }
    public static int getMasterConfig(MasterConfigName configName) {
        Optional<MasterConfig> config = masterConfigs.stream().filter(c->c.getConfigName().equalsIgnoreCase(configName.getName())).findFirst();
        return config.map(masterConfig -> Integer.parseInt(masterConfig.getValue())).orElse(configName.getValue());
    }
    public static void initAppDatabase(Context context) {
        appDatabase = DatabaseHelper.getInstance(context);
    }
    public static void setCardDirection(String cardId, RideAndAlight.Direction direction){
//        appDatabase.insertIntoTripsTable(cardId,direction.name());
        TripsRepository.getInstance(appDatabase.getAppDatabase()).insert(cardId,direction.name());
    }
    public static void updateCardDirection(String cardId, RideAndAlight.Direction direction){
//        appDatabase.updateTripsTable(cardId,direction.name());
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateDirection(cardId,direction.name());
    }
    public static void updateFromStation(String cardId, String station){
//        appDatabase.updateTripsTableFromStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateFromStation(cardId,station);
    }
    public static void updateToStation(String cardId, String station){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateToStation(cardId,station);
    }
    public static void updateCashBackAmount(String cardId, int amount){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateCashBackAmount(cardId,amount);
    }

    public static void delete(String cardId){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).deleteByCardId(cardId);
    }

    public static void truncateTable(){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).truncate();
    }
    public static TripsEntity getTripsByCardId(String cardId){
        return appDatabase.getTripsByCardId(cardId);
    }
    public static void updateOrInsertTrips(String cardId, RideAndAlight.Direction direction){
        TripsEntity tripsEntity = Utils.getTripsByCardId(cardId);
        if(tripsEntity!=null){
            Utils.updateCardDirection(cardId,direction);
        }
        else {
            Utils.setCardDirection(cardId,direction);
        }
    }

    public static String hexToString(String hex){
        StringBuilder result = new StringBuilder();

        // Process every 2 characters in the hex string
        for (int i = 0; i < hex.length(); i += 2) {
            String hexPair = hex.substring(i, i + 2); // Get a pair of hex digits
            int decimal = Integer.parseInt(hexPair, 16); // Convert to decimal
            result.append((char) decimal); // Map to character
        }
        return result.toString();
    }


}
