package com.dtca.busvalidator.busvalidatorsdk.model;

import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;
import com.dtca.busvalidator.busvalidatorsdk.db.entity.TripsEntity;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.helper.ValidateCard;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.AlightNotAllowedException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.BalanceNotAvailableException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CancelOfEntryNotAllowedException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CancelOfEntryTimeExpireException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardReadException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardWriteException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.NotSameBusException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.NotSameDateException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.RideNotAllowedException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.RouteNotFoundException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.SameStationException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.StatusNotRideException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.TypeNotSetException;
import com.dtca.busvalidator.busvalidatorsdk.model.interfac.DataInterface;
import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Main class for handling bus ride and alight operations using Felica smart cards.
 * This class manages the complete lifecycle of bus transactions including boarding (ride),
 * alighting, and cancellation of entry operations.
 *
 * <p>The class handles:</p>
 * <ul>
 *   <li>Reading and writing data to/from Felica cards</li>
 *   <li>Validating card status and transaction eligibility</li>
 *   <li>Calculating fares based on route and stations</li>
 *   <li>Managing negative balance scenarios</li>
 *   <li>Processing transaction data and generating logs</li>
 * </ul>
 *
 * <p>Service ID Codes:</p>
 * <ul>
 *   <li>D220: Ride with deduction from stored value (non-negative balance)</li>
 *   <li>D320: Ride with deduction from stored value (negative balance)</li>
 *   <li>D630: Alight with refund of stored value (non-negative balance)</li>
 *   <li>D730: Alight with refund of stored value (negative balance)</li>
 * </ul>
 *
 * @author DTCA Bus Validator Team
 * @version 1.0
 * @since 1.0
 */
@Setter
@Getter
public class RideAndAlight {

    /** Service ID for ride transaction with non-negative stored value balance */
    public static final String RIDE_AND_DEDUCTION_FROM_SV_NOT_NEGATIVE = "D220";

    /** Service ID for ride transaction with negative stored value balance */
    public static final String RIDE_AND_DEDUCTION_FROM_SV_NEGATIVE = "D320";

    /** Service ID for alight transaction with non-negative stored value balance */
    public static final String ALIGHT_AND_REFUND_OF_SV_NOT_NEGATIVE = "D630";

    /** Service ID for alight transaction with negative stored value balance */
    public static final String ALIGHT_AND_REFUND_OF_SV_NEGATIVE = "D730";

    /** Time stamp difference in hours for time calculations */
    private final int TIME_STAMP_DIFF = 8;

    /** The Felica card instance being processed */
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    FelicaCard felicaCard;

    /** Attribute information from the card including transaction ID and negative value */
    private AttributeInfo attributeInfo;

    /** Electronic purse information containing balance and execution ID */
    private EPurseInfo ePurseInfo;

    /** Stored log information for transaction history */
    private StoredLogInformation storedLogInformation;

    /** Gate access log information for current transaction */
    private GateAccessLogInformation gateAccessLogInformation;

    /** Gate access log information for transfer operations */
    private GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;

    /** Interface for reading and writing data to the card */
    @Getter(AccessLevel.NONE)
    private ReadWriteInCard readWriteInCard;

    /** Starting station/place code */
    private String startingPlace;

    /** Ending station/place code */
    private String endingPlace;

    /** Fare matrix containing fare information for different station combinations */
    private FareMatrix fareMatrix;

    /** Current ride status flag indicating if passenger is in ride mode */
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private boolean rideStatus;

    /** Type of transaction: RIDE, ALIGHT, or CANCEL_OF_ENTRY */
    private Type type;

    /** Direction of travel: UPSTREAM or DOWNSTREAM */
    private Direction direction;

    /** Ride transaction object containing boarding information */
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private Ride ride;

    /** Alight transaction object containing alighting information */
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private Alight alight;

    /** Detailed Felica card information */
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private FelicaCardDetail felicaCardDetail;

    /** Unique card identifier in hexadecimal format */
    private String cardId;

    /** Calculated fare for the journey */
    @Getter
    private Integer fare;

    /** Place 1 code for journey tracking */
    private String place1;

    /**
     * Constructs a new RideAndAlight instance and initializes card data.
     * Reads all necessary information from the Felica card including attribute info,
     * e-purse data, stored logs, and gate access logs.
     *
     * @param felicaCard The Felica card object containing card data and read/write capabilities
     * @throws Exception if card cannot be read or card detail is null
     * @throws CardReadException if card reading fails
     */
    public RideAndAlight(FelicaCard felicaCard) throws Exception {
        this.readWriteInCard = felicaCard;
        this.felicaCard = felicaCard;
        if (felicaCard.getFelicaCardDetail() == null)
            throw new CardReadException("Can not read card at this moment. please try again later");
        this.cardId = Utils.byteToHex(felicaCard.getIdi());
        this.felicaCardDetail = felicaCard.getFelicaCardDetail();
        this.attributeInfo = felicaCard.getFelicaCardDetail().getAttributeInfo();
        this.ePurseInfo = felicaCard.getFelicaCardDetail().getEPurseInfo();
        this.storedLogInformation = felicaCard.getFelicaCardDetail().getStoredLogInformation();
        this.gateAccessLogInformation = felicaCard.getFelicaCardDetail().getGateAccessLogInformation();
        this.gateAccessLogInformationForTransfer = felicaCard.getFelicaCardDetail().getGateAccessLogInformationForTransfer();
        this.rideStatus = Utils.convertByteArrayToBit(gateAccessLogInformation.getStatusFlag()).toCharArray()[15] == '1';
    }

