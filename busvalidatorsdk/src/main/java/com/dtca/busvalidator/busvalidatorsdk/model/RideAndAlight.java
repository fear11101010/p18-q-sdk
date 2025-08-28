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

@Setter
@Getter
public class RideAndAlight {
    public static final String RIDE_AND_DEDUCTION_FROM_SV_NOT_NEGATIVE = "D220";
    public static final String RIDE_AND_DEDUCTION_FROM_SV_NEGATIVE = "D320";
    public static final String ALIGHT_AND_REFUND_OF_SV_NOT_NEGATIVE = "D630";
    public static final String ALIGHT_AND_REFUND_OF_SV_NEGATIVE = "D730";

    private final int TIME_STAMP_DIFF = 8;
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    FelicaCard felicaCard;
    private AttributeInfo attributeInfo;
    private EPurseInfo ePurseInfo;
    private StoredLogInformation storedLogInformation;
    private GateAccessLogInformation gateAccessLogInformation;
    private GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;
    @Getter(AccessLevel.NONE)
    private ReadWriteInCard readWriteInCard;
    private String startingPlace;
    private String endingPlace;
    private FareMatrix fareMatrix;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private boolean rideStatus;
    private Type type;
    private Direction direction;
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private Ride ride;
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private Alight alight;
    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private FelicaCardDetail felicaCardDetail;
    private String cardId;
    @Getter
    private Integer fare;


    private String place1;

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
//            this.serviceId = Utils.byteToHex(new byte[]{storedLogInformation.getServiceClassificationCode(), storedLogInformation.getContextCode()});
        this.rideStatus = Utils.convertByteArrayToBit(gateAccessLogInformation.getStatusFlag()).toCharArray()[15] == '1';
        /*if(type==Type.CANCEL_OF_ENTRY && (!ValidateCard.isBus(gateAccessLogInformation.getStatusFlag()) ||
                !ValidateCard.isStatusRide(gateAccessLogInformation.getStatusFlag()) ||
                !ValidateCard.isSameBus(gateAccessLogInformation.getCurrentEquipmentLocationNumber()) ||
                !ValidateCard.isSameDate(storedLogInformation) ||
                !ValidateCard.isGreaterThenTime(this.gateAccessLogInformation))){
            throw new CancelOfEntryNotAllowedException("Cancel of entry not allowed at this moment. Please try again later");

        }*/
        /*else if (!ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber()) || !ValidateCard.isSameDate(this.storedLogInformation) || ValidateCard.isStatusAlight(gateAccessLogInformation.getStatusFlag())) {
            type = Type.RIDE;
        } else if (ValidateCard.isStatusRide(gateAccessLogInformation.getStatusFlag())) {
            if(!ValidateCard.isBus(gateAccessLogInformation.getStatusFlag())){
               throw new RideNotAllowedException("Please exit from MRT first.");
            }
            type = Type.ALIGHT;
        }*/
    }

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
//            if(!ValidateCard.isSameDate(storedLogInformation)){
            if (!ValidateCard.isSameDate(gateAccessLogInformation)) {
                throw new NotSameDateException("Date is expire");
            }
            if (ValidateCard.isGreaterThenTime(this.gateAccessLogInformation, MasterConfigName.MINIMUM_CANCEL_OF_ENTRY_TIME)) {
                throw new CancelOfEntryTimeExpireException("Cancel of entry time expire");
            }
//            if(!ValidateCard.isSameStation(storedLogInformation.getPlace1(),station.getStationCode())){
            if (!ValidateCard.isSameStation(gateAccessLogInformation.getCurrentStationCode(), station.getStationCode())) {
                throw new SameStationException("not in same station");
            }
            createAlight(station);
            return;
        } else if (!ValidateCard.checkCardDirection(Utils.byteToHex(felicaCard.getIdi()), direction) ||
//                !ValidateCard.isSameRoute(station.getStationCode(),Utils.byteToHex(this.storedLogInformation.getPlace1())) ||
                !ValidateCard.isSameRoute(station.getStationCode(), Utils.byteToHex(this.gateAccessLogInformation.getCurrentStationCode())) ||
                !ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber()) ||
