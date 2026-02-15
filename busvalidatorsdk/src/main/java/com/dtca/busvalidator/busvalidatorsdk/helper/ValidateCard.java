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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Utility class for validating transit cards in a bus validator system.
 * This class provides various validation methods for card status, balance, routes,
 * and transaction rules for a transit card payment system.
 *
 * <p>The class handles validation for:
 * <ul>
 *   <li>Card status and activation checks</li>
 *   <li>Card blacklist verification</li>
 *   <li>Route and station validation</li>
 *   <li>Balance and fare calculations</li>
 *   <li>Transaction timing and sequencing</li>
 * </ul>
 *
 * @author DTCA Bus Validator SDK
 * @version 1.0
 */
public class ValidateCard {

    /** Minimum balance required on a card (in lowest currency unit) */
    public static final int MINIMUM_CARD_BALANCE = 0;

    /** Time difference threshold in minutes for transaction validation */
    private static final int TIME_DIFFERENT = 5;

    /** Unit type for time difference (minute) */
    private static final String TIME_DIFFERENT_TYPE = "minute";

    /**
     * Performs common validation checks on the card function code.
     * Validates specific bit positions to ensure the card meets standard requirements.
     *
     * @param cardFunctionCode the card function code as a byte array
     * @return {@code true} if all validation bits are correctly set, {@code false} otherwise
     */
    public static boolean commonValidationCheck(byte[] cardFunctionCode) {
        char[] bits = Utils.convertByteArrayToBit(cardFunctionCode).toCharArray();
        return bits[15] == '1' && bits[14] == '1' && bits[11] == '0' && bits[10] == '0' && bits[9] == '0'
                && (bits[8] == '0' || bits[8] == '1') && bits[6] == '1';
    }

    /**
     * Checks if two card IDs are identical.
     *
     * @param initIdi the initial card ID to compare
     * @param idi the current card ID to compare
     * @return {@code true} if both card IDs are equal, {@code false} otherwise
     */
    public static boolean isSameCardId(byte[] initIdi, byte[] idi) {
        return Arrays.equals(initIdi, idi);
    }

    /**
     * Validates whether a card is in an active state based on its status flag.
     *
     * <p>Status combinations:
     * <ul>
     *   <li>Bit 7=0, Bit 6=0: Card not issued</li>
     *   <li>Bit 7=0, Bit 6=1: Card in bad state</li>
     *   <li>Bit 7=1, Bit 6=1: Card collected/deactivated</li>
     *   <li>Bit 7=1, Bit 6=0: Card active (valid state)</li>
     * </ul>
     *
     * @param statusFlag the card status flag byte
     * @return {@code true} if the card is active
     * @throws CardUnissuedException if the card has not been issued
     * @throws CardUnavailableException if the card is in a bad state or has been collected
     */
    public static boolean isCardActive(byte statusFlag) throws CardUnissuedException, CardUnavailableException {
        if ((statusFlag & (1 << 7)) == 0 && (statusFlag & (1 << 6)) == 0) {
            throw new CardUnissuedException("Card is not issued yet");
        } else if ((statusFlag & (1 << 7)) == 0 && (statusFlag & (1 << 6)) != 0) {
            throw new CardUnavailableException("Card is not available (bad state)");
        } else if ((statusFlag & (1 << 7)) != 0 && (statusFlag & (1 << 6)) != 0) {
            throw new CardUnavailableException("Card is not available (collected)");
        }
        return (statusFlag & (1 << 7)) != 0 && (statusFlag & (1 << 6)) == 0;
    }

    /**
     * Determines if a card is a test card based on its function code.
     * Test cards are used for system testing and should be handled differently.
     *
     * @param cardFunctionCode the card function code as a byte array
     * @return {@code true} if this is a test card, {@code false} otherwise
     */
    public static boolean isTestCard(byte[] cardFunctionCode) {
        int i = Utils.byteArrayToInt(cardFunctionCode);
        return (i & (1 << 8)) != 0;
    }

