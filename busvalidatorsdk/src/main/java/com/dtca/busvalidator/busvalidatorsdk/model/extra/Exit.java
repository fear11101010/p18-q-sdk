package com.dtca.busvalidator.busvalidatorsdk.model.extra;

import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.model.AttributeInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.EPurseInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.FelicaCardDetail;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformationForTransfer;
import com.dtca.busvalidator.busvalidatorsdk.model.StoredLogInformation;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import lombok.NonNull;

public class Exit {

    private FelicaCard felicaCard;
    private AttributeInfo attributeInfo;
    private EPurseInfo ePurseInfo;
    private StoredLogInformation storedLogInformation;
    private GateAccessLogInformation gateAccessLogInformation;
    private GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;

    public Exit(@NonNull FelicaCard felicaCard) {
        FelicaCardDetail felicaCardDetail = felicaCard.getFelicaCardDetail();
        this.felicaCard = felicaCard;
        this.attributeInfo = felicaCardDetail.getAttributeInfo();
        this.ePurseInfo = felicaCardDetail.getEPurseInfo();
        this.storedLogInformation = felicaCardDetail.getStoredLogInformation();
        this.gateAccessLogInformation = felicaCardDetail.getGateAccessLogInformation();
        this.gateAccessLogInformationForTransfer = felicaCardDetail.getGateAccessLogInformationForTransfer();
    }

    private void updateAttributeInfo() {
        int transactionId = Utils.byteToInteger(this.attributeInfo.getTxnDataId()) + 1;
        this.attributeInfo.setTxnDataId(Utils.hexToByte(String.format("%04X", transactionId)));
    }

    private void updateEPurseInfo() {
        this.ePurseInfo.setBinCashbackData(new byte[4]); // 4 byte
        int executionId = Utils.byteToInteger(this.ePurseInfo.getBinExecutionId()) + 1;
        this.ePurseInfo.setBinExecutionId(Utils.hexToByte(String.format("%04X", executionId)));
        int currentBalance = Utils.charArrayToIntLE(this.ePurseInfo.getBinRemainingSV(),4)-20;
        this.ePurseInfo.setBinRemainingSV(Utils.intToCharArrayLE(currentBalance));
    }

    private void updateStoredValueLog() {
        assert storedLogInformation != null;
        // station1 = 266(dec)--010A(hex)
        // station2 = 276(dec)--0114(hex)
        // fare = 20

        storedLogInformation.setEquipmentClassificationCode((byte) 0x08);
        storedLogInformation.setServiceClassificationCode((byte) 0x52);
        storedLogInformation.setContextCode((byte) 0x10);
        storedLogInformation.setPaymentMethodCode((byte) 0x00);
        storedLogInformation.setCardBalance(Arrays.copyOfRange(ePurseInfo.getBinRemainingSV(),0,3));
        Map<String, String> date = Utils.getYearMonthDateHourMinute();
        String day = date.get("year") + date.get("month") + date.get("day");
        day = String.format("%04X", Integer.parseInt(day, 2));
        storedLogInformation.setDate(Utils.hexToByte(day));
        String time = date.get("hour") + date.get("minute") + "00000";
        storedLogInformation.setTime(Utils.hexToByte(String.format("%02X", Integer.parseInt(date.get("hour") + "000", 2)))[0]);
        storedLogInformation.setPlace1(Utils.hexToByte("010A"));
        storedLogInformation.setPlace2(Utils.hexToByte("0114"));
        storedLogInformation.setStoredValueLogId(ePurseInfo.getBinExecutionId());

    }

    private void updateGateAccessLogInformation() {

        // Construct the Status Flag (2 bytes)
        // bit15 : Enter/Exit = 0 (Exit)
        // bit14 : Stored Value used = 1 (YES)
        // bit13 : Season Pass/Coupon Ticket/Staff Pass used = 0 (NO)
        // bit12 : Transfer Discount = 0 (NO)
        // bit11 : Kinds of transport mode = 0 (Railway)
        // bit10 : Transfer from a Bus/Tram/BRT to a train = 0 (NO)
        // bit9  : Apply discount fare = 0
        // bit8~bit0 : Reserved = 0

        // Binary Representation: 1100 0000 0000 0000 = 0xC0 0x00
        byte[] statusFlagBytes = Utils.hexToByte("4000");
        gateAccessLogInformation.setStatusFlag(statusFlagBytes);

        Map<String, String> date = Utils.getYearMonthDateHourMinute();
        String currentDate = date.get("year") + date.get("month") + date.get("day");
        gateAccessLogInformation.setDate(Utils.hexToByte(String.format("%04X", Integer.parseInt(currentDate, 2))));
        String currentTime = date.get("hour") + date.get("minute");
        gateAccessLogInformation.setTime(Utils.hexToByte(String.format("%04X", Integer.parseInt(currentTime, 2))));

        gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte("0114"));
        gateAccessLogInformation.setCurrentEquipmentLocationNumber(Utils.hexToByte("2050"));