//                !ValidateCard.isSameDate(this.storedLogInformation) ||
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
//        if(direction!=null){
//            Utils.updateOrInsertTrips(Utils.byteToHex(felicaCard.getIdi()),direction);
//        }
        if (this.type == Type.RIDE) {
            if (!ValidateCard.isEnoughBalanceAvailable(this.storedLogInformation.getCardBalance(), MasterConfigName.MINIMUM_RIDE_BALANCE)) {
                throw new BalanceNotAvailableException("You does not have enough balance for ride. You need at least " + Utils.getMasterConfig(MasterConfigName.MINIMUM_RIDE_BALANCE) + " TK for ride.");
            }
            if (!ValidateCard.checkServiceIdIsEligibleForRide(this.gateAccessLogInformation)) {
                throw new RideNotAllowedException("You does not exit from MRT. Please first exit first from MRT");
            }
            /*if (!ValidateCard.isSameRoute(station.getStationCode()) ||
                    !ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber())) {
                createRide(station);
                return;
            }*/

//            if (!ValidateCard.isSameRoute(station.getStationCode(),Utils.byteToHex(this.storedLogInformation.getPlace1())) ||
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
            /*if(ValidateCard.isStatusRide(this.gateAccessLogInformation.getStatusFlag())){
                if(ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber())){
                    if(ValidateCard.isSameDate(this.storedLogInformation)){
                        if(ValidateCard.isSameRoute(station.getStationCode())){
                            if(ValidateCard.isSameStation(this.storedLogInformation.getPlace1(), station.getStationCode())){
                                if(ValidateCard.isCircularRoute(station.getStationCode())){
                                    this.alight = new Alight(felicaCardDetail);
                                    this.alight.setStation(station);
                                } else{
                                    throw new AlightNotAllowedException("Alight not allowed at this moment. If you want alight you need to cancel of entry. Please contact with conductor");
                                }
                            } else{
                                this.alight = new Alight(felicaCardDetail);
                                this.alight.setStation(station);
                            }
                        } else{
                            this.ride = new Ride(felicaCardDetail);
                            this.ride.setStation(station);
                        }
                    } else {
                        this.ride = new Ride(felicaCardDetail);
                        this.ride.setStation(station);
                    }
                }
                else{
                    this.ride = new Ride(felicaCardDetail);
                    this.ride.setStation(station);
                }
            }else {
                throw new AlightNotAllowedException("Alight not allowed at this moment. Please try again later");
            }*/
            if (!ValidateCard.isStatusRide(this.gateAccessLogInformation.getStatusFlag())) {
                throw new AlightNotAllowedException("Alight not allowed at this moment. Please try again later");
            }

            /*if (!ValidateCard.isSameBus(this.gateAccessLogInformation.getCurrentEquipmentLocationNumber()) ||
                    !ValidateCard.isSameDate(this.storedLogInformation)) {
                createRide(station);
                return;
            }*/

//            if (ValidateCard.isSameRoute(station.getStationCode(),Utils.byteToHex(this.storedLogInformation.getPlace1())) &&
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

    private Integer getMaxFare() {
        assert fareMatrix != null;
        assert fareMatrix.getFareMatrix() != null;
        String stationCode;
        if (type == Type.RIDE && ride != null) {
            stationCode = ride.getStation().getStationCode();
        } else {
//            stationCode = Utils.byteToHex(storedLogInformation.getPlace1()).toUpperCase();
            stationCode = Utils.byteToHex(gateAccessLogInformation.getCurrentStationCode()).toUpperCase();
        }

        return direction == Direction.UPSTREAM ? Objects.requireNonNull(fareMatrix.getFareMatrix().get(stationCode)).get("maxFareUpStream") :
                Objects.requireNonNull(fareMatrix.getFareMatrix().get(stationCode)).get("maxFareDownStream");

    }

    public Integer setFare() {
        assert fareMatrix != null;
        assert fareMatrix.getFareMatrix() != null;
        assert alight != null;
        assert alight.getStation() != null;
        if (fareMatrix.isCircular() && alight.getStation().getStationCode().equalsIgnoreCase(place1) && type.equals(Type.ALIGHT)) {
            return Objects.requireNonNull(fareMatrix.getFareMatrix()
                    .get(alight.getStation().getStationCode())).get("maxFareUpStream");
        }
//        return Objects.requireNonNull(fareMatrix.getFareMatrix().get(Utils.byteToHex(storedLogInformation.getPlace1()).toUpperCase())).get(alight.getStation().getStationCode());
        return Objects.requireNonNull(fareMatrix.getFareMatrix().get(Utils.byteToHex(gateAccessLogInformation.getCurrentStationCode())
                .toUpperCase())).get(alight.getStation().getStationCode());

    }

    public void writeData(DataInterface dataInterface) throws Exception {
        // write data
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

        // block number

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
        /*blockNumberList[0] = (byte) 0x80; // Attribute information file
        blockNumberList[1] = (byte) 0x00;
        blockNumberList[2] = (byte) 0x80;
        blockNumberList[3] = (byte) 0x01;
        blockNumberList[4] = (byte) 0x81; // e-Purse  file
        blockNumberList[5] = (byte) 0x00;
        blockNumberList[6] = (byte) 0x82; // Stored value log information file
        blockNumberList[7] = (byte) 0x00;
        blockNumberList[8] = (byte) 0x83; // Gate access log file
        blockNumberList[9] = (byte) 0x00;
        blockNumberList[10] = (byte) 0x84; // Gate access log file (for transfer)
        blockNumberList[11] = (byte) 0x00;*/
//        blockNumberList[12] = (byte) 0x84;
//        blockNumberList[13] = (byte) 0x01;
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
//            Utils.delete(Utils.byteToHex(felicaCard.getIdi()));
        } else {
            throw new TypeNotSetException("No type set");
        }
//        long eTime = System.currentTimeMillis();
        System.out.println("#RD>>> Card Write Time: "+(System.currentTimeMillis()-sTime));
    }

    private void sendTransactionData(@NonNull DataInterface dataInterface, boolean isProcessed) {
/*
        deviceSN                -

        cardId                  -  8
        recycleCounter          -  1
        transactionDataId       -  2
        serviceId               -  2
        Time Stamp(Date) BCD    -  4
        Time Stamp(Time) BCD    -  3
        processUnfinishedFlag   -  1
        svLogId                 -  2
        svBalance               -  3
        svSpent                 -  3
        processedLocation1      -  2
        processedLocation2      -  2
        negativeValue           -  2
        negativeValueUsed       -  2

        0A013011227414D1 00 0006 D220 20240923 122751 00 0006 000131 FFFFF1 830C 0000 0000 0000
        0A0130051ECB020E 00 000B D630 20250811 130239 00 000B 0003F7 000000 8C10 8C11 0000 0000

        0A0130021ECB0210 00 0010 D220 20250811 144041 00 0010 0003E8 FFFFFF D88C 1100 0000 0000 00

        0A0130021ECB0210 00 0015 D220 20250811 155228 00 0015 000384 FFFFD8 8C19 0000 0000 0000

        0a0130041ecb020f 00 0018 d220 20250820 123027 00 0018 0004DD FFFFD8 8c1c 0000 0000 0000
        0a0130041ecb020f 00 0019 d630 20250820 123324 00 0019 0004F1 000014 8c1c 8c19 0000 0000

        0a0130031ecb09bb 00 0041 B001 20250820 133721 00 0000 000000 000000 0000 0000 0000 0000

        */
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss",Locale.ENGLISH);
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
//        Log.d("META DATA: ", metaData);
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

    private void createAlight(Route.Station station) {
//        this.type = Type.ALIGHT;
//        this.place1 = Utils.byteToHex(storedLogInformation.getPlace1());
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


    public enum Direction {
        UPSTREAM, DOWNSTREAM
    }

    public enum Type {
        RIDE, ALIGHT, CANCEL_OF_ENTRY
    }


    @Setter
    @Getter
    private class Ride {
        private final int initialBalance;
        private final int initialNegativeBalance;
        private AttributeInfo attributeInfo;
        private EPurseInfo ePurseInfo;
        private StoredLogInformation storedLogInformation;
        private GateAccessLogInformation gateAccessLogInformation;
        private GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;
        private String serviceId;
        private boolean rideStatus;
        @Setter(AccessLevel.NONE)
        @Getter(AccessLevel.NONE)
        private boolean isNegative;
        @Getter(AccessLevel.PRIVATE)
        private Route.Station station;


        public Ride(@NonNull FelicaCardDetail felicaCardDetail) {
            this.attributeInfo = felicaCardDetail.getAttributeInfo();
            this.ePurseInfo = felicaCardDetail.getEPurseInfo();
            this.storedLogInformation = felicaCardDetail.getStoredLogInformation();
            this.gateAccessLogInformation = felicaCardDetail.getGateAccessLogInformation();
            this.gateAccessLogInformationForTransfer = felicaCardDetail.getGateAccessLogInformationForTransfer();
            this.initialBalance = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            this.initialNegativeBalance = Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2); // big indian to int

        }

        private void updateAttributeInfo() {
            if (attributeInfo != null) {
                int transId = Utils.byteToInteger(attributeInfo.getTxnDataId()) + 1;
                attributeInfo.setTxnDataId(Utils.hexToByte(String.format("%04X", transId)));
                if (isNegative) {
                    int maxFare = RideAndAlight.this.getMaxFare();
                    // current negative balance + new negative balance
//                    int negativeValue = Math.abs(maxFare - Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(),4))+initialNegativeBalance;
                    int negativeValue = Math.abs(maxFare - Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4));
                    attributeInfo.setNegativeValue(Utils.intToCharArrayLE(negativeValue, 2)); // little indian format
                } else {
                    attributeInfo.setNegativeValue(new byte[2]);
                }
            }
        }

        public void setStation(Route.Station station) {
            this.station = station;
            Utils.updateFromStation(Utils.byteToHex(felicaCard.getIdi()), station.getStationCode());
            this.isNegative = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4) - (RideAndAlight.this.getMaxFare() +
                    initialNegativeBalance) < 0;
        }

        private void updateEPurseInfo() {
            assert ePurseInfo != null;
            int executionId = Utils.byteToInteger(ePurseInfo.getBinExecutionId()) + 1;
            ePurseInfo.setBinExecutionId(Utils.hexToByte(String.format("%04X", executionId)));

            int maxFare = RideAndAlight.this.getMaxFare();
            int sv = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            if (isNegative) {
                ePurseInfo.setBinCashbackData(Utils.intToCharArrayLE(sv)); // Little indian format
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(0)); // Little indian format
                Utils.updateCashBackAmount(Utils.byteToHex(felicaCard.getIdi()), sv);
            } else {
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(Math.max(sv - maxFare, 0))); // Little indian format
                ePurseInfo.setBinCashbackData(Utils.intToCharArrayLE(maxFare)); // Little indian format
                Utils.updateCashBackAmount(Utils.byteToHex(felicaCard.getIdi()), maxFare);
            }
        }

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
                // Little indian format
                storedLogInformation.setCardBalance(Utils.convertToTwosComplementLE(-Integer.parseInt(Utils.byteToHex(Utils.reverseArray(attributeInfo.getNegativeValue())), 16), 3));
            } else {
                storedLogInformation.setServiceClassificationCode((byte) 0xD2);
                storedLogInformation.setCardBalance(Arrays.copyOfRange(ePurseInfo.getBinRemainingSV(), 0, 3)); // Little indian format
            }
        }

        private void updateAccessLogInformation() {
            assert gateAccessLogInformation != null;
            String statusFlagInBit = "110010" + (attributeInfo.getDiscountCode() == 0x00 ? "0" : "1") + "000000000";
            gateAccessLogInformation.setStatusFlag(Utils.hexToByte(String.format("%04X", Integer.parseInt(statusFlagInBit, 2))));
            Map<String, String> date = Utils.getYearMonthDateHourMinute();
            String day = date.get("year") + date.get("month") + date.get("day");
            gateAccessLogInformation.setDate(Utils.hexToByte(String.format("%04X", Integer.parseInt(day, 2))));
            String time = "000" + date.get("hour") + "00" + date.get("minute");
            gateAccessLogInformation.setTime(Utils.hexToByte(String.format("%04X", Integer.parseInt(time, 2))));
//            gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte(String.format("%02x", Integer.parseInt(station.getCode().substring(0, 3))) + String.format("%02x", Integer.parseInt(station.getCode().substring(3)))));
            gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte(station.getStationCode()));