    /**
     * Checks if a card has been voided based on its control code.
     * A void card cannot be used for transactions.
     *
     * @param cardControlCode the card control code byte
     * @return {@code true} if the card is voided, {@code false} otherwise
     */
    public static boolean isVoidCard(byte cardControlCode) {
        return ((cardControlCode & (1 << 7)) == 0 && (cardControlCode & (1 << 6)) != 0) ||
                ((cardControlCode & (1 << 7)) != 0 && (cardControlCode & (1 << 6)) == 0) ||
                ((cardControlCode & (1 << 7)) != 0 && (cardControlCode & (1 << 6)) != 0);
    }

    /**
     * Checks if a card is blacklisted based on its control code.
     *
     * @param cardControlCode the card control code byte
     * @return {@code true} if bit 7 is set (card is blacklisted), {@code false} otherwise
     */
    public static boolean isCardBlacklisted(byte cardControlCode) {
        return (cardControlCode & (1 << 7)) != 0;
    }

    /**
     * Verifies if a card is blacklisted by checking the local database.
     *
     * @param context the Android context for database access
     * @param idi the card ID as a byte array
     * @param recycleCounter the card's recycle counter (not currently used in lookup)
     * @return {@code true} if the card is found in the blacklist database, {@code false} otherwise
     */
    public static boolean isCardBlacklistedInDB(Context context, byte[] idi, byte recycleCounter) {
        BlackListEntity blackListEntity = DatabaseHelper.getInstance(context)
                .fetchBlacklistDataByCardId(Utils.byteToHex(idi).toUpperCase());
        return blackListEntity != null;
    }

    /**
     * Determines if the transaction status is a "ride" (boarding) action.
     *
     * @param bytes the status flag bytes
     * @return {@code true} if bit 15 is set (ride status), {@code false} otherwise
     * @throws CardReadException if the bytes are null or empty
     */
    public static boolean isStatusRide(byte[] bytes) throws CardReadException {
        if (bytes == null || bytes.length == 0)
            throw new CardReadException("Please read card first");
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(bytes) & mask;
        return statusFlag != 0;
    }

    /**
     * Determines if the transaction status is an "alight" (exiting) action.
     *
     * @param bytes the status flag bytes
     * @return {@code true} if bit 15 is not set (alight status), {@code false} otherwise
     * @throws CardReadException if the bytes are null or empty
     */
    public static boolean isStatusAlight(byte[] bytes) throws CardReadException {
        if (bytes == null || bytes.length == 0)
            throw new CardReadException("Please read card first");
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(bytes) & mask;
        return statusFlag == 0;
    }

    /**
     * Checks if the transport mode is a bus based on the status flag.
     *
     * @param bytes the status flag bytes
     * @return {@code true} if bit 11 is set (bus mode), {@code false} otherwise
     * @throws CardReadException if the bytes are null or empty
     */
    public static boolean isBus(byte[] bytes) throws CardReadException {
        if (bytes == null || bytes.length == 0)
            throw new CardReadException("Please read card first");
        int mask = 1 << 11;
        int statusFlag = Utils.byteArrayToInt(bytes) & mask;
        return statusFlag != 0;
    }

    /**
     * Verifies if the equipment location matches the current device or any paired devices.
     * This ensures the transaction is occurring on the same bus or paired bus system.
     *
     * @param bytes the equipment location number as a byte array
     * @return {@code true} if the equipment matches current or paired devices, {@code false} otherwise
     */
    public static boolean isSameBus(byte[] bytes) {
        String equipmentLocationNumber = Utils.byteToHex(bytes);
        return Utils.getInstance().getDeviceInfo().getEquipmentLocationNumber().equalsIgnoreCase(equipmentLocationNumber) ||
                (Utils.getInstance().getDeviceInfo().getPairedEquipmentLocationNumber() != null &&
                        Arrays.stream(Utils.getInstance().getDeviceInfo().getPairedEquipmentLocationNumber())
                                .anyMatch(n -> n.equalsIgnoreCase(equipmentLocationNumber)));
    }

