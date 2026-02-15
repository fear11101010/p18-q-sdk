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
/**
 * Utility class for the Bus Validator SDK providing various helper methods for:
 * <ul>
 *   <li>Byte array and hexadecimal conversions</li>
 *   <li>Little-endian integer conversions</li>
 *   <li>Fare calculation and route management</li>
 *   <li>Card and trip management</li>
 *   <li>Device serial communication</li>
 *   <li>Configuration management</li>
 * </ul>
 *
 * This class implements the Singleton pattern to ensure only one instance exists.
 *
 * @author DTCA Bus Validator Team
 * @version 1.0
 */
public class Utils {
    /**
     * Authentication key used for card validation operations.
     * This is a 16-byte AES key stored as a byte array.
     */
    public static final byte[] AUTH_KEY = {0x6C, (byte) 0xF9, (byte) 0xB1, (byte) 0xC8, 0x44, (byte) 0xC2, 0x6D, (byte) 0x9D, (byte) 0xA3, 0x0E, (byte) 0xF0, 0x62, 0x13, (byte) 0xC9, 0x75, (byte) 0xD1};
    /**
     * Fare matrix containing route and fare information for the current bus route.
     */
    public FareMatrix fareMatrix;
    /**
     * List of all available routes in the system.
     */
    public List<Route> routes;
    /**
     * Singleton instance of the Utils class.
     */
    private static Utils utils;
    /**
     * List of master configuration settings.
     */
    private static List<MasterConfig> masterConfigs;
    /**
     * Database helper instance for accessing the application database.
     */
    private static DatabaseHelper appDatabase;
    /**
     * List of card IDs that have been processed.
     */
    @Getter
    private List<String> cardList;
    /**
     * Device information for the current validator hardware.
     */
    @Getter
    private DeviceInfo deviceInfo;
    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the card list and cache directory.
     */
    private Utils() {
        cardList = new ArrayList<>();
        File cacheDir = new File("disk_cache");
    }
    /**
     * Gets the singleton instance of the Utils class.
     * Creates a new instance if one doesn't exist.
     *
     * @return the singleton Utils instance
     */
    public static synchronized Utils getInstance() {
        if (utils == null) {
            utils = new Utils();
        }
        return utils;
    }
    /**
     * Converts a byte array to its hexadecimal string representation.
     *
     * @param bytes the byte array to convert
     * @return hexadecimal string representation of the byte array
     *
     * @example
     * <pre>
     * byte[] bytes = {0x1A, 0x2B, 0x3C};
     * String hex = byteToHex(bytes); // Returns "1a2b3c"
     * </pre>
     */
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
    /**
     * Converts a hexadecimal string to its byte array representation.
     *
     * @param hex the hexadecimal string to convert (must have even length)
     * @return byte array representation of the hex string
     * @throws NumberFormatException if the hex string contains invalid characters
     *
     * @example
     * <pre>
     * String hex = "1a2b3c";
     * byte[] bytes = hexToByte(hex); // Returns {0x1A, 0x2B, 0x3C}
     * </pre>
     */
    public static byte[] hexToByte(@NonNull String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) +
                    Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }
    /**
     * Converts a byte array to an integer using little-endian byte order.
     * Handles negative numbers using two's complement representation.
     *
     * @param data the byte array to convert
     * @param len the number of bytes to process (1-4)
     * @return the integer value in little-endian format
     *
     * @example
     * <pre>
     * byte[] data = {0x01, 0x02, 0x00, 0x00};
     * int value = charArrayToIntLE(data, 2); // Returns 513 (0x0201)
     * </pre>
     */
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
    /**
     * Converts an integer to a 4-byte array in little-endian format.
     * Modifies the provided output array in place.
     *
     * @param in the integer value to convert
     * @param out the output byte array (must be at least 4 bytes)
     */
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
    /**
     * Converts an integer to a 4-byte array in little-endian format.
     *
     * @param in the integer value to convert
     * @return 4-byte array in little-endian format
     */
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
    /**
     * Converts an integer to a byte array of specified length in little-endian format.
     *
     * @param input the integer value to convert
     * @param len the desired length of the output array
     * @return byte array of specified length in little-endian format
     */
    public static byte[] intToCharArrayLE(int input,int len) {
        byte[] out = new byte[len];
        out[0] = (byte) input;
        for (int i = 1;i<len;i++){
            input = input >> 8;
            out[i] = (byte) input;
        }
        return out;
    }
    /**
     * Converts a short integer to a 2-byte array in big-endian format.
     *
     * @param in the integer value to convert (typically 0-65535)
     * @return 2-byte array in big-endian format
     */
    public static byte[] convertShortToCharArray(int in) {
//        Log.d("intToCharArrayLE", in + "");
        byte[] out = new byte[2];
        out[0] = (byte) (in >> 8);
        out[1] = (byte) in;
        return out;
    }

    /**
     * Converts an integer to its two's complement representation in little-endian format.
     * Used for representing negative numbers in binary form.
     *
     * @param in the integer value to convert
     * @param len the desired length of the output array
     * @return byte array containing two's complement representation
     */

    public static byte[] convertToTwosComplementLE(int in, int len) {
        in = Math.abs(in);
        int onesComplement = ~in;
        int twosComplement = onesComplement + 1;
        return intToCharArrayLE(twosComplement,len);
    }
    /**
     * Reverses the order of elements in a byte array.
     *
     * @param array the byte array to reverse
     * @return new byte array with elements in reversed order
     */
    public static byte[] reverseArray(byte[] array) {
        byte[] reversed = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            reversed[i] = array[array.length - 1 - i];
        }
        return reversed;
    }
    /**
     * Converts a two's complement byte array to an integer.
     * Handles both positive and negative values.
     *
     * @param in the byte array in two's complement format
     * @param len the number of bytes to process
     * @return the integer value
     */
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
    /**
     * Gets the current date and time components in binary string format.
     * Uses Asia/Dhaka timezone.
     *
     * @return Map containing binary string representations of year, month, day, hour, and minute
     *
     * @example
     * <pre>
     * Map&lt;String, String&gt; time = getYearMonthDateHourMinute();
     * String yearBinary = time.get("year"); // e.g., "0010110" for 2026
     * </pre>
     */
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
    /**
     * Reads and processes a blacklist file containing blocked card IDs.
     * Parses the binary file format and stores blacklist entries in the database.
     *
     * @param context the Android application context
     * @param file the blacklist file to read
     * @throws IOException if file reading fails
     *
     * @implNote File format: 40-character blocks where first 16 chars are card ID
     *           and last 2 chars are the blacklist reason code
     */
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
    /**
     * Extracts the service ID from stored log information.
     * Service ID is composed of classification code and context code.
     *
     * @param storedLogInformation the log information containing service details
     * @return service ID as a 4-character hexadecimal string
     *
     * @example
     * <pre>
     * String serviceId = getServiceId(logInfo); // Returns "D220"
     * </pre>
     */
    public static String getServiceId(StoredLogInformation storedLogInformation) {
        String classificationCode = String.format("%02X", storedLogInformation.getServiceClassificationCode());
        String contextCode = String.format("%02X", storedLogInformation.getContextCode());
        return classificationCode + contextCode;
    }
    /**
     * Initializes the fare matrix from a JSON string.
     * The fare matrix contains route information and fare prices between stations.
     *
     * @param json JSON string containing fare matrix data
     */
    public void initializeFareMatrix(String json) {
        Gson gson = new Gson();
        fareMatrix = gson.fromJson(json, FareMatrix.class);
        System.out.println(fareMatrix.getRouteName());
        System.out.println(fareMatrix.getFareMatrix());
    }
    /**
     * Initializes the route list from a JSON string.
     *
     * @param json JSON string containing list of route data
     */
    public void initializeRouteList(String json) {
        routes = new Gson().fromJson(json, new TypeToken<List<Route>>() {
        }.getType());
    }
    /**
     * Converts a byte array to an integer value.
     * First converts to hexadecimal string, then parses as integer.
     *
     * @param bytes the byte array to convert
     * @return integer value
     */
    public static int byteToInteger(byte[] bytes) {
        String hex = byteToHex(bytes);
        return Integer.parseInt(hex, 16);
    }
    /**
     * Converts a byte array to its binary string representation.
     * Each byte is converted to an 8-bit binary string.
     *
     * @param bytes the byte array to convert
     * @return binary string representation with spaces trimmed
     *
     * @example
     * <pre>
     * byte[] bytes = {0x0F, (byte)0xFF};
     * String binary = convertByteArrayToBit(bytes); // Returns "0000111111111111"
     * </pre>
     */
    public static String convertByteArrayToBit(byte[] bytes) {
        StringBuilder bitString = new StringBuilder();
        for (byte b : bytes) {
            bitString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return bitString.toString().trim();
    }
    /**
     * Converts a byte array to an integer using big-endian byte order.
     *
     * @param byteArray the byte array to convert (max 4 bytes)
     * @return integer value
     * @throws IllegalArgumentException if byte array is longer than 4 bytes
     */
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
    /**
     * Retrieves the device serial number from the hardware.
     * Uses BasicOper library to communicate with the device.
     *
     * @return device serial number as a string, or null if retrieval fails
     */
    public static String getDeviceSerialNo() {

        String[] data = BasicOper.dc_GetDeviceUid().split("\\|");
        if (data.length > 0 && data[0].equals("0000")) {
            return data[1];
        }
        return null;
    }
    /**
     * Sets the device information from a JSON string.
     *
     * @param json JSON string containing device information
     */
    public void setDeviceInfo(String json) {
        this.deviceInfo = new Gson().fromJson(json, DeviceInfo.class);
    }
    /**
     * Clears all entries from the card list.
     */
    public void clearCardList() {
        this.cardList.clear();
    }
    /**
     * Calculates the fare for a trip based on stored log information and card ID.
     * Handles special cases for circular routes and retrieves stored trip data.
     *
     * @param storedLogInformation log information containing service and location details
     * @param idi card ID as byte array
     * @return fare amount as an integer
     * @throws RuntimeException if route is not found
     */
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
    /**
     * Calculates the fare for a trip based on stored log information only.
     *
     * @param storedLogInformation log information containing origin and destination stations
     * @return fare amount as an integer
     * @throws RuntimeException if route is not found
     */
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
    /**
     * Calculates the fare for a stored trip by card ID.
     * Retrieves trip information from database and calculates fare.
     *
     * @param cardId the card ID to look up
     * @return fare amount as an integer
     * @throws RuntimeException if route is not found
     */
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
    /**
     * Opens and initializes the serial reader for card communication.
     * Attempts to connect via SPI first, then falls back to UART.
     *
     * @return 0 if successful, -2 if connection fails
     */
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
    /**
     * Initializes master configuration settings from a JSON string.
     *
     * @param json JSON string containing list of master configuration entries
     */
    public  void initMasterConfig(String json) {
        if(json!=null){
            masterConfigs = new Gson().fromJson(json, new TypeToken<List<MasterConfig>>(){}.getType());
        }
    }
    /**
     * Retrieves a master configuration value by name.
     * Returns default value if configuration is not found.
     *
     * @param configName the configuration name enum
     * @return configuration value as an integer
     */
    public static int getMasterConfig(MasterConfigName configName) {
        Optional<MasterConfig> config = masterConfigs.stream().filter(c->c.getConfigName().equalsIgnoreCase(configName.getName())).findFirst();
        return config.map(masterConfig -> Integer.parseInt(masterConfig.getValue())).orElse(configName.getValue());
    }
    /**
     * Initializes the application database helper.
     *
     * @param context the Android application context
     */
    public static void initAppDatabase(Context context) {
        appDatabase = DatabaseHelper.getInstance(context);
    }
    /**
     * Sets the travel direction for a card and creates a new trip record.
     *
     * @param cardId the card ID
     * @param direction the travel direction (UPSTREAM or DOWNSTREAM)
     */
    public static void setCardDirection(String cardId, RideAndAlight.Direction direction){
//        appDatabase.insertIntoTripsTable(cardId,direction.name());
        TripsRepository.getInstance(appDatabase.getAppDatabase()).insert(cardId,direction.name());
    }
    /**
     * Updates the travel direction for an existing card trip.
     *
     * @param cardId the card ID
     * @param direction the new travel direction
     */
    public static void updateCardDirection(String cardId, RideAndAlight.Direction direction){
//        appDatabase.updateTripsTable(cardId,direction.name());
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateDirection(cardId,direction.name());
    }
    /**
     * Updates the origin station for a card's trip.
     *
     * @param cardId the card ID
     * @param station the station code
     */
    public static void updateFromStation(String cardId, String station){
//        appDatabase.updateTripsTableFromStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateFromStation(cardId,station);
    }
    /**
     * Updates the destination station for a card's trip.
     *
     * @param cardId the card ID
     * @param station the station code
     */
    public static void updateToStation(String cardId, String station){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateToStation(cardId,station);
    }
    /**
     * Updates the cashback amount for a card's trip.
     *
     * @param cardId the card ID
     * @param amount the cashback amount
     */
    public static void updateCashBackAmount(String cardId, int amount){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).updateCashBackAmount(cardId,amount);
    }
    /**
     * Deletes a trip record by card ID.
     *
     * @param cardId the card ID
     */
    public static void delete(String cardId){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).deleteByCardId(cardId);
    }
    /**
     * Truncates the trips table, removing all trip records.
     */
    public static void truncateTable(){
//        appDatabase.updateTripsTableToStation(cardId,station);
        TripsRepository.getInstance(appDatabase.getAppDatabase()).truncate();
    }
    /**
     * Retrieves trip information by card ID.
     *
     * @param cardId the card ID to look up
     * @return TripsEntity containing trip details, or null if not found
     */
    public static TripsEntity getTripsByCardId(String cardId){
        return appDatabase.getTripsByCardId(cardId);
    }
    /**
     * Updates an existing trip or inserts a new one if it doesn't exist.
     *
     * @param cardId the card ID
     * @param direction the travel direction
     */
    public static void updateOrInsertTrips(String cardId, RideAndAlight.Direction direction){
        TripsEntity tripsEntity = Utils.getTripsByCardId(cardId);
        if(tripsEntity!=null){
            Utils.updateCardDirection(cardId,direction);
        }
        else {
            Utils.setCardDirection(cardId,direction);
        }
    }
    /**
     * Converts a hexadecimal string to its ASCII string representation.
     *
     * @param hex the hexadecimal string to convert (pairs of hex digits)
     * @return ASCII string representation
     *
     * @example
     * <pre>
     * String hex = "48656C6C6F";
     * String text = hexToString(hex); // Returns "Hello"
     * </pre>
     */
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