    /**
     * Sets the station and determines the transaction type (RIDE, ALIGHT, or CANCEL_OF_ENTRY).
     * Performs comprehensive validation checks including:
     * <ul>
     *   <li>Card direction validation</li>
     *   <li>Route matching</li>
     *   <li>Bus matching</li>
     *   <li>Date validation</li>
     *   <li>Balance availability</li>
     *   <li>Transaction eligibility</li>
     * </ul>
     *
     * @param stationJson JSON string representing the station object
     * @throws TypeNotSetException if transaction type is not properly set
     * @throws CardReadException if card reading fails
     * @throws AlightNotAllowedException if alight operation is not permitted
     * @throws RouteNotFoundException if route cannot be found
     * @throws CancelOfEntryNotAllowedException if cancel of entry is not permitted
     * @throws RideNotAllowedException if ride operation is not permitted
     * @throws BalanceNotAvailableException if insufficient balance for ride
     * @throws CancelOfEntryTimeExpireException if cancel of entry time window has expired
     * @throws NotSameDateException if transaction date doesn't match
     * @throws NotSameBusException if bus doesn't match
     * @throws StatusNotRideException if card is not in ride status
     * @throws SameStationException if attempting to alight at same station as boarding
     */
    public void setStation(String stationJson) throws TypeNotSetException, CardReadException, AlightNotAllowedException, RouteNotFoundException, CancelOfEntryNotAllowedException, RideNotAllowedException, BalanceNotAvailableException, CancelOfEntryTimeExpireException, NotSameDateException, NotSameBusException, StatusNotRideException, SameStationException {

        Route.Station station = new Gson().fromJson(stationJson, Route.Station.class);
        if (type == Type.CANCEL_OF_ENTRY) {

            if (!ValidateCard.isBus(gateAccessLogInformation.getStatusFlag())) {
                throw new NotSameBusException("Not bus exception");
            }
            if (!ValidateCard.isStatusRide(gateAccessLogInformation.getStatusFlag()) || !ValidateCard.isLastTransactionRide(felicaCardDetail.getStoredLogInformationList())) {
                throw new StatusNotRideException("Not in Ride Mode");
            }
            if (!ValidateCard.isSameBus(gateAccessLogInformation.getCurrentEquipmentLocationNumber())) {
                throw new NotSameBusException("Bus not same");
            }
            if (!ValidateCard.isSameDate(gateAccessLogInformation)) {
                throw new NotSameDateException("Date is expire");
            }
            if (ValidateCard.isGreaterThenTime(this.gateAccessLogInformation, MasterConfigName.MINIMUM_CANCEL_OF_ENTRY_TIME)) {
                throw new CancelOfEntryTimeExpireException("Cancel of entry time expire");
            }
            if (!ValidateCard.isSameStation(gateAccessLogInformation.getCurrentStationCode(), station.getStationCode())) {
                throw new SameStationException("not in same station");
            }
            createAlight(station);
            return;
        } else if (!ValidateCard.checkCardDirection(Utils.byteToHex(felicaCard.getIdi()), direction) ||
                !ValidateCard.isSameRoute(station.getStationCode(), Utils.byteToHex(this.gateAccessLogInformation.getCurrentStationCode())) ||
                !ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber()) ||
                !ValidateCard.isSameDate(this.gateAccessLogInformation) ||
                ValidateCard.isStatusAlight(gateAccessLogInformation.getStatusFlag()) ||
                ValidateCard.isGreaterThenTime(this.gateAccessLogInformation, MasterConfigName.ALIGHT_EXPIRY_TIME) ||
                !ValidateCard.isLastTransactionRide(felicaCardDetail.getStoredLogInformationList())) {
            type = Type.RIDE;
        } else if (ValidateCard.isStatusRide(gateAccessLogInformation.getStatusFlag())) {
            if (!ValidateCard.isBus(gateAccessLogInformation.getStatusFlag())) {
                throw new RideNotAllowedException("Please exit from MRT first.");
            }
            type = Type.ALIGHT;
        }