//            gateAccessLogInformation.setCurrentEquipmentLocationNumber(Utils.hexToByte("0101"));
            gateAccessLogInformation.setCurrentEquipmentLocationNumber(Utils.hexToByte(Utils.getInstance().getDeviceInfo().getEquipmentLocationNumber()));
            gateAccessLogInformation.setAmountOfBasicFare(Utils.hexToByte("000000")); // Little indian format

            int maxFare = RideAndAlight.this.getMaxFare();
            // Little indian format
            gateAccessLogInformation.setAmountOfDistanceFare(Arrays.copyOfRange(Utils.intToCharArrayLE(maxFare), 0, 3));
        }

        private void updateTransferAccessLogInformation() {
            assert gateAccessLogInformationForTransfer != null;
            int maxFare = RideAndAlight.this.getMaxFare();
            GateAccessLogInformationForTransfer.Block0 block0 = new GateAccessLogInformationForTransfer.Block0();
            block0.setReserved(new byte[]{0x00, 0x00});
//            block0.setOriginStation(Utils.hexToByte(String.format("%02x", Integer.parseInt(station.getCode().substring(0, 3))) + String.format("%02x", Integer.parseInt(station.getCode().substring(3)))));
            block0.setOriginStation(Utils.hexToByte(station.getStationCode()));
            block0.setTransferStation1(new byte[]{0x00, 0x00});
            block0.setTransferStation2(new byte[]{0x00, 0x00});
            block0.setTransferStation3(new byte[]{0x00, 0x00});
            block0.setFareAllocationAmountForOwnLine(Arrays.copyOfRange(Utils.intToCharArrayLE(maxFare), 0, 3)); // Little indian format
            block0.setFareAllocationAmountForOtherLine(new byte[]{0x00, 0x00, 0x00});

            GateAccessLogInformationForTransfer.Block1 block1 = new GateAccessLogInformationForTransfer.Block1();
            block1.setReserved(Utils.hexToByte(String.format("%022X", 0)));
            block1.setAmountOfTemporaryFare(Utils.hexToByte(String.format("%06X", 0)));
            block1.setStationForTemporaryFareCalculation(Utils.hexToByte(String.format("%04X", 0)));
            gateAccessLogInformationForTransfer.setBlock0(block0);
            gateAccessLogInformationForTransfer.setBlock1(block1);
        }

        private void updateData() {
            // update data
            updateAttributeInfo();
            updateEPurseInfo();
            updateStoredValueLog();
            updateAccessLogInformation();
            updateTransferAccessLogInformation();
        }

        public byte[] getByteData() {
            updateData();

            byte[] attributeInfoData = attributeInfo.getData();
            byte[] ePurseData = ePurseInfo.getData();
            byte[] storageInfoData = storedLogInformation.getData();
            byte[] gateAccessLogData = gateAccessLogInformation.getData();
            byte[] gateAccessLogTransferData = gateAccessLogInformationForTransfer.getBlock0Data();

            // make data

            ByteBuffer byteBuffer = ByteBuffer.allocate(attributeInfoData.length + ePurseData.length + storageInfoData.length + gateAccessLogData.length + gateAccessLogTransferData.length);
            byteBuffer.put(attributeInfoData);
            byteBuffer.put(ePurseData);
            byteBuffer.put(storageInfoData);
            byteBuffer.put(gateAccessLogData);
            byteBuffer.put(gateAccessLogTransferData);
            return byteBuffer.array();
        }
    }

    @Setter
    @Getter
    private class Alight {
        private final AttributeInfo attributeInfo;
        private final EPurseInfo ePurseInfo;
        private final StoredLogInformation storedLogInformation;
        private final GateAccessLogInformation gateAccessLogInformation;
        private final GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;
        private final int initialBalance;
        private final int initialNegativeBalance;
        private String serviceId;
        private int cashbackAmount;
        private boolean alightStatus;
        @Setter(AccessLevel.NONE)
        @Getter(AccessLevel.NONE)
        private boolean isNegative;
        @Getter(AccessLevel.PRIVATE)
        private Route.Station station;


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
//                TripsEntity tripsEntity = Utils.getTripsByCardId(Utils.byteToHex(felicaCard.getIdi()));
                this.cashbackAmount = felicaCard.getCashbackAmount();
                /*if(tripsEntity != null){
                    this.cashbackAmount = tripsEntity.cashBackAmount;
                }*/
            } else {
                this.cashbackAmount = Utils.charArrayToIntLE(ePurseInfo.getBinCashbackData(), 4);
            }
        }

        public void setStation(Route.Station station) {
            this.station = station;
            Utils.updateToStation(Utils.byteToHex(felicaCard.getIdi()), station.getStationCode());
//            int cashBackAmount = Utils.charArrayToIntLE(ePurseInfo.getBinCashbackData(), 4);
//            int cashBackAmount = Utils.getTripsByCardId(Utils.byteToHex(felicaCard.getIdi())).cashBackAmount;
            int sv = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
//            int fare = RideAndAlight.this.getFare();
            RideAndAlight.this.fare = setFare();
            this.isNegative = sv + cashbackAmount - RideAndAlight.this.fare < 0;
        }

        private void updateAttributeInfo() {
            if (attributeInfo != null) {
                int transId = Utils.byteToInteger(attributeInfo.getTxnDataId()) + 1;
                attributeInfo.setTxnDataId(Utils.hexToByte(String.format("%04X", transId)));
                if (isNegative) {
                    int fare = RideAndAlight.this.getFare();
//                    int negativeValue = Math.abs(Utils.byteToInteger(attributeInfo.getNegativeValue())-Math.abs(fare - RideAndAlight.this.getMaxFare()));
                    int negativeValue = Math.abs(fare - cashbackAmount);
                    attributeInfo.setNegativeValue(Utils.intToCharArrayLE(negativeValue, 2)); // Little indian format
                } else {
                    attributeInfo.setNegativeValue(new byte[2]);
                }
            }
        }

        private void updateEPurseInfo() {
            assert ePurseInfo != null;
            int executionId = Utils.byteToInteger(ePurseInfo.getBinExecutionId()) + 1;
            ePurseInfo.setBinExecutionId(Utils.hexToByte(String.format("%04X", executionId)));

//            int cashBackAmount = Utils.charArrayToIntLE(ePurseInfo.getBinCashbackData(), 4);
//            int cashBackAmount = Utils.getTripsByCardId(Utils.byteToHex(felicaCard.getIdi())).cashBackAmount;
            int sv = Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4);
            int fare = RideAndAlight.this.getFare();
            if (isNegative) {
//            ePurseInfo.setBinCashbackData(Utils.hexToByte(String.format("%08X",sv)));
                ePurseInfo.setBinCashbackData(Utils.intToCharArrayLE(0));
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(0));
            } else {
//            ePurseInfo.setBinRemainingSV(Utils.hexToByte(String.format("%04X",sv-maxFare)));
                ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(sv + cashbackAmount - fare));