        gateAccessLogInformation.setAmountOfBasicFare(Arrays.copyOfRange(Utils.intToCharArrayLE(10), 0, 3));
        gateAccessLogInformation.setAmountOfDistanceFare(Arrays.copyOfRange(Utils.intToCharArrayLE(10), 0, 3));
    }

    private void updateGateAccessLogInformationForTransfer() {
        GateAccessLogInformationForTransfer.Block0 block0 = new GateAccessLogInformationForTransfer.Block0();
        block0.setOriginStation(Utils.hexToByte("010A")); // Madhubag
        block0.setTransferStation1(new byte[2]);
        block0.setTransferStation2(new byte[2]);
        block0.setTransferStation3(new byte[2]);
        block0.setFareAllocationAmountForOwnLine(Arrays.copyOfRange(Utils.intToCharArrayLE(20), 0, 3)); // decimal = 20
        block0.setFareAllocationAmountForOtherLine(new byte[3]); // decimal = 0
        block0.setReserved(new byte[2]);
        gateAccessLogInformationForTransfer.setBlock0(block0);

        GateAccessLogInformationForTransfer.Block1 block1 = new GateAccessLogInformationForTransfer.Block1();
        block1.setAmountOfTemporaryFare(new byte[3]);
        block1.setStationForTemporaryFareCalculation(new byte[2]);
        block1.setReserved(new byte[11]);
        gateAccessLogInformationForTransfer.setBlock1(block1);
    }

    public void executeExit() throws Exception {

        updateAttributeInfo();
        updateEPurseInfo();
        updateStoredValueLog();
        updateGateAccessLogInformation();
        updateGateAccessLogInformationForTransfer();

        int serviceCode = 5;
        byte[] serviceCodes = new byte[serviceCode*4];

        //	1308 - Card Attribute Information (Little endian)
        serviceCodes[0] = 0x08; // serviced code 08
        serviceCodes[1] = 0x13; // serviced code 13
        serviceCodes[2] = 0x01; // version key
        serviceCodes[3] = 0x00; // version key

        //	1410 - e-Purse Information  (Little endian)
        serviceCodes[4] = 0x10;// serviced code 10
        serviceCodes[5] = 0x14;// serviced code 14
        serviceCodes[6] = 0x01;// version key
        serviceCodes[7] = 0x00;// version key

        //  220C - sv log file
        serviceCodes[8] = 0x0C;// serviced code 10
        serviceCodes[9] = 0x22;// serviced code 14
        serviceCodes[10] = 0x01;// version key
        serviceCodes[11] = 0x00;// version key

        //	250C - Gate Access Log Information File
        serviceCodes[12] = 0x0C;// serviced code 0C
        serviceCodes[13] = 0x25;// serviced code 25
        serviceCodes[14] = 0x01;// version key
        serviceCodes[15] = 0x00;// version key


        //	2608 - Gate Access Log Information (For Transfer)
        serviceCodes[16] = 0x08;// serviced code 08
        serviceCodes[17] = 0x26;// serviced code 26
        serviceCodes[18] = 0x01;// version key
        serviceCodes[19] = 0x00;// version key

        int blockNumber = 6;
        byte[] blockNumbers = new byte[blockNumber*2];

        //	Card Attribute Information
        blockNumbers[0] = (byte) 0x81;
        blockNumbers[1] = (byte) 0x00;
        blockNumbers[2] = (byte) 0x81;
        blockNumbers[3] = (byte) 0x01;

        //	e-Purse Information
        blockNumbers[4] = (byte) 0x82;
        blockNumbers[5] = (byte) 0x00;

        //	stored log Information
        blockNumbers[6] = (byte) 0x84;
        blockNumbers[7] = (byte) 0x00;

        //	Gate Access Log Information File
        blockNumbers[8] = (byte) 0x85;
        blockNumbers[9] = (byte) 0x00;

        //	Gate Access Log Information (For Transfer)
        blockNumbers[10] = (byte) 0x86;
        blockNumbers[11] = (byte) 0x00;
//        blockNumbers[12] = (byte) 0x84;
//        blockNumbers[13] = (byte) 0x01;

        byte[] attr = attributeInfo.getData();
        byte[] ePurse = ePurseInfo.getData();
        byte[] svLog = storedLogInformation.getData();
        byte[] accessLog = gateAccessLogInformation.getData();
        byte[] accessLogForTransfer = gateAccessLogInformationForTransfer.getBlock0Data();

        ByteBuffer data = ByteBuffer.allocate(attr.length + ePurse.length + svLog.length + accessLog.length + accessLogForTransfer.length);
        data.put(attr);
        data.put(ePurse);
        data.put(svLog);
        data.put(accessLog);
        data.put(accessLogForTransfer);

        this.felicaCard.writeInCard(serviceCode, serviceCodes, blockNumber, blockNumbers, data.array());
    }

}