        if (this.type == Type.RIDE) {
            if (!ValidateCard.isEnoughBalanceAvailable(this.storedLogInformation.getCardBalance(), MasterConfigName.MINIMUM_RIDE_BALANCE)) {
                throw new BalanceNotAvailableException("You does not have enough balance for ride. You need at least " + Utils.getMasterConfig(MasterConfigName.MINIMUM_RIDE_BALANCE) + " TK for ride.");
            }
            if (!ValidateCard.checkServiceIdIsEligibleForRide(this.gateAccessLogInformation)) {
                throw new RideNotAllowedException("You does not exit from MRT. Please first exit first from MRT");
            }

            if (!ValidateCard.isSameRoute(station.getStationCode(), Utils.byteToHex(this.gateAccessLogInformation.getCurrentStationCode())) ||
                    !ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber())) {
                createRide(station);
                return;
            }
            if (!ValidateCard.isSameDate(this.storedLogInformation)) {
                createRide(station);
                return;
            }
            createRide(station);
        } else if (this.type == Type.ALIGHT) {
            if (!ValidateCard.checkServiceIdIsEligibleForAlight(this.gateAccessLogInformation)) {
                throw new AlightNotAllowedException("You does not exit from MRT. Please first exit first from MRT");
            }
            if (Utils.getInstance().getCardList().stream().anyMatch(s -> s.equalsIgnoreCase(cardId))) {
                throw new AlightNotAllowedException("Alight not allowed at this moment. If you want alight you need to cancel of entry. Please contact with conductor");
            }
            if (ValidateCard.isGreaterThenTime(this.gateAccessLogInformation, MasterConfigName.ALIGHT_EXPIRY_TIME)) {
                throw new AlightNotAllowedException("Alight not allowed at this moment. If you want alight you need to cancel of entry. Please contact with conductor");
            }

            if (!ValidateCard.isStatusRide(this.gateAccessLogInformation.getStatusFlag())) {
                throw new AlightNotAllowedException("Alight not allowed at this moment. Please try again later");
            }

            if (ValidateCard.isSameRoute(station.getStationCode(), Utils.byteToHex(this.gateAccessLogInformation.getCurrentStationCode())) &&
                    !ValidateCard.isSameStation(this.gateAccessLogInformation.getCurrentStationCode(), station.getStationCode())) {
                createAlight(station);
                return;
            }

            if (ValidateCard.isCircularRoute(station.getStationCode())) {
                createAlight(station);
            } else {
                throw new AlightNotAllowedException("Alight not allowed at this moment. If you want alight you need to cancel of entry. Please contact with conductor");
            }
        } else {
            throw new TypeNotSetException("Type not set. Please set type first (Ride or Alight)");
        }
    }

    /**
     * Retrieves the maximum fare for the current station based on direction.
     *
     * @return Maximum fare amount for upstream or downstream direction
     * @throws AssertionError if fareMatrix or its data is null
     */
    private Integer getMaxFare() {
        assert fareMatrix != null;
        assert fareMatrix.getFareMatrix() != null;
        String stationCode;
        if (type == Type.RIDE && ride != null) {
            stationCode = ride.getStation().getStationCode();
        } else {
            stationCode = Utils.byteToHex(gateAccessLogInformation.getCurrentStationCode()).toUpperCase();
        }

        return direction == Direction.UPSTREAM ? Objects.requireNonNull(fareMatrix.getFareMatrix().get(stationCode)).get("maxFareUpStream") :
                Objects.requireNonNull(fareMatrix.getFareMatrix().get(stationCode)).get("maxFareDownStream");

    }

    /**
     * Calculates and sets the fare for the journey between boarding and alighting stations.
     * Handles special cases for circular routes where origin and destination are the same.
     *
     * @return Calculated fare amount for the journey
     * @throws AssertionError if fareMatrix, alight station, or required data is null
     */
    public Integer setFare() {
        assert fareMatrix != null;
        assert fareMatrix.getFareMatrix() != null;
        assert alight != null;
        assert alight.getStation() != null;
        if (fareMatrix.isCircular() && alight.getStation().getStationCode().equalsIgnoreCase(place1) && type.equals(Type.ALIGHT)) {
            return Objects.requireNonNull(fareMatrix.getFareMatrix()
                    .get(alight.getStation().getStationCode())).get("maxFareUpStream");
        }
        return Objects.requireNonNull(fareMatrix.getFareMatrix().get(Utils.byteToHex(gateAccessLogInformation.getCurrentStationCode())
                .toUpperCase())).get(alight.getStation().getStationCode());

    }

    /**
     * Writes transaction data to the Felica card and sends transaction information to the backend.
     * This method handles the complete write operation including:
     * <ul>
     *   <li>Preparing service codes and block numbers for multiple card files</li>
     *   <li>Writing data to attribute info, e-purse, stored log, and gate access files</li>
     *   <li>Sending transaction data through the provided interface</li>
     *   <li>Error handling and rollback on write failures</li>
     * </ul>
     *
     * @param dataInterface Interface for sending transaction data to backend systems
     * @throws Exception if any error occurs during card write operation
     * @throws CardWriteException if data cannot be written to the card
     * @throws TypeNotSetException if transaction type is not set
     */
    public void writeData(DataInterface dataInterface) throws Exception {
        long sTime = System.currentTimeMillis();
        int serviceNumber = 5;
        byte[] serviceCodeList = new byte[serviceNumber * 4];
        serviceCodeList[0] = (byte) 0x08; // Attribute information file
        serviceCodeList[1] = (byte) 0x13; // Attribute information file
        serviceCodeList[2] = (byte) 0x01; // Version key
        serviceCodeList[3] = (byte) 0x00; // Version key
        serviceCodeList[4] = (byte) 0x10; // e-Purse  file
        serviceCodeList[5] = (byte) 0x14; // e-Purse  file
        serviceCodeList[6] = (byte) 0x01; // Version key
        serviceCodeList[7] = (byte) 0x00; // Version key
        serviceCodeList[8] = (byte) 0x0C; // Stored value log information file
        serviceCodeList[9] = (byte) 0x22; // Stored value log information file
        serviceCodeList[10] = (byte) 0x01; // Version key
        serviceCodeList[11] = (byte) 0x00; // Version key
        serviceCodeList[12] = (byte) 0x0C; // Gate access log file
        serviceCodeList[13] = (byte) 0x25; // Gate access log file
        serviceCodeList[14] = (byte) 0x01; // Version key
        serviceCodeList[15] = (byte) 0x00; // Version key
        serviceCodeList[16] = (byte) 0x08;  // Gate access log file (for transfer)
        serviceCodeList[17] = (byte) 0x26; // Gate access log file (for transfer)
        serviceCodeList[18] = (byte) 0x01; // Version key
        serviceCodeList[19] = (byte) 0x00; // Version key

        int blockNumber = 6;
        byte[] blockNumberList = new byte[blockNumber * 2];
        blockNumberList[0] = (byte) 0x81; // Attribute information file
        blockNumberList[1] = (byte) 0x00;
        blockNumberList[2] = (byte) 0x81;
        blockNumberList[3] = (byte) 0x01;
        blockNumberList[4] = (byte) 0x82; // e-Purse  file
        blockNumberList[5] = (byte) 0x00;
        blockNumberList[6] = (byte) 0x84; // Stored value log information file
        blockNumberList[7] = (byte) 0x00;
        blockNumberList[8] = (byte) 0x85; // Gate access log file
        blockNumberList[9] = (byte) 0x00;
        blockNumberList[10] = (byte) 0x86; // Gate access log file (for transfer)
        blockNumberList[11] = (byte) 0x00;

        if (type == Type.RIDE) {
            byte[] bytes = ride.getByteData();
            int i = readWriteInCard.writeInCard(serviceNumber, serviceCodeList, blockNumber, blockNumberList, bytes);
            if (i != 1) {
                sendTransactionData(dataInterface, false);
                throw new CardWriteException("Can not write in card at this moment. Please try again later");
            }
            sendTransactionData(dataInterface, true);
        } else if (type == Type.ALIGHT || type == Type.CANCEL_OF_ENTRY) {
            int i = readWriteInCard.writeInCard(serviceNumber, serviceCodeList, blockNumber, blockNumberList, alight.getByteData());
            if (i != 1) {
                sendTransactionData(dataInterface, false);
                throw new CardWriteException("Can not write in card at this moment. Please try again later");
            }
            sendTransactionData(dataInterface, true);
        } else {
            throw new TypeNotSetException("No type set");
        }
        System.out.println("#RD>>> Card Write Time: "+(System.currentTimeMillis()-sTime));
    }

    /**
     * Sends transaction data to the backend system through the provided data interface.
     * Creates a comprehensive transaction record including card details, balance changes,
     * location information, and metadata for auditing and reconciliation.
     *
     * @param dataInterface Interface for transmitting transaction data
     * @param isProcessed Flag indicating whether the transaction was successfully processed
     */
    private void sendTransactionData(@NonNull DataInterface dataInterface, boolean isProcessed) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        int initialBalance = this.type == Type.RIDE ? ride.getInitialBalance() : alight.getInitialBalance();
        int initialNegativeBalance = type == Type.RIDE ? ride.getInitialNegativeBalance() : alight.getInitialNegativeBalance();
        String metaData = Utils.getDeviceSerialNo() + "."
                + Utils.byteToHex(felicaCard.getIdi())
                + String.format("%02X", felicaCardDetail.getIssuerInfo().getRecycleCounter())
                + Utils.byteToHex(attributeInfo.getTxnDataId())
                + Utils.byteToHex(new byte[]{storedLogInformation.getServiceClassificationCode(), storedLogInformation.getContextCode()})
                + dateTimeFormatter.format(localDateTime)
                + (isProcessed ? "00" : "01")
                + Utils.byteToHex(storedLogInformation.getStoredValueLogId())
                + String.format("%06X", Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4) & 0XFFFFFF)
                + String.format("%06X", (Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4) - initialBalance) & 0XFFFFFF)
                + Utils.byteToHex(storedLogInformation.getPlace1())
                + Utils.byteToHex(storedLogInformation.getPlace2())
                + String.format("%04X", Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2) & 0XFFFF)
                + String.format("%04X", (Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2) - initialNegativeBalance) & 0xFFFF)
                .toUpperCase();

        TransactionData transactionData = TransactionData.builder()
                .cardId(this.cardId)
                .recycleCounter(Utils.byteToHex(new byte[]{felicaCardDetail.getIssuerInfo().getRecycleCounter()}))
                .transactionDataId(Utils.byteArrayToInt(attributeInfo.getTxnDataId()))
                .serviceId(Utils.byteToHex(new byte[]{storedLogInformation.getServiceClassificationCode(), storedLogInformation.getContextCode()}))
                .dateTimeStamp(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))
                .cardFunctionCode(Utils.byteArrayToInt(attributeInfo.getCardFunctionCode()))
                .cardControlCode(attributeInfo.getCardControlCode() & 0XFF)
                .discountCode(attributeInfo.getDiscountCode() & 0XFF)
                .cardExpirationDate(Utils.byteToHex(attributeInfo.getExpiryDate()))
                .processUnfinishedFlag(isProcessed ? 0x00 : 0x01)
                .deviceSerialNumber(Utils.getDeviceSerialNo())
                .svLogId(Utils.byteArrayToInt(ePurseInfo.getBinExecutionId()))
                .svBalance(Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4))
                .svSpent(Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4) - initialBalance)
                .processedLocation1(Utils.byteToHex(storedLogInformation.getPlace1()))
                .processedLocation2(Utils.byteToHex(storedLogInformation.getPlace2()))
                .statusFlag(Utils.byteArrayToInt(gateAccessLogInformation.getStatusFlag()))
                .basicFareAmount(Utils.charArrayToIntLE(gateAccessLogInformation.getAmountOfBasicFare(), 3))
                .distanceFareAmount(Utils.convertTwosComplementByteArrayToLittleIndian(gateAccessLogInformation.getAmountOfDistanceFare(), 3))
                .discountFareAmount(0)
                .inStoppage(Utils.byteToHex(storedLogInformation.getPlace1()))
                .outStoppage(Utils.byteToHex(storedLogInformation.getPlace2()))
                .negativeValue(Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2))
                .negativeValueUsed(Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2) - initialNegativeBalance)
                .message(type == Type.ALIGHT &&
                        Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformation.getCardBalance(), 3) < Utils.getMasterConfig(MasterConfigName.MINIMUM_RIDE_BALANCE) ?
                        "card17" : "NA")
                .metaData(metaData.toUpperCase())
                .build();

        dataInterface.receiveTransactionData(transactionData);
    }

    /**
     * Creates a new ride transaction for the given station.
     * Initializes the ride object and updates the card list and trip tracking.
     *
     * @param station The station where the ride is initiated
     */
    private void createRide(Route.Station station) {
        Utils.delete(Utils.byteToHex(felicaCard.getIdi()));
        if (direction != null) {
            Utils.updateOrInsertTrips(Utils.byteToHex(felicaCard.getIdi()), direction);
        }
        this.type = Type.RIDE;
        Utils.getInstance().getCardList().add(cardId);
        this.ride = new Ride(felicaCardDetail);
        this.ride.setStation(station);
    }

    /**
     * Creates a new alight transaction for the given station.
     * Updates card direction and retrieves the origin station from trip records.
     *
     * @param station The station where the passenger alights
     */
    private void createAlight(Route.Station station) {
        if (direction != null) {
            Utils.updateCardDirection(Utils.byteToHex(felicaCard.getIdi()), direction);
        }
        TripsEntity tripsEntity = Utils.getTripsByCardId(Utils.byteToHex(felicaCard.getIdi()));
        if(tripsEntity != null && tripsEntity.fromStation != null){
            this.place1 = tripsEntity.fromStation;
        }
        else {
            this.place1 = Utils.byteToHex(gateAccessLogInformation.getCurrentStationCode());
        }
        this.alight = new Alight(felicaCardDetail);
        this.alight.setStation(station);
    }

    /**
     * Enum representing the direction of travel on the route.
     */
    public enum Direction {
        /** Upstream direction (typically away from city center or towards route start) */
        UPSTREAM,
        /** Downstream direction (typically towards city center or route end) */
        DOWNSTREAM
    }

    /**
     * Enum representing the type of transaction being processed.
     */
    public enum Type {
        /** Boarding/entry transaction */
        RIDE,
        /** Alighting/exit transaction */
        ALIGHT,
        /** Cancellation of entry transaction */
        CANCEL_OF_ENTRY
    }

    /**
     * Inner class representing a ride (boarding) transaction.
     * Handles all data updates and calculations required when a passenger boards the bus.
     *
     * <p>This class manages:</p>
     * <ul>
     *   <li>Updating card attribute information including transaction ID</li>
     *   <li>Deducting maximum fare from e-purse balance</li>
     *   <li>Recording stored value log with ride details</li>
     *   <li>Updating gate access log with entry information</li>
     *   <li>Handling negative balance scenarios</li>
     * </ul>
     */
    @Setter
    @Getter
    private class Ride {
        /** Initial balance before the ride transaction */
        private final int initialBalance;

        /** Initial negative balance before the ride transaction */
        private final int initialNegativeBalance;

        /** Card attribute information */
        private AttributeInfo attributeInfo;

        /** Electronic purse information */
        private EPurseInfo ePurseInfo;

        /** Stored log information */
        private StoredLogInformation storedLogInformation;

        /** Gate access log information */
        private GateAccessLogInformation gateAccessLogInformation;

        /** Gate access log for transfer operations */
        private GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;

        /** Service ID for this transaction */
        private String serviceId;

        /** Ride status flag */
        private boolean rideStatus;

        /** Flag indicating if balance will be negative after fare deduction */
        @Setter(AccessLevel.NONE)
        @Getter(AccessLevel.NONE)
        private boolean isNegative;

        /** Station where ride is initiated */
        @Getter(AccessLevel.PRIVATE)
        private Route.Station station;

        /**
         * Constructs a new Ride transaction object.
         * Initializes all card information and stores initial balance values.
         *
         * @param felicaCardDetail The detailed Felica card information
         */
        public Ride(@NonNull FelicaCardDetail felicaCardDetail) {
            this.attributeInfo = felicaCardDetail.getAttributeInfo();
            this.ePurseInfo = felicaCardDetail.getEPurseInfo();
            this.storedLogInformation = felicaCardDetail.getStoredLogInformation();
            this.gateAccessLogInformation = felicaCardDetail.getGateAccessLogInformation();
            this.gateAccessLogInformationForTransfer = felicaCardDetail.getGateAccessLogInformationForTransfer();
            this.initialBalance = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            this.initialNegativeBalance = Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2);
        }

        /**
         * Updates the attribute information for the ride transaction.
         * Increments transaction ID and handles negative balance scenarios.
         */
        private void updateAttributeInfo() {
            if (attributeInfo != null) {
                int transId = Utils.byteToInteger(attributeInfo.getTxnDataId()) + 1;
                attributeInfo.setTxnDataId(Utils.hexToByte(String.format("%04X", transId)));
                if (isNegative) {
                    int maxFare = RideAndAlight.this.getMaxFare();
                    int negativeValue = Math.abs(maxFare - Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4));
                    attributeInfo.setNegativeValue(Utils.intToCharArrayLE(negativeValue, 2));
                } else {
                    attributeInfo.setNegativeValue(new byte[2]);
                }
            }
        }

        /**
         * Sets the boarding station and determines if the balance will be negative.
         * Updates the trip record with the from station.
         *
         * @param station The station where passenger boards
         */
        public void setStation(Route.Station station) {
            this.station = station;
            Utils.updateFromStation(Utils.byteToHex(felicaCard.getIdi()), station.getStationCode());
            this.isNegative = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4) - (RideAndAlight.this.getMaxFare() +
                    initialNegativeBalance) < 0;
        }

        /**
         * Updates the e-purse information for the ride transaction.
         * Deducts maximum fare and handles cashback data.
         */
        private void updateEPurseInfo() {
            assert ePurseInfo != null;
            int executionId = Utils.byteToInteger(ePurseInfo.getBinExecutionId()) + 1;
            ePurseInfo.setBinExecutionId(Utils.hexToByte(String.format("%04X", executionId)));

            int maxFare = RideAndAlight.this.getMaxFare();
            int sv = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            if (isNegative) {
                ePurseInfo.setBinCashbackData(Utils.intToCharArrayLE(sv));
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(0));
                Utils.updateCashBackAmount(Utils.byteToHex(felicaCard.getIdi()), sv);
            } else {
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(Math.max(sv - maxFare, 0)));
                ePurseInfo.setBinCashbackData(Utils.intToCharArrayLE(maxFare));
                Utils.updateCashBackAmount(Utils.byteToHex(felicaCard.getIdi()), maxFare);
            }
        }

        /**
         * Updates the stored value log information for the ride transaction.
         * Records equipment codes, service codes, timestamp, and location data.
         */
        private void updateStoredValueLog() {
            assert storedLogInformation != null;
            storedLogInformation.setEquipmentClassificationCode((byte) 0x08);
            storedLogInformation.setServiceClassificationCode((byte) 0x52);
            storedLogInformation.setContextCode((byte) 0x20);
            storedLogInformation.setPaymentMethodCode((byte) 0x00);
            Map<String, String> date = Utils.getYearMonthDateHourMinute();
            String day = date.get("year") + date.get("month") + date.get("day");
            day = String.format("%04X", Integer.parseInt(day, 2));
            storedLogInformation.setDate(Utils.hexToByte(day));
            String time = date.get("hour") + date.get("minute") + "00000";
            storedLogInformation.setTime(Utils.hexToByte(String.format("%02X", Integer.parseInt(date.get("hour") + "000", 2)))[0]);
            storedLogInformation.setPlace1(Utils.hexToByte(station.getStationCode()));
            storedLogInformation.setPlace2(Utils.hexToByte("0000"));
            storedLogInformation.setStoredValueLogId(ePurseInfo.getBinExecutionId());

            if (isNegative) {
                storedLogInformation.setServiceClassificationCode((byte) 0xD3);
                storedLogInformation.setCardBalance(Utils.convertToTwosComplementLE(-Integer.parseInt(Utils.byteToHex(Utils.reverseArray(attributeInfo.getNegativeValue())), 16), 3));
            } else {
                storedLogInformation.setServiceClassificationCode((byte) 0xD2);
                storedLogInformation.setCardBalance(Arrays.copyOfRange(ePurseInfo.getBinRemainingSV(), 0, 3));
            }
        }

        /**
         * Updates the gate access log information for the ride transaction.
         * Records status flags, timestamp, station code, and fare information.
         */
        private void updateAccessLogInformation() {
            assert gateAccessLogInformation != null;
            String statusFlagInBit = "110010" + (attributeInfo.getDiscountCode() == 0x00 ? "0" : "1") + "000000000";
            gateAccessLogInformation.setStatusFlag(Utils.hexToByte(String.format("%04X", Integer.parseInt(statusFlagInBit, 2))));
            Map<String, String> date = Utils.getYearMonthDateHourMinute();
            String day = date.get("year") + date.get("month") + date.get("day");
            gateAccessLogInformation.setDate(Utils.hexToByte(String.format("%04X", Integer.parseInt(day, 2))));
            String time = "000" + date.get("hour") + "00" + date.get("minute");
            gateAccessLogInformation.setTime(Utils.hexToByte(String.format("%04X", Integer.parseInt(time, 2))));
            gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte(station.getStationCode()));
            gateAccessLogInformation.setCurrentEquipmentLocationNumber(Utils.hexToByte(Utils.getInstance().getDeviceInfo().getEquipmentLocationNumber()));
            gateAccessLogInformation.setAmountOfBasicFare(Utils.hexToByte("000000"));

            int maxFare = RideAndAlight.this.getMaxFare();
            gateAccessLogInformation.setAmountOfDistanceFare(Arrays.copyOfRange(Utils.intToCharArrayLE(maxFare), 0, 3));
        }

        /**
         * Updates the transfer access log information for the ride transaction.
         * Records origin station and fare allocation details.
         */
        private void updateTransferAccessLogInformation() {
            assert gateAccessLogInformationForTransfer != null;
            int maxFare = RideAndAlight.this.getMaxFare();
            GateAccessLogInformationForTransfer.Block0 block0 = new GateAccessLogInformationForTransfer.Block0();
            block0.setReserved(new byte[]{0x00, 0x00});
            block0.setOriginStation(Utils.hexToByte(station.getStationCode()));
            block0.setTransferStation1(new byte[]{0x00, 0x00});
            block0.setTransferStation2(new byte[]{0x00, 0x00});
            block0.setTransferStation3(new byte[]{0x00, 0x00});
            block0.setFareAllocationAmountForOwnLine(Arrays.copyOfRange(Utils.intToCharArrayLE(maxFare), 0, 3));
            block0.setFareAllocationAmountForOtherLine(new byte[]{0x00, 0x00, 0x00});

            GateAccessLogInformationForTransfer.Block1 block1 = new GateAccessLogInformationForTransfer.Block1();
            block1.setReserved(Utils.hexToByte(String.format("%022X", 0)));
            block1.setAmountOfTemporaryFare(Utils.hexToByte(String.format("%06X", 0)));
            block1.setStationForTemporaryFareCalculation(Utils.hexToByte(String.format("%04X", 0)));
            gateAccessLogInformationForTransfer.setBlock0(block0);
            gateAccessLogInformationForTransfer.setBlock1(block1);
        }

        /**
         * Updates all data components for the ride transaction.
         * Calls all individual update methods in sequence.
         */
        private void updateData() {
            updateAttributeInfo();
            updateEPurseInfo();
            updateStoredValueLog();
            updateAccessLogInformation();
            updateTransferAccessLogInformation();
        }

        /**
         * Generates the complete byte array data for writing to the card.
         * Combines all updated information into a single byte buffer.
         *
         * @return Byte array containing all ride transaction data
         */
        public byte[] getByteData() {
            updateData();

            byte[] attributeInfoData = attributeInfo.getData();
            byte[] ePurseData = ePurseInfo.getData();
            byte[] storageInfoData = storedLogInformation.getData();
            byte[] gateAccessLogData = gateAccessLogInformation.getData();
            byte[] gateAccessLogTransferData = gateAccessLogInformationForTransfer.getBlock0Data();

            ByteBuffer byteBuffer = ByteBuffer.allocate(attributeInfoData.length + ePurseData.length + storageInfoData.length + gateAccessLogData.length + gateAccessLogTransferData.length);
            byteBuffer.put(attributeInfoData);
            byteBuffer.put(ePurseData);
            byteBuffer.put(storageInfoData);
            byteBuffer.put(gateAccessLogData);
            byteBuffer.put(gateAccessLogTransferData);
            return byteBuffer.array();
        }
    }

    /**
     * Inner class representing an alight (exit) transaction.
     * Handles all data updates and calculations required when a passenger exits the bus.
     *
     * <p>This class manages:</p>
     * <ul>
     *   <li>Calculating actual fare based on origin and destination stations</li>
     *   <li>Refunding excess fare (difference between maximum and actual fare)</li>
     *   <li>Updating card balance with refund or additional deduction</li>
     *   <li>Recording stored value log with alight details</li>
     *   <li>Handling negative balance scenarios</li>
     * </ul>
     */
    @Setter
    @Getter
    private class Alight {
        /** Card attribute information */
        private final AttributeInfo attributeInfo;

        /** Electronic purse information */
        private final EPurseInfo ePurseInfo;

        /** Stored log information */
        private final StoredLogInformation storedLogInformation;

        /** Gate access log information */
        private final GateAccessLogInformation gateAccessLogInformation;

        /** Gate access log for transfer operations */
        private final GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;

        /** Initial balance before the alight transaction */
        private final int initialBalance;

        /** Initial negative balance before the alight transaction */
        private final int initialNegativeBalance;

        /** Service ID for this transaction */
        private String serviceId;

        /** Amount to be refunded (cashback) */
        private int cashbackAmount;

        /** Alight status flag */
        private boolean alightStatus;

        /** Flag indicating if balance will be negative after fare settlement */
        @Setter(AccessLevel.NONE)
        @Getter(AccessLevel.NONE)
        private boolean isNegative;

        /** Station where passenger alights */
        @Getter(AccessLevel.PRIVATE)
        private Route.Station station;

        /**
         * Constructs a new Alight transaction object.
         * Initializes all card information and determines cashback amount based on service ID.
         *
         * @param felicaCardDetail The detailed Felica card information
         */
        public Alight(@NonNull FelicaCardDetail felicaCardDetail) {
            this.attributeInfo = felicaCardDetail.getAttributeInfo();
            this.ePurseInfo = felicaCardDetail.getEPurseInfo();
            this.storedLogInformation = felicaCardDetail.getStoredLogInformation();
            this.gateAccessLogInformation = felicaCardDetail.getGateAccessLogInformation();
            this.gateAccessLogInformationForTransfer = felicaCardDetail.getGateAccessLogInformationForTransfer();
            this.initialBalance = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            this.initialNegativeBalance = Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2);
            this.serviceId = Utils.getServiceId(storedLogInformation);
            if (Arrays.stream(new String[]{"d220", "d320"}).noneMatch(s -> s.equalsIgnoreCase(serviceId))) {
                this.cashbackAmount = felicaCard.getCashbackAmount();
            } else {
                this.cashbackAmount = Utils.charArrayToIntLE(ePurseInfo.getBinCashbackData(), 4);
            }
        }

        /**
         * Sets the alighting station and calculates final fare.
         * Determines if the final balance will be negative.
         *
         * @param station The station where passenger alights
         */
        public void setStation(Route.Station station) {
            this.station = station;
            Utils.updateToStation(Utils.byteToHex(felicaCard.getIdi()), station.getStationCode());
            int sv = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            RideAndAlight.this.fare = setFare();
            this.isNegative = sv + cashbackAmount - RideAndAlight.this.fare < 0;
        }

        /**
         * Updates the attribute information for the alight transaction.
         * Increments transaction ID and handles negative balance scenarios.
         */
        private void updateAttributeInfo() {
            if (attributeInfo != null) {
                int transId = Utils.byteToInteger(attributeInfo.getTxnDataId()) + 1;
                attributeInfo.setTxnDataId(Utils.hexToByte(String.format("%04X", transId)));
                if (isNegative) {
                    int fare = RideAndAlight.this.getFare();
                    int negativeValue = Math.abs(fare - cashbackAmount);
                    attributeInfo.setNegativeValue(Utils.intToCharArrayLE(negativeValue, 2));
                } else {
                    attributeInfo.setNegativeValue(new byte[2]);
                }
            }
        }

        /**
         * Updates the e-purse information for the alight transaction.
         * Calculates final balance with fare adjustment and clears cashback data.
         */
        private void updateEPurseInfo() {
            assert ePurseInfo != null;
            int executionId = Utils.byteToInteger(ePurseInfo.getBinExecutionId()) + 1;
            ePurseInfo.setBinExecutionId(Utils.hexToByte(String.format("%04X", executionId)));

            int sv = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            int fare = RideAndAlight.this.getFare();
            if (isNegative) {
                ePurseInfo.setBinCashbackData(Utils.intToCharArrayLE(0));
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(0));
            } else {
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(sv + cashbackAmount - fare));
                ePurseInfo.setBinCashbackData(new byte[4]);
            }
        }

        /**
         * Updates the stored value log information for the alight transaction.
         * Records equipment codes, service codes, timestamp, and both origin and destination.
         */
        private void updateStoredValueLog() {
            assert storedLogInformation != null;
            String serviceId = String.format("%02X%02X",storedLogInformation.getServiceClassificationCode(), storedLogInformation.getContextCode());
            if(!Arrays.asList(RIDE_AND_DEDUCTION_FROM_SV_NOT_NEGATIVE,RIDE_AND_DEDUCTION_FROM_SV_NEGATIVE).contains(serviceId.toUpperCase())){
                byte[] place1 = getPlace1();
                if(place1 != null) {
                    storedLogInformation.setPlace1(place1);
                }
            }
            storedLogInformation.setEquipmentClassificationCode((byte) 0x42);
            storedLogInformation.setContextCode((byte) 0x30);
            storedLogInformation.setPaymentMethodCode((byte) 0x00);
            Map<String, String> date = Utils.getYearMonthDateHourMinute();
            String day = date.get("year") + date.get("month") + date.get("day");
            day = String.format("%04X", Integer.parseInt(day, 2));
            storedLogInformation.setDate(Utils.hexToByte(day));
            storedLogInformation.setPlace2(Utils.hexToByte(station.getStationCode()));
            String time = date.get("hour") + date.get("minute") + "00000";
            storedLogInformation.setTime(Utils.hexToByte(String.format("%02X", Integer.parseInt(date.get("hour") + "000", 2)))[0]);
            storedLogInformation.setStoredValueLogId(ePurseInfo.getBinExecutionId());

            if (isNegative) {
                storedLogInformation.setServiceClassificationCode((byte) 0xD7);
                storedLogInformation.setCardBalance(Utils.convertToTwosComplementLE(-Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2), 3));
            } else {
                storedLogInformation.setServiceClassificationCode((byte) 0xD6);
                storedLogInformation.setCardBalance(Arrays.copyOfRange(ePurseInfo.getBinRemainingSV(), 0, 3));
            }
        }

        /**
         * Updates the gate access log information for the alight transaction.
         * Records status flags, timestamp, station code, and final fare calculation.
         */
        private void updateAccessLogInformation() {
            assert gateAccessLogInformation != null;
            String statusFlagInBit = "010010" + (attributeInfo.getDiscountCode() == 0x00 ? "0" : "1") + "000000000";
            gateAccessLogInformation.setStatusFlag(Utils.hexToByte(String.format("%04X", Integer.parseInt(statusFlagInBit, 2))));
            Map<String, String> date = Utils.getYearMonthDateHourMinute();
            String day = date.get("year") + date.get("month") + date.get("day");
            gateAccessLogInformation.setDate(Utils.hexToByte(String.format("%04X", Integer.parseInt(day, 2))));
            String time = "000" + date.get("hour") + "00" + date.get("minute");
            gateAccessLogInformation.setTime(Utils.hexToByte(String.format("%04X", Integer.parseInt(time, 2))));
            gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte(station.getStationCode()));
            gateAccessLogInformation.setCurrentEquipmentLocationNumber(Utils.hexToByte(Utils.getInstance().getDeviceInfo().getEquipmentLocationNumber()));
            gateAccessLogInformation.setAmountOfBasicFare(Utils.hexToByte("000000"));

            int maxFare = Utils.charArrayToIntLE(gateAccessLogInformation.getAmountOfDistanceFare(), 3);
            gateAccessLogInformation.setAmountOfDistanceFare(RideAndAlight.this.getFare() - maxFare < 0 ? Utils.convertToTwosComplementLE(RideAndAlight.this.getFare() - maxFare, 3) : Arrays.copyOfRange(Utils.intToCharArrayLE(RideAndAlight.this.getFare() - maxFare), 0, 3));
        }

        /**
         * Updates the transfer access log information for the alight transaction.
         * Records origin station and fare allocation details including fare adjustment.
         */
        private void updateTransferAccessLogInformation() {
            assert gateAccessLogInformationForTransfer != null;
            int maxFare = RideAndAlight.this.getMaxFare();
            GateAccessLogInformationForTransfer.Block0 block0 = new GateAccessLogInformationForTransfer.Block0();
            block0.setReserved(new byte[]{0x00, 0x00});
            block0.setOriginStation(Utils.hexToByte(station.getStationCode()));
            block0.setTransferStation1(new byte[]{0x00, 0x00});
            block0.setTransferStation2(new byte[]{0x00, 0x00});
            block0.setTransferStation3(new byte[]{0x00, 0x00});
            block0.setFareAllocationAmountForOwnLine(RideAndAlight.this.getFare() - maxFare < 0 ? Utils.convertToTwosComplementLE(RideAndAlight.this.getFare() - maxFare, 3) : Arrays.copyOfRange(Utils.intToCharArrayLE(RideAndAlight.this.getFare() - maxFare), 0, 3));
            block0.setFareAllocationAmountForOtherLine(new byte[]{0x00, 0x00, 0x00});

            GateAccessLogInformationForTransfer.Block1 block1 = new GateAccessLogInformationForTransfer.Block1();
            block1.setReserved(Utils.hexToByte(String.format("%022X", 0)));
            block1.setAmountOfTemporaryFare(Utils.hexToByte(String.format("%06X", 0)));
            block1.setStationForTemporaryFareCalculation(Utils.hexToByte(String.format("%04X", 0)));
            gateAccessLogInformationForTransfer.setBlock0(block0);
            gateAccessLogInformationForTransfer.setBlock1(block1);
        }

        /**
         * Updates all data components for the alight transaction.
         * Calls all individual update methods in sequence.
         */
        private void updateData() {
            updateAttributeInfo();
            updateEPurseInfo();
            updateStoredValueLog();
            updateAccessLogInformation();
            updateTransferAccessLogInformation();
        }

        /**
         * Generates the complete byte array data for writing to the card.
         * Combines all updated information into a single byte buffer.
         *
         * @return Byte array containing all alight transaction data
         */
        public byte[] getByteData() {
            updateData();

            byte[] attributeInfoData = attributeInfo.getData();
            byte[] ePurseData = ePurseInfo.getData();
            byte[] storageInfoData = storedLogInformation.getData();
            byte[] gateAccessLogData = gateAccessLogInformation.getData();
            byte[] gateAccessLogTransferData = gateAccessLogInformationForTransfer.getData();

            ByteBuffer byteBuffer = ByteBuffer.allocate(attributeInfoData.length + ePurseData.length + storageInfoData.length + gateAccessLogData.length + gateAccessLogTransferData.length);
            byteBuffer.put(attributeInfoData);
            byteBuffer.put(ePurseData);
            byteBuffer.put(storageInfoData);
            byteBuffer.put(gateAccessLogData);
            byteBuffer.put(gateAccessLogTransferData);
            return byteBuffer.array();
        }

        /**
         * Retrieves the Place1 (origin station) from stored log information.
         * Searches through the log history to find the most recent ride transaction.
         *
         * @return Byte array containing Place1 station code, or null if not found
         */
        private byte[] getPlace1() {
            List<StoredLogInformation> storedLogInformationList = felicaCardDetail.getStoredLogInformationList();
            for(StoredLogInformation storedLogInformation : storedLogInformationList){
                String serviceId = String.format("%02X%02X",storedLogInformation.getServiceClassificationCode(),
                        storedLogInformation.getContextCode());
                if(Arrays.asList("D220","D320").contains(serviceId.toUpperCase())){
                    return storedLogInformation.getPlace1();
                }
            }

            return null;
        }
    }
}