//            ePurseInfo.setBinCashbackData(Utils.hexToByte(String.format("%04X",maxFare)));
                ePurseInfo.setBinCashbackData(new byte[4]);
            }
        }

        private void updateStoredValueLog() {
            assert storedLogInformation != null;
            String serviceId = String.format("%02X%02X",storedLogInformation.getServiceClassificationCode(), storedLogInformation.getContextCode());
            if(!Arrays.asList(RIDE_AND_DEDUCTION_FROM_SV_NOT_NEGATIVE,RIDE_AND_DEDUCTION_FROM_SV_NEGATIVE).contains(serviceId.toUpperCase())){
                /*TripsEntity tripsEntity = Utils.getTripsByCardId(Utils.byteToHex(felicaCard.getIdi()));
                if(tripsEntity!=null && tripsEntity.fromStation!=null){
                    storedLogInformation.setPlace1(Utils.hexToByte(tripsEntity.fromStation));
                }*/
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
//            st
            storedLogInformation.setStoredValueLogId(ePurseInfo.getBinExecutionId());

            if (isNegative) {
                storedLogInformation.setServiceClassificationCode((byte) 0xD7);
                storedLogInformation.setCardBalance(Utils.convertToTwosComplementLE(-Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2), 3));
            } else {
                storedLogInformation.setServiceClassificationCode((byte) 0xD6);
                storedLogInformation.setCardBalance(Arrays.copyOfRange(ePurseInfo.getBinRemainingSV(), 0, 3));
            }
        }

        private void updateAccessLogInformation() {
            assert gateAccessLogInformation != null;
            String statusFlagInBit = "010010" + (attributeInfo.getDiscountCode() == 0x00 ? "0" : "1") + "000000000";
            gateAccessLogInformation.setStatusFlag(Utils.hexToByte(String.format("%04X", Integer.parseInt(statusFlagInBit, 2))));
            Map<String, String> date = Utils.getYearMonthDateHourMinute();
            String day = date.get("year") + date.get("month") + date.get("day");
            gateAccessLogInformation.setDate(Utils.hexToByte(String.format("%04X", Integer.parseInt(day, 2))));
            String time = "000" + date.get("hour") + "00" + date.get("minute");
            gateAccessLogInformation.setTime(Utils.hexToByte(String.format("%04X", Integer.parseInt(time, 2))));
//            gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte(String.format("%02x", Integer.parseInt(station.getCode().substring(0, 3))) + String.format("%02x", Integer.parseInt(station.getCode().substring(3)))));
            gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte(station.getStationCode()));
            gateAccessLogInformation.setCurrentEquipmentLocationNumber(Utils.hexToByte(Utils.getInstance().getDeviceInfo().getEquipmentLocationNumber()));
            gateAccessLogInformation.setAmountOfBasicFare(Utils.hexToByte("000000"));

            int maxFare = Utils.charArrayToIntLE(gateAccessLogInformation.getAmountOfDistanceFare(), 3);