    /**
     * Checks if the service ID is eligible for a ride (boarding) transaction.
     *
     * @param accessLogInformation the gate access log containing transaction details
     * @return {@code true} if eligible for ride, {@code false} otherwise
     */
    public static boolean checkServiceIdIsEligibleForRide(GateAccessLogInformation accessLogInformation) {
        String statusFlag = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag());
        char[] bits = statusFlag.toCharArray();
        if (bits[0] == '1' && bits[4] == '0') {
            return false;
        }
        return bits[0] == '0' || bits[4] == '1';
    }

    /**
     * Checks if the service ID is eligible for an alight (exit) transaction.
     *
     * @param accessLogInformation the gate access log containing transaction details
     * @return {@code true} if eligible for alight, {@code false} otherwise
     */
    public static boolean checkServiceIdIsEligibleForAlight(GateAccessLogInformation accessLogInformation) {
        String statusFlag = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag());
        char[] bits = statusFlag.toCharArray();
        if (bits[0] == '1' && bits[4] == '0') {
            return false;
        }
        return bits[0] == '1' || bits[4] == '1';
    }

    /**
     * Checks if a station code belongs to any configured route.
     *
     * @param stationCodeToRide the station code to validate
     * @return {@code true} if the station is found in any route, {@code false} otherwise
     */
    public static boolean isSameRoute(String stationCodeToRide) {
        return Utils.getInstance().routes.stream()
                .anyMatch(r -> r.getStations().stream()
                        .anyMatch(s -> s.getStationCode().equalsIgnoreCase(stationCodeToRide)));
    }

    /**
     * Checks if two station codes belong to the same route.
     *
     * @param stationCodeToRide the current station code
     * @param previousStationCode the previous station code
     * @return {@code true} if both stations are on the same route, {@code false} otherwise
     */
    public static boolean isSameRoute(String stationCodeToRide, String previousStationCode) {
        Optional<Route> currentRoute = Utils.getInstance().routes.stream()
                .filter(r -> r.getStations().stream()
                        .anyMatch(s -> s.getStationCode().equalsIgnoreCase(stationCodeToRide)))
                .findFirst();
        Optional<Route> previousRoute = Utils.getInstance().routes.stream()
                .filter(r -> r.getStations().stream()
                        .anyMatch(s -> s.getStationCode().equalsIgnoreCase(previousStationCode)))
                .findFirst();
        return currentRoute.isPresent() && previousRoute.isPresent()
                && currentRoute.get().getId() == previousRoute.get().getId();
    }

    /**
     * Checks if the current station matches the station code to ride.
     *
     * @param currentStation the current station as a byte array
     * @param stationCodeToRide the target station code
     * @return {@code true} if the stations match, {@code false} otherwise
     */
    public static boolean isSameStation(byte[] currentStation, String stationCodeToRide) {
        return Utils.byteToHex(currentStation).equalsIgnoreCase(stationCodeToRide);
    }

    /**
     * Validates if the transaction date in stored log matches the current date.
     *
     * @param storedLogInformation the stored transaction log
     * @return {@code true} if the dates match, {@code false} otherwise
     */
    public static boolean isSameDate(StoredLogInformation storedLogInformation) {
        String date = Utils.convertByteArrayToBit(storedLogInformation.getDate());
        String time = String.format("%8s", Integer.toBinaryString(storedLogInformation.getTime() & 0xFF))
                .replace(' ', '0');

        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        String d = String.format(Locale.ENGLISH, "%02d-%02d-%02d 00:00:00", day, month, year);
        LocalDateTime previousDate = LocalDateTime.parse(d, dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return currentDate.isEqual(previousDate);
    }

    /**
     * Validates if the transaction date in gate access log matches the current date.
     *
     * @param gateAccessLogInformation the gate access log information
     * @return {@code true} if the dates match, {@code false} otherwise
     */
    public static boolean isSameDate(GateAccessLogInformation gateAccessLogInformation) {
        String date = Utils.convertByteArrayToBit(gateAccessLogInformation.getDate());

        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        String d = String.format(Locale.ENGLISH, "%02d-%02d-%02d 00:00:00", day, month, year);
        LocalDateTime previousDate = LocalDateTime.parse(d, dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return currentDate.isEqual(previousDate);
    }

    /**
     * Checks if the transaction time exceeds the default time threshold (5 minutes).
     *
     * @param gateAccessLogInformation the gate access log information
     * @return {@code true} if more than 5 minutes have passed since the transaction, {@code false} otherwise
     */
    public static boolean isGreaterThenTime(GateAccessLogInformation gateAccessLogInformation) {
        String date = Utils.convertByteArrayToBit(gateAccessLogInformation.getDate());
        String time = Utils.convertByteArrayToBit(gateAccessLogInformation.getTime());

        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);
        int hour = Integer.parseInt(time.substring(0, 8), 2);
        int minute = Integer.parseInt(time.substring(8, 16), 2);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        LocalDateTime previousDate = LocalDateTime.parse(
                String.format(Locale.ENGLISH, "%02d-%02d-%02d %02d:%02d:00", day, month, year, hour, minute),
                dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter)
                .withSecond(0).withNano(0);
        Duration duration = Duration.between(previousDate, currentDate);

        return duration.toMinutes() > TIME_DIFFERENT;
    }

    /**
     * Checks if the transaction time exceeds a configurable time threshold.
     *
     * @param gateAccessLogInformation the gate access log information
     * @param configName the master configuration name for the time threshold
     * @return {@code true} if the elapsed time exceeds the configured threshold, {@code false} otherwise
     */
    public static boolean isGreaterThenTime(GateAccessLogInformation gateAccessLogInformation, MasterConfigName configName) {
        String date = Utils.convertByteArrayToBit(gateAccessLogInformation.getDate());
        String time = Utils.convertByteArrayToBit(gateAccessLogInformation.getTime());

        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);
        int hour = Integer.parseInt(time.substring(0, 8), 2);
        int minute = Integer.parseInt(time.substring(8, 16), 2);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss", Locale.ENGLISH);
        LocalDateTime previousDate = LocalDateTime.parse(
                String.format(Locale.ENGLISH, "%02d-%02d-%02d %02d:%02d:00", day, month, year, hour, minute),
                dateTimeFormatter);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDate = LocalDateTime.parse(dateTimeFormatter.format(now), dateTimeFormatter)
                .withSecond(0).withNano(0);
        Duration duration = Duration.between(previousDate, currentDate);

        return duration.toMinutes() > Utils.getMasterConfig(configName);
    }

    /**
     * Performs common validation checks for both ride and alight transactions.
     * Combines multiple validation rules including service eligibility, bus matching, and date validation.
     *
     * @param storedLogInformation the stored transaction log
     * @param gateAccessLogInformation the gate access log information
     * @return {@code true} if all common checks pass, {@code false} otherwise
     */
    public static boolean commonCheckForRideAndAlight(StoredLogInformation storedLogInformation,
                                                      GateAccessLogInformation gateAccessLogInformation) {
        return checkServiceIdIsEligibleForRide(gateAccessLogInformation)
                && isSameBus(gateAccessLogInformation.getCurrentEquipmentLocationNumber())
                && !isSameDate(storedLogInformation);
    }

    /**
     * Determines if a route is circular based on the station code.
     * A circular route returns to its starting point.
     *
     * @param stationCode the station code to check
     * @return {@code true} if the route is circular, {@code false} otherwise
     * @throws RouteNotFoundException if no route contains the specified station code
     */
    public static boolean isCircularRoute(String stationCode) throws RouteNotFoundException {
        Optional<Route> tempRoute = Utils.getInstance().routes.stream()
                .filter(r -> r.getStations().stream()
                        .anyMatch(s -> s.getStationCode().equalsIgnoreCase(stationCode)))
                .findFirst();
        if (!tempRoute.isPresent())
            throw new RouteNotFoundException("Route not exists");

        Route route = tempRoute.get();
        return route.isCircularRoute();
    }

    /**
     * Validates if sufficient balance is available on the card for the transaction.
     * Handles both positive and two's complement negative balance representations.
     *
     * @param cardBalance the card balance as a byte array
     * @param config the master configuration name for minimum required balance
     * @return {@code true} if sufficient balance is available, {@code false} otherwise
     */
    public static boolean isEnoughBalanceAvailable(byte[] cardBalance, MasterConfigName config) {
        if ((cardBalance[cardBalance.length - 1] & 0x80) != 0) {
            return Utils.convertTwosComplementByteArrayToLittleIndian(cardBalance, cardBalance.length)
                    >= Utils.getMasterConfig(config);
        }
        int balance = Utils.charArrayToIntLE(cardBalance, cardBalance.length);
        return balance >= Utils.getMasterConfig(config);
    }

    /**
     * Checks if the negative balance value is within acceptable limits.
     * Maximum allowed negative balance is 100 units.
     *
     * @param svValue the stored value as a byte array
     * @return {@code true} if negative balance is within limits (≤100), {@code false} otherwise
     */
    public static boolean checkMaxNegativeValue(byte[] svValue) {
        int negativeBalance = Utils.charArrayToIntLE(svValue, 4);
        return negativeBalance <= 100;
    }

    /**
     * Determines if this is the card's first issuance by checking the SV log ID.
     * A log ID of 0 indicates the card has never been used.
     *
     * @param svLogId the stored value log ID as a byte array
     * @return {@code true} if this is the first issue (log ID = 0), {@code false} otherwise
     * @throws CardUnissuedException if the card validation fails (unused exception in current implementation)
     */
    public static boolean isFirstIssue(byte[] svLogId) throws CardUnissuedException {
        int svId = Utils.byteArrayToInt(svLogId);
        return svId == 0;
    }

    /**
     * Checks if the card is an MRT (Mass Rapid Transit) card based on issuer ID.
     * MRT cards have a specific issuer ID of "0A04".
     *
     * @param issuerId the issuer ID as a byte array
     * @return {@code true} if this is an MRT card, {@code false} otherwise
     * @throws CardUnissuedException if the card validation fails (unused exception in current implementation)
     */
    public static boolean isMRTCard(byte[] issuerId) throws CardUnissuedException {
        return Utils.byteToHex(issuerId).equalsIgnoreCase("0A04");
    }

    /**
     * Validates if the card's travel direction matches the expected direction.
     * Used to ensure passengers are traveling in the correct direction on their trip.
     *
     * @param cardId the card ID as a string
     * @param direction the expected travel direction
     * @return {@code true} if the card's direction matches the expected direction, {@code false} otherwise
     */
    public static boolean checkCardDirection(String cardId, RideAndAlight.Direction direction) {
        TripsEntity tripsEntity = Utils.getTripsByCardId(cardId);
        System.out.println("card direction check" + (tripsEntity != null
                && tripsEntity.direction.equalsIgnoreCase(direction.name())));
        return tripsEntity != null && tripsEntity.direction.equalsIgnoreCase(direction.name());
    }

    /**
     * Checks if the last transaction in the log list was a ride (boarding) transaction.
     * Service IDs "D220" and "D320" indicate ride transactions.
     *
     * @param storedLogInformationList the list of stored transaction logs
     * @return {@code true} if a ride transaction exists and is not the last item, {@code false} otherwise
     */
    public static boolean isLastTransactionRide(List<StoredLogInformation> storedLogInformationList) {
        for (StoredLogInformation storedLogInformation : storedLogInformationList) {
            String serviceId = String.format("%02X%02X",
                    storedLogInformation.getServiceClassificationCode(),
                    storedLogInformation.getContextCode());
            if (Arrays.asList("D220", "D320").contains(serviceId.toUpperCase())) {
                int position = storedLogInformationList.indexOf(storedLogInformation);
                return position < storedLogInformationList.size() - 1;
            }
        }
        return false;
    }
}