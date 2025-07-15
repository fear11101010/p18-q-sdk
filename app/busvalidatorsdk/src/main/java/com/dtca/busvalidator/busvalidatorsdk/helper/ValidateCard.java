package com.dtca.busvalidator.busvalidatorsdk.helper;

import android.content.Context;

import com.dtca.busvalidator.busvalidatorsdk.db.DatabaseHelper;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.BlackListEntity;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.TripsEntity;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.MasterConfigName;
import com.dtca.busvalidator.busvalidatorsdk.model.RideAndAlight;
import com.dtca.busvalidator.busvalidatorsdk.model.Route;
import com.dtca.busvalidator.busvalidatorsdk.model.StoredLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardReadException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardUnavailableException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardUnissuedException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.RouteNotFoundException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public class ValidateCard {
    public static final int MINIMUM_CARD_BALANCE = 0;
    private static final int TIME_DIFFERENT = 5;
    private static final String TIME_DIFFERENT_TYPE = "minute";

    public static boolean commonValidationCheck(byte[] cardFunctionCode) {
        char[] bits = Utils.convertByteArrayToBit(cardFunctionCode).toCharArray();
        return bits[15] == '1' && bits[14] == '1' && bits[11] == '0' && bits[10] == '0' && bits[9] == '0'
                && (bits[8] == '0' || bits[8] == '1') && bits[6] == '1';
    }
    public static boolean isSameCardId(byte[] initIdi,byte[] idi) {
        return Arrays.equals(initIdi,idi);
    }
    public static boolean isCardActive(byte statusFlag) throws CardUnissuedException, CardUnavailableException {
        if((statusFlag & (1 << 7)) == 0 && (statusFlag & (1 << 6)) == 0){
            throw new CardUnissuedException("Card is not issued yet");
        } else if ((statusFlag & (1 << 7)) == 0 && (statusFlag & (1 << 6)) != 0) {
            throw new CardUnavailableException("Card is not available (bad state)");
        }  else if ((statusFlag & (1 << 7)) != 0 && (statusFlag & (1 << 6)) != 0) {
            throw new CardUnavailableException("Card is not available (collected)");
        }
        return (statusFlag & (1 << 7)) != 0 && (statusFlag & (1 << 6)) == 0;
    }
    public static boolean isTestCard(byte[] cardFunctionCode) {
        int i = Utils.byteArrayToInt(cardFunctionCode);
        return (i & (1 << 8)) != 0;
    }

    public static boolean isVoidCard(byte cardControlCode) {
        return ((cardControlCode & (1 << 7)) == 0 && (cardControlCode & (1 << 6)) != 0) ||
                ((cardControlCode & (1 << 7)) != 0 && (cardControlCode & (1 << 6)) == 0) ||
                ((cardControlCode & (1 << 7)) != 0 && (cardControlCode & (1 << 6)) != 0);
    }

    public static boolean isCardBlacklisted(byte cardControlCode) {
        return (cardControlCode & (1 << 7)) != 0;
    }

    public static boolean isCardBlacklistedInDB(Context context, byte[] idi, byte recycleCounter) {
//        byte[] cardId = new byte[9];
//        System.arraycopy(idi, 0, cardId, 0, 8);
//        cardId[8] = recycleCounter;
        BlackListEntity blackListEntity = DatabaseHelper.getInstance(context).fetchBlacklistDataByCardId(Utils.byteToHex(idi).toUpperCase());
        return blackListEntity != null;
    }

    public static boolean isStatusRide(byte[] bytes) throws CardReadException {
        if (bytes == null || bytes.length == 0)
            throw new CardReadException("Please read card first");
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(bytes) & mask;
        return statusFlag != 0;
    }

    public static boolean isStatusAlight(byte[] bytes) throws CardReadException {
        if (bytes == null || bytes.length == 0)
            throw new CardReadException("Please read card first");
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(bytes) & mask;
        return statusFlag == 0;
    }
    public static boolean isBus(byte[] bytes) throws CardReadException {
        if (bytes == null || bytes.length == 0)
            throw new CardReadException("Please read card first");
        int mask = 1 << 11;
        int statusFlag = Utils.byteArrayToInt(bytes) & mask;
        return statusFlag != 0;
    }

    public static boolean isSameBus(byte[] bytes) {
        String equipmentLocationNumber = Utils.byteToHex(bytes);
        return Utils.getInstance().getDeviceInfo().getEquipmentLocationNumber().equalsIgnoreCase(equipmentLocationNumber) ||
                (Utils.getInstance().getDeviceInfo().getPairedEquipmentLocationNumber() != null &&
                        Arrays.stream(Utils.getInstance().getDeviceInfo().getPairedEquipmentLocationNumber()).anyMatch(n->n.equalsIgnoreCase(equipmentLocationNumber)));
    }

    public static boolean checkServiceIdIsEligibleForRide(GateAccessLogInformation accessLogInformation) {
        String statusFlag = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag());
        char[] bits = statusFlag.toCharArray();
        if (bits[0] == '1' && bits[4] == '0') {
            return false;
        }
        return bits[0] == '0' || bits[4] == '1';
    }

    public static boolean checkServiceIdIsEligibleForAlight(GateAccessLogInformation accessLogInformation) {
        String statusFlag = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag());
        char[] bits = statusFlag.toCharArray();
        if (bits[0] == '1' && bits[4] == '0') {
            return false;
        }
        return bits[0] == '1' || bits[4] == '1';
    }


    public static boolean isSameRoute(String stationCodeToRide) {
        return Utils.getInstance().routes.stream()
                .anyMatch(r -> r.getStations().stream().anyMatch(s -> s.getStationCode().equalsIgnoreCase(stationCodeToRide)));
    }

    public static boolean isSameRoute(String stationCodeToRide,String previousStationCode) {
        Optional<Route> currentRoute = Utils.getInstance().routes.stream()
                .filter(r->r.getStations().stream().anyMatch(s -> s.getStationCode().equalsIgnoreCase(stationCodeToRide))).findFirst();
        Optional<Route> previousRoute = Utils.getInstance().routes.stream()
                .filter(r->r.getStations().stream().anyMatch(s -> s.getStationCode().equalsIgnoreCase(previousStationCode))).findFirst();
        return currentRoute.isPresent() && previousRoute.isPresent() && currentRoute.get().getId() == previousRoute.get().getId();
    }

    public static boolean isSameStation(byte[] currentStation, String stationCodeToRide) {
        return Utils.byteToHex(currentStation).equalsIgnoreCase(stationCodeToRide);
    }

    public static boolean isSameDate(StoredLogInformation storedLogInformation) {
        String date = Utils.convertByteArrayToBit(storedLogInformation.getDate());
        String time = String.format("%8s", Integer.toBinaryString(storedLogInformation.getTime() & 0xFF)).replace(' ', '0');
        ;
        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);


        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        String d = String.format(Locale.ENGLISH,"%02d-%02d-%02d 00:00:00", day, month, year);
        LocalDateTime previousDate = LocalDateTime.parse(d, dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        Duration duration = Duration.between(previousDate, currentDate);

        return currentDate.isEqual(previousDate);

    }
    public static boolean isSameDate(GateAccessLogInformation gateAccessLogInformation) {
        String date = Utils.convertByteArrayToBit(gateAccessLogInformation.getDate());
        /*String time = String.format("%8s", Integer.toBinaryString(gateAccessLogInformation.getTime()[0] & 0xFF)).replace(' ', '0');
        ;*/
        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);


        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        String d = String.format(Locale.ENGLISH,"%02d-%02d-%02d 00:00:00", day, month, year);
        LocalDateTime previousDate = LocalDateTime.parse(d, dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        Duration duration = Duration.between(previousDate, currentDate);

        return currentDate.isEqual(previousDate);

    }

    public static boolean isGreaterThenTime(GateAccessLogInformation gateAccessLogInformation) {
        String date = Utils.convertByteArrayToBit(gateAccessLogInformation.getDate());
        String time = Utils.convertByteArrayToBit(gateAccessLogInformation.getTime());
        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);
        int hour = Integer.parseInt(time.substring(0, 8), 2);
        int minute = Integer.parseInt(time.substring(8, 16), 2);


        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        LocalDateTime previousDate = LocalDateTime.parse(String.format(Locale.ENGLISH,"%02d-%02d-%02d %02d:%02d:00", day, month, year, hour, minute),
                dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter).withSecond(0).withNano(0);
        Duration duration = Duration.between(previousDate, currentDate);
        return duration.toMinutes() > TIME_DIFFERENT;

    }
    public static boolean isGreaterThenTime(GateAccessLogInformation gateAccessLogInformation, MasterConfigName configName) {
        String date = Utils.convertByteArrayToBit(gateAccessLogInformation.getDate());
        String time = Utils.convertByteArrayToBit(gateAccessLogInformation.getTime());
        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);
        int hour = Integer.parseInt(time.substring(0, 8), 2);
        int minute = Integer.parseInt(time.substring(8, 16), 2);


        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss", Locale.ENGLISH);
        LocalDateTime previousDate = LocalDateTime.parse(String.format(Locale.ENGLISH,"%02d-%02d-%02d %02d:%02d:00", day, month, year, hour, minute),
                dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter).withSecond(0).withNano(0);
        Duration duration = Duration.between(previousDate, currentDate);
        return duration.toMinutes() > Utils.getMasterConfig(configName);

    }

    public static boolean commonCheckForRideAndAlight(StoredLogInformation storedLogInformation, GateAccessLogInformation gateAccessLogInformation) {
        return checkServiceIdIsEligibleForRide(gateAccessLogInformation) && isSameBus(gateAccessLogInformation.getCurrentEquipmentLocationNumber())
                && !isSameDate(storedLogInformation);
    }

    public static boolean isCircularRoute(String stationCode) throws RouteNotFoundException {
        Optional<Route> tempRoute = Utils.getInstance().routes.stream()
                .filter(r -> r.getStations().stream().anyMatch(s -> s.getStationCode().equalsIgnoreCase(stationCode))).findFirst();
        if (!tempRoute.isPresent()) throw new RouteNotFoundException("Route not exists");
        Route route = tempRoute.get();
        return route.isCircularRoute();
    }

    public static boolean isEnoughBalanceAvailable(byte[] cardBalance,MasterConfigName config) {
        if ((cardBalance[cardBalance.length - 1] & 0x80) != 0) {
            return Utils.convertTwosComplementByteArrayToLittleIndian(cardBalance,cardBalance.length)>=Utils.getMasterConfig(config);
        }
        int balance = Utils.charArrayToIntLE(cardBalance, cardBalance.length);
        return balance >= Utils.getMasterConfig(config);
    }

    public static boolean checkMaxNegativeValue(byte[] svValue) {
        int negativeBalance = Utils.charArrayToIntLE(svValue, 4);
        return negativeBalance <= 100;
    }

    public static boolean isFirstIssue(byte[] svLogId) throws CardUnissuedException {
        int svId = Utils.byteArrayToInt(svLogId);
        return svId == 0;
    }
    public static boolean isMRTCard(byte[] issuerId) throws CardUnissuedException {
        return Utils.byteToHex(issuerId).equalsIgnoreCase("0A04");
    }

    public static boolean checkCardDirection(String cardId, RideAndAlight.Direction direction) {
        TripsEntity tripsEntity = Utils.getTripsByCardId(cardId);
        System.out.println("card direction check"+(tripsEntity != null && tripsEntity.direction.equalsIgnoreCase(direction.name())));
        return tripsEntity != null && tripsEntity.direction.equalsIgnoreCase(direction.name());
    }
}