//            gateAccessLogInformation.setAmountOfDistanceFare(Utils.hexToByte(String.format("%06X", RideAndAlight.this.getFare() - maxFare)));
            gateAccessLogInformation.setAmountOfDistanceFare(RideAndAlight.this.getFare() - maxFare < 0 ? Utils.convertToTwosComplementLE(RideAndAlight.this.getFare() - maxFare, 3) : Arrays.copyOfRange(Utils.intToCharArrayLE(RideAndAlight.this.getFare() - maxFare), 0, 3));
        }

        private void updateTransferAccessLogInformation() {
            assert gateAccessLogInformationForTransfer != null;
            int maxFare = RideAndAlight.this.getMaxFare();
            GateAccessLogInformationForTransfer.Block0 block0 = new GateAccessLogInformationForTransfer.Block0();
            block0.setReserved(new byte[]{0x00, 0x00});
//            block0.setOriginStation(Utils.hexToByte(String.format("%02x", Integer.parseInt(station.getCode().substring(0, 3))) + String.format("%02x", Integer.parseInt(station.getCode().substring(3)))));
            block0.setOriginStation(Utils.hexToByte(station.getStationCode()));
            block0.setTransferStation1(new byte[]{0x00, 0x00});
            block0.setTransferStation2(new byte[]{0x00, 0x00});
            block0.setTransferStation3(new byte[]{0x00, 0x00});
//            block0.setFareAllocationAmountForOwnLine(Utils.hexToByte(String.format("%06X", station.getFare()[RideAndAlight.this.getRide().getStation().getPosition()] - maxFare)));
            block0.setFareAllocationAmountForOwnLine(RideAndAlight.this.getFare() - maxFare < 0 ? Utils.convertToTwosComplementLE(RideAndAlight.this.getFare() - maxFare, 3) : Arrays.copyOfRange(Utils.intToCharArrayLE(RideAndAlight.this.getFare() - maxFare), 0, 3));
            block0.setFareAllocationAmountForOtherLine(new byte[]{0x00, 0x00, 0x00});

            GateAccessLogInformationForTransfer.Block1 block1 = new GateAccessLogInformationForTransfer.Block1();
            block1.setReserved(Utils.hexToByte(String.format("%022X", 0)));
            block1.setAmountOfTemporaryFare(Utils.hexToByte(String.format("%06X", 0)));
            block1.setStationForTemporaryFareCalculation(Utils.hexToByte(String.format("%04X", 0)));
            gateAccessLogInformationForTransfer.setBlock0(block0);
            gateAccessLogInformationForTransfer.setBlock1(block1);
        }

        private void updateData() {
            // update data
            updateAttributeInfo();
            updateEPurseInfo();
            updateStoredValueLog();
            updateAccessLogInformation();
            updateTransferAccessLogInformation();
        }

        public byte[] getByteData() {
            updateData();

            byte[] attributeInfoData = attributeInfo.getData();
            byte[] ePurseData = ePurseInfo.getData();
            byte[] storageInfoData = storedLogInformation.getData();
            byte[] gateAccessLogData = gateAccessLogInformation.getData();
            byte[] gateAccessLogTransferData = gateAccessLogInformationForTransfer.getData();

            // make data

            ByteBuffer byteBuffer = ByteBuffer.allocate(attributeInfoData.length + ePurseData.length + storageInfoData.length + gateAccessLogData.length + gateAccessLogTransferData.length);
            byteBuffer.put(attributeInfoData);
            byteBuffer.put(ePurseData);
            byteBuffer.put(storageInfoData);
            byteBuffer.put(gateAccessLogData);
            byteBuffer.put(gateAccessLogTransferData);
            return byteBuffer.array();
        }

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
