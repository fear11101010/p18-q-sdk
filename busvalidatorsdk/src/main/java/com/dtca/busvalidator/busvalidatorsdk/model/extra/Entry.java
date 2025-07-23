package com.dtca.busvalidator.busvalidatorsdk.model.extra;

import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.model.AttributeInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.EPurseInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.FelicaCardDetail;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformationForTransfer;

import java.nio.ByteBuffer;
import java.util.Map;

import lombok.NonNull;

public class Entry {

    private FelicaCard felicaCard;
    private AttributeInfo attributeInfo;
    private EPurseInfo ePurseInfo;
    private GateAccessLogInformation gateAccessLogInformation;
    private GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer;

    public Entry(@NonNull FelicaCard felicaCard) {
        FelicaCardDetail felicaCardDetail = felicaCard.getFelicaCardDetail();
        this.felicaCard = felicaCard;
        this.attributeInfo = felicaCardDetail.getAttributeInfo();
        this.ePurseInfo = felicaCardDetail.getEPurseInfo();
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
    }

    private void updateGateAccessLogInformation() {

        // Construct the Status Flag (2 bytes)
        // bit15 : Enter/Exit = 1 (Enter)
        // bit14 : Stored Value used = 1 (YES)
        // bit13 : Season Pass/Coupon Ticket/Staff Pass used = 0 (NO)
        // bit12 : Transfer Discount = 0 (NO)
        // bit11 : Kinds of transport mode = 0 (Railway)
        // bit10 : Transfer from a Bus/Tram/BRT to a train = 0 (NO)
        // bit9  : Apply discount fare = 0
        // bit8~bit0 : Reserved = 0

        // Binary Representation: 1100 0000 0000 0000 = 0xC0 0x00
        byte[] statusFlagBytes = Utils.hexToByte("C000");
        gateAccessLogInformation.setStatusFlag(statusFlagBytes);

        Map<String, String> date = Utils.getYearMonthDateHourMinute();
        String currentDate = date.get("year") + date.get("month") + date.get("day");
        gateAccessLogInformation.setDate(Utils.hexToByte(String.format("%04X", Integer.parseInt(currentDate, 2))));
        String currentTime = date.get("hour") + date.get("minute");
        gateAccessLogInformation.setTime(Utils.hexToByte(String.format("%04X", Integer.parseInt(currentTime, 2))));

        gateAccessLogInformation.setCurrentStationCode(Utils.hexToByte("8C1C"));
        gateAccessLogInformation.setCurrentEquipmentLocationNumber(Utils.hexToByte("2040"));

        gateAccessLogInformation.setAmountOfBasicFare(new byte[3]);
        gateAccessLogInformation.setAmountOfDistanceFare(new byte[3]);

    }

    private void updateGateAccessLogInformationForTransfer() {
        GateAccessLogInformationForTransfer.Block0 block0 = new GateAccessLogInformationForTransfer.Block0();
        block0.setOriginStation(Utils.hexToByte("010A")); // Madhubag
        block0.setTransferStation1(new byte[2]);
        block0.setTransferStation2(new byte[2]);
        block0.setTransferStation3(new byte[2]);
        block0.setFareAllocationAmountForOwnLine(Utils.hexToByte("14")); // decimal = 20
        block0.setFareAllocationAmountForOtherLine(Utils.hexToByte("32")); // decimal = 50
        block0.setReserved(new byte[2]);
        gateAccessLogInformationForTransfer.setBlock0(block0);

        GateAccessLogInformationForTransfer.Block1 block1 = new GateAccessLogInformationForTransfer.Block1();
        block1.setAmountOfTemporaryFare(new byte[3]);
        block1.setStationForTemporaryFareCalculation(new byte[2]);
        block1.setReserved(new byte[11]);
        gateAccessLogInformationForTransfer.setBlock1(block1);
    }

    public int executeEntry() throws Exception {

        updateAttributeInfo();
        updateEPurseInfo();
        updateGateAccessLogInformation();
        updateGateAccessLogInformationForTransfer();

        int serviceCode = 4;
        byte[] serviceCodes = new byte[16];

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

        //	250C - Gate Access Log Information File
        serviceCodes[8] = 0x0C;// serviced code 0C
        serviceCodes[9] = 0x25;// serviced code 25
        serviceCodes[10] = 0x01;// version key
        serviceCodes[11] = 0x00;// version key


        //	2608 - Gate Access Log Information (For Transfer)
        serviceCodes[12] = 0x08;// serviced code 08
        serviceCodes[13] = 0x26;// serviced code 26
        serviceCodes[14] = 0x01;// version key
        serviceCodes[15] = 0x00;// version key

        int blockNumber = 6;
        byte[] blockNumbers = new byte[12];

        //	Card Attribute Information
        blockNumbers[0] = (byte) 0x80;
        blockNumbers[1] = (byte) 0x00;
        blockNumbers[2] = (byte) 0x80;
        blockNumbers[3] = (byte) 0x01;

        //	e-Purse Information
        blockNumbers[4] = (byte) 0x81;
        blockNumbers[5] = (byte) 0x00;

        //	Gate Access Log Information File
        blockNumbers[6] = (byte) 0x82;
        blockNumbers[7] = (byte) 0x00;

        //	Gate Access Log Information (For Transfer)
        blockNumbers[8] = (byte) 0x83;
        blockNumbers[9] = (byte) 0x00;
        blockNumbers[10] = (byte) 0x83;
        blockNumbers[11] = (byte) 0x01;

        byte[] attr = attributeInfo.getData();
        byte[] ePurse = ePurseInfo.getData();
        byte[] accessLog = gateAccessLogInformation.getData();
        byte[] accessLogForTransfer = gateAccessLogInformationForTransfer.getData();

        ByteBuffer data = ByteBuffer.allocate(attr.length + ePurse.length + accessLog.length + accessLogForTransfer.length);
        data.put(attr);
        data.put(ePurse);
        data.put(accessLog);
        data.put(accessLogForTransfer);

        return this.felicaCard.writeInCard(serviceCode, serviceCodes, blockNumber, blockNumbers, data.array());
    }

}

