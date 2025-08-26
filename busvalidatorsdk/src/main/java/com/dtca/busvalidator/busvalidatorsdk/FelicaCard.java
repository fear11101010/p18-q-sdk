package com.dtca.busvalidator.busvalidatorsdk;

import android.text.TextUtils;
import android.util.Log;

import com.decard.NDKMethod.BasicOper;
import com.dtca.busvalidator.busvalidatorsdk.helper.SAMCommandCodes;
import com.dtca.busvalidator.busvalidatorsdk.helper.ServiceCode;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.helper.ValidateCard;
import com.dtca.busvalidator.busvalidatorsdk.model.AttributeInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.BlackList;
import com.dtca.busvalidator.busvalidatorsdk.model.EPurseInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.FelicaCardDetail;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.GateAccessLogInformationForTransfer;
import com.dtca.busvalidator.busvalidatorsdk.model.IssuerInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.OperatorInfo;
import com.dtca.busvalidator.busvalidatorsdk.model.ReadWriteInCard;
import com.dtca.busvalidator.busvalidatorsdk.model.StoredLogInformation;
import com.dtca.busvalidator.busvalidatorsdk.model.TransactionHistory;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardBlackListException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardIdSameException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardNotActiveException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardNotFoundException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardReadException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardUnissuedException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.FelicaMutualAuthException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.MRTCardNotAllowedException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.VoidCardException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.WrongServiceIdException;
import com.dtca.busvalidator.busvalidatorsdk.model.interfac.ClearCardId;
import com.dtca.busvalidator.busvalidatorsdk.model.interfac.DataInterface;
import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

public class FelicaCard implements ReadWriteInCard, ClearCardId {
    private static FelicaCard felicaCard;
    @Getter
    private final Sam sam;
    @Getter
    private byte[] iDm;
    @Getter
    private byte[] pMm;
    private byte[] idt;
    @Getter
    private byte[] idi;
    private byte[] initIdi;
    @Getter
    private byte[] systemCode;
    @Getter
    @Setter
    private FelicaCardDetail felicaCardDetail;
    @Getter
    private int currentBalance;

    private FelicaCard(Sam sam) {
        this.sam = sam;
        String[] result = BasicOper.dc_config_card(3).split("\\|", -1);
        if (result[0].equals("0000")) {
            // log.d("FelicaCard", "FelicaCard: Card config successful--" + result[1]);
        }
    }

    public static FelicaCard getInstance(Sam sam) {
        if (felicaCard == null) {
            felicaCard = new FelicaCard(sam);
        }
        return felicaCard;

    }

    public void detectFelicaCard() throws Exception {
        long st1 = System.currentTimeMillis();
        String[] result = BasicOper.dc_FeliCaReset().split("\\|", -1);
        if (result.length >= 3 && result[0].equals("0000") && Utils.hexToByte(result[2]).length >= 18) {
            this.iDm = Arrays.copyOfRange(Utils.hexToByte(result[2]), 0, 8);
            this.pMm = Arrays.copyOfRange(Utils.hexToByte(result[2]), 8, 16);
            this.systemCode = Arrays.copyOfRange(Utils.hexToByte(result[2]), 16, 18);

            System.out.println("#RD>>> Card Detect Time: " + (System.currentTimeMillis() - st1));
        } else {
//            BasicOper.dc_FeliCaReset();
            result = BasicOper.dc_FeliCaApdu("0600FFFF0100").split("\\|", -1);
            if (result[0].equals("0000") && !TextUtils.isEmpty(result[1])) {
                byte[] hexToByte = Utils.hexToByte(result[1]);
                if (hexToByte.length >= 19) {
                    byte[] bytes = Arrays.copyOfRange(hexToByte, 2, hexToByte.length);
                    this.iDm = Arrays.copyOfRange(bytes, 0, 8);
                    this.pMm = Arrays.copyOfRange(bytes, 8, 16);
                    this.systemCode = Arrays.copyOfRange(bytes, 16, 18);
                } else {
                    throw new CardNotFoundException("No Card Detected. Please tap card");
                }
            }
            throw new CardNotFoundException("No Card Detected. Please tap card");
        }

    }

    public int readCard(DataInterface dataInterface) throws Exception {
        long st1 = System.currentTimeMillis();
        byte[] readData = new byte[256];
        int[] readLen = new int[1];

//        byte[] bytes = readWithoutAuth();
        int ret = readFiles(readData, readLen);
        if (ret == 0) {
            throw new CardReadException("Can not read card at this moment. please try again later");
        }
        System.out.println("#RD>>> Card Read Time: " + (System.currentTimeMillis() - st1));
        long st2 = System.currentTimeMillis();
        populateFelicaCard(Arrays.copyOfRange(readData, 3, readData.length), readLen[0] - 3);
        if (ValidateCard.isMRTCard(felicaCardDetail.getIssuerInfo().getCardIssuerID())) {
            throw new MRTCardNotAllowedException("MRT Card is not allowed");
        }
        if (ValidateCard.isFirstIssue(felicaCardDetail.getStoredLogInformation().getStoredValueLogId())) {
            throw new CardUnissuedException("Card is not second issued");
        }
        /*if (ValidateCard.isFirstIssue(felicaCardDetail.getStoredLogInformation().getStoredValueLogId())) {
            throw new CardUnissuedException("Card is not second issued");
        }*/
        if (ValidateCard.isSameCardId(initIdi, idi)) {
            throw new CardIdSameException("Card is not initialized");
        }
        if (!ValidateCard.isCardActive(felicaCardDetail.getOperatorInfo().getStatusFlag())) {
            throw new CardNotActiveException("Card is not active");
        }
        /*if(!ValidateCard.isTestCard(felicaCardDetail.getAttributeInfo().getCardFunctionCode())){
            throw new TestCardException("Test Card Not Allowed");
        }*/

        if (ValidateCard.isCardBlacklisted(felicaCardDetail.getAttributeInfo().getCardControlCode()) ||
                ValidateCard.isCardBlacklistedInDB(sam.getContext(), idi, felicaCardDetail.getIssuerInfo().getRecycleCounter())) {

            BlackList blackList = new BlackList(this);
            blackList.writeData(dataInterface);
            throw new CardBlackListException("Card is blacklisted");
        }
        if (ValidateCard.isVoidCard(felicaCardDetail.getAttributeInfo().getCardControlCode())) {
            throw new VoidCardException("Card is void");
        }

        this.currentBalance = Utils.charArrayToIntLE(this.felicaCardDetail.getEPurseInfo().getBinRemainingSV(), 4);
        System.out.println("#RD>>> Card Validation Time: " + (System.currentTimeMillis() - st2));
        return 1;
    }

    public int readCardForTransactionHistory(DataInterface dataInterface) throws Exception {
        byte[] readData = new byte[256];
        int[] readLen = new int[1];

        int ret = readFilesForTransactionHistory(readData, readLen);
        if (ret == 0) {
            throw new CardReadException("Can not read card at this moment. please try again later");
        }
        populateFelicaCardForTransactionHistory(Arrays.copyOfRange(readData, 3, readData.length), readLen[0] - 3, dataInterface);

        return 1;
    }

    //    private void populateFelicaCard(byte[] data, int len, byte[] openBlockData) {
    private void populateFelicaCard(byte[] data, int len) {
        byte[] bytes = Arrays.copyOfRange(data, 0, len - 2);
        IssuerInfo issuerInfo = IssuerInfo.generateData(Arrays.copyOfRange(bytes, 0, 16));
//        PersonalInfo personalInfo = PersonalInfo.generateData(Arrays.copyOfRange(bytes, 16, 16 * 5));
        AttributeInfo attributeInfo = AttributeInfo.generateData(Arrays.copyOfRange(bytes, 16, 16 * 3));
        EPurseInfo ePurseInfo = EPurseInfo.generateData(Arrays.copyOfRange(bytes, 16 * 3, 16 * 4));
        OperatorInfo operatorInfo = OperatorInfo.generateData(Arrays.copyOfRange(bytes, 16 * 4, 16 * 5));
        StoredLogInformation storedLogInformation = StoredLogInformation.generateData(Arrays.copyOfRange(bytes, 16 * 5, 16 * 6));
        List<StoredLogInformation> storedLogInformationList = getStoredLogInformationList(Arrays.copyOfRange(bytes, 16 * 5, 16 * 8));
        GateAccessLogInformation gateAccessLogInformation = GateAccessLogInformation.generateData(Arrays.copyOfRange(bytes, 16 * 8, 16 * 9));
        GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer = GateAccessLogInformationForTransfer.generateData(Arrays.copyOfRange(bytes, 16 * 9, 16 * 10));

        /*StoredLogInformation storedLogInformation = StoredLogInformation.generateData(Arrays.copyOfRange(openBlockData, 0, 16));
        GateAccessLogInformation gateAccessLogInformation = GateAccessLogInformation.generateData(Arrays.copyOfRange(openBlockData, 16, 16 * 2));
        GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer = GateAccessLogInformationForTransfer.generateData(Arrays.copyOfRange(bytes, 16 * 2, 16 * 3));
       */

        /* GeneralInfo generalInfo = GeneralInfo.builder()
                .binCardID(idi)
                .binReCycleCounter(issuerInfo.getRecycleCounter())
                .lngRemainingSV(Utils.charArrayToIntLE(ePurseInfo.getBinRemainingSV(), 4))
                .lngCashBackData(Utils.charArrayToIntLE(ePurseInfo.getBinCashbackData(), 4))
                .intNegativeValue((short) Utils.charArrayToIntLE(attributeInfo.getNegativeValue(), 2))
                .build();*/
        /*if ((attributeInfo.getCardFunctionCode()[0] & (byte) 0xFE) == (byte) 0xC0) {
            generalInfo.setBinCardType((byte) 0); // RAPIDPASS_SVC_CARD
        } else if (((attributeInfo.getCardFunctionCode()[1] & (byte) 0x30) == (byte) 0x10) && ((attributeInfo.getCardFunctionCode()[0] & (byte) 0xFE) == (byte) 0x02)) {
            generalInfo.setBinCardType((byte) 10); // RAPIDPASS_OPERATOR_CARD
        } else if (((attributeInfo.getCardFunctionCode()[1] & (byte) 0x30) == (byte) 0x20) && ((attributeInfo.getCardFunctionCode()[0] & (byte) 0xFE) == (byte) 0x02)) {
            generalInfo.setBinCardType((byte) 11); // RAPIDPASS_NEXT_CARD
        } else if (((attributeInfo.getCardFunctionCode()[1] & (byte) 0x30) == (byte) 0x30) && ((attributeInfo.getCardFunctionCode()[0] & (byte) 0xFE) == (byte) 0x02)) {
            generalInfo.setBinCardType((byte) 12); // RAPIDPASS_PREV_CARD
        } else {
            generalInfo.setBinCardType((byte) -1);
        }*/

        this.felicaCardDetail = new FelicaCardDetail(issuerInfo, attributeInfo, ePurseInfo,
                operatorInfo, storedLogInformation, storedLogInformationList,gateAccessLogInformation, gateAccessLogInformationForTransfer);

    }

    public List<StoredLogInformation> getStoredLogInformationList(byte[] bytes) {
        List<StoredLogInformation> storedLogInformationList = new ArrayList<>();
        for(int i = 0;i < bytes.length;i += 16){
            storedLogInformationList.add(StoredLogInformation.generateData(Arrays.copyOfRange(bytes,i,i+16)));
        }
        return storedLogInformationList;
    }

    private void populateFelicaCardForTransactionHistory(byte[] data, int len, DataInterface dataInterface) throws Exception {
        byte[] bytes = Arrays.copyOfRange(data, 0, len - 2);
        AttributeInfo attributeInfo = AttributeInfo.generateData(Arrays.copyOfRange(bytes, 0, 16 * 2));
        OperatorInfo operatorInfo = OperatorInfo.generateData(Arrays.copyOfRange(bytes, 16 * 2, 16 * 3));
        StoredLogInformation storedLogInformation = StoredLogInformation.generateData(Arrays.copyOfRange(bytes, 16 * 3, 16 * 4));
        if (ValidateCard.isFirstIssue(storedLogInformation.getStoredValueLogId())) {
            throw new CardUnissuedException("Card is not second issued");
        }
        if (ValidateCard.isSameCardId(initIdi, idi)) {
            throw new CardIdSameException("Card is not initialized");
        }
        if (!ValidateCard.isCardActive(operatorInfo.getStatusFlag())) {
            throw new CardNotActiveException("Card is not active");
        }
        if (ValidateCard.isCardBlacklisted(attributeInfo.getCardControlCode()) ||
                ValidateCard.isCardBlacklistedInDB(sam.getContext(), idi, (byte) 0)) {

//            BlackList blackList = new BlackList(this);
//            blackList.writeData(dataInterface);
            throw new CardBlackListException("Card is blacklisted");
        }
        if (ValidateCard.isVoidCard(attributeInfo.getCardControlCode())) {
            throw new VoidCardException("Card is void");
        }
    }

    public int mutualAuthV2WithFeliCa(byte serviceCodeNum,
                                      byte[] serviceCodeKeyVerList) throws Exception {

        byte[] felicaCmdParams = new byte[256];
        int felicaCmdParamsLen;

        byte[] samResBuf = new byte[262];
        byte[] felicaCmd = new byte[262];
        byte[] felicaRes = new byte[262];
        int[] samResLen = new int[1];
        int[] felicaResLen = new int[1];
        int[] felicaCmdLen = new int[1];

        // Generate params sent to SAM
        System.arraycopy(iDm, 0, felicaCmdParams, 0, 8); //IDm

        felicaCmdParams[8] = 0x00;                // Reserved
        felicaCmdParams[8 + 1] = 0x03;                // Key Type(Node key, Diversification Code specified)
        felicaCmdParams[8 + 1 + 1] = systemCode[0];        // SystemCode(Big endian)
        felicaCmdParams[8 + 1 + 2] = systemCode[1];        // SystemCode(Big endian)
        felicaCmdParams[8 + 1 + 1 + 2] = 0x00; // Operation Parameter(No Diversification, AES128)
        Arrays.fill(felicaCmdParams, 8 + 1 + 1 + 2 + 1, 8 + 1 + 1 + 2 + 1 + 16, (byte) 0x00);// Diversification code(All Zero)
        felicaCmdParams[8 + 1 + 1 + 2 + 1 + 16] = serviceCodeNum; // Number of Service
        System.arraycopy(serviceCodeKeyVerList, 0, felicaCmdParams, 8 + 1 + 1 + 2 + 1 + 16 + 1, serviceCodeNum * 4);// Service code list
        felicaCmdParamsLen = 8 + 1 + 1 + 2 + 1 + 16 + 1 + serviceCodeNum * 4;
        long felicaCmdRes = sam.askFeliCaCmdToSAMSC(SAMCommandCodes.SAM_COMMAND_CODE_MUTUAL_AUTH_V2_RWSAM,
                SAMCommandCodes.SAM_SUB_COMMAND_CODE_MUTUAL_AUTH_V2_RWSAM, felicaCmdParamsLen,
                felicaCmdParams, felicaCmdLen, felicaCmd);
//        // log.d("TAG", "mutualAuthV2WithFeliCa: ");
        if (felicaCmdRes == 0) {
            return 0;
        }
        int felicaTransRes = transmitDataToFeliCaCard(felicaCmdLen[0], felicaCmd,
                felicaResLen, felicaRes);
        if (felicaTransRes == 0) {
            return 0;
        }
        int auth1V2Res = sam.sendAuth1V2ResultToSAM(felicaResLen[0], felicaRes, felicaCmdLen, felicaCmd);
        if (auth1V2Res == 0) {
            return 0;
        }
        felicaTransRes = transmitDataToFeliCaCard(felicaCmdLen[0], felicaCmd,
                felicaResLen, felicaRes);
        if (felicaTransRes == 0) {
            return 0;
        }

        int samRes = sam.sendCardResultToSAM(felicaResLen[0], felicaRes, samResLen,
                samResBuf);
        if (samRes == 0) {
            return 0;
        }
        if (samResBuf[0] != (byte) 0x00) {
            return 0;
        }

        this.idi = new byte[8];
        this.initIdi = new byte[8];
        System.arraycopy(samResBuf, 1, idi, 0, 8);

        this.idt = new byte[2];
        this.idt[0] = samResBuf[1 + 8 + 8];
        this.idt[1] = samResBuf[1 + 8 + 8 + 1];

        return 1;

    }

    public int transmitDataToFeliCaCard(int felicaCmdLen, byte[] felicaCmdBuf, int[] felicaResLen, byte[] felicaResBuf) {
        byte[] sendBuf = new byte[262];
        int sendLen, receiveLen;

        sendLen = felicaCmdLen + 1; // Length of FeliCa Command
        sendBuf[0] = (byte) sendLen;
        System.arraycopy(felicaCmdBuf, 0, sendBuf, 1, felicaCmdLen);

        // log.d("felicaCmd", Utils.byteToHex(Arrays.copyOfRange(sendBuf, 0, sendLen)));
        String[] result = BasicOper.dc_FeliCaApdu(Utils.byteToHex(Arrays.copyOfRange(sendBuf, 0, sendLen))).split("\\|", -1);
//        String[] result = BasicOper.dc_FeliCaApdu("2E060127D50197018372010F22108000800180028003800480058006800780088009800A800B800C800D800E800F").split("\\|", -1);

        if (!result[0].equals("0000")) {
//            tap_error = 1;
            return 0;
        }

        // log.d("felicaCommandFinal", "felicaCommandFinal: " + result[1]);

        byte[] hexToByte = Utils.hexToByte(result[1]);

        if (hexToByte.length < 10) {
            return 0;
        }
        receiveLen = hexToByte.length;
        System.arraycopy(hexToByte, 1, felicaResBuf, 0, receiveLen - 1);
        felicaResLen[0] = receiveLen - 1;

        return 1;
    }

    @Override
    public FelicaCard readData() throws Exception {
        if (readCard(transactionData -> Log.d("transaction_data", new Gson().toJson(transactionData))) == 0) {
            return null;
        }
        return this;
    }

    @Override
    public int writeInCard(int serviceNum, byte[] serviceList, int blockNum, byte[] blockList, byte[] blockData) throws Exception {
//        int ret = mutualAuthV2WithFeliCa((byte) serviceNum, serviceList);
//        if (ret == 0) {
//            return 0;
//        }
//        long st = System.currentTimeMillis();
//        ret = writeBlockData(blockNum, blockNum * 2, blockList, blockData);
        int ret = writeBlockData(blockNum, blockNum * 2, blockList, blockData);
        if (ret == 0) {
            return 0;
        }
//        long et = System.currentTimeMillis();
//        System.out.println("Write Time: "+(double) (et - st));
//        Log.d("writeInCard_time", "------->" + ((double) (et - st)) / 1000);
        return 1;
    }


    public int readDataBlock(byte blockNum, byte[] blockList, int[] readLen, byte[] readData) throws Exception {
        byte[] felicaCmdParams = new byte[256], felicaCmd = new byte[262], felicaRes = new byte[262];
        int felicaCmdParamsLen;
        int[] felicaCmdLen = new int[1], felicaResLen = new int[1];

        // Generate params sent to sam
        felicaCmdParams[0] = idt[0];
        felicaCmdParams[1] = idt[1];
        felicaCmdParams[2] = blockNum;
        System.arraycopy(blockList, 0, felicaCmdParams, 2 + 1, blockNum * 2);
        felicaCmdParamsLen = 2 + 1 + blockNum * 2;
        int ret = (int) sam.askFeliCaCmdToSAM(SAMCommandCodes.SAM_COMMAND_CODE_READ, felicaCmdParamsLen, felicaCmdParams, felicaCmdLen, felicaCmd);
        if (ret == 0) {
            return 0;
        }
        ret = transmitDataToFeliCaCard(felicaCmdLen[0] - 2, felicaCmd, felicaResLen, felicaRes);
        if (ret == 0) {
            return 0;
        }
        ret = sam.sendCardResultToSAM(felicaResLen[0], felicaRes, readLen, readData);
        if (ret == 0) {
            return 0;
        }
        return 1;

    }

    public int mutualAuthWithFelicaCard() throws Exception {
        long st = System.currentTimeMillis();
        byte serviceNum;
        byte[] serviceList = new byte[64];
        serviceNum = 7;
        serviceList[0] = 0x08; //Issuer info file
        serviceList[1] = 0x11; //Issuer info file
        serviceList[2] = 0x01; //Service key ver
        serviceList[3] = 0x00; //Service key ver

        serviceList[4] = 0x08; //Card attribute info file
        serviceList[5] = 0x13; //Card attribute info file
        serviceList[6] = 0x01; //Service key ver
        serviceList[7] = 0x00; //Service key ver
        serviceList[8] = 0x10; //EPurse info file
        serviceList[9] = 0x14; //EPurse info file
        serviceList[10] = 0x01; //Service key ver
        serviceList[11] = 0x00; //Service key ver
        serviceList[12] = 0x08; //Operator info file
        serviceList[13] = 0x21; //Operator info file
        serviceList[14] = 0x01; //Service key ver
        serviceList[15] = 0x00; //Service key ver
        serviceList[16] = 0x0C; //Stored value log file
        serviceList[17] = 0x22; //Stored value log file
        serviceList[18] = 0x01; //Service key ver
        serviceList[19] = 0x00; //Service key ver
        serviceList[20] = 0x0C; //Gate access log file
        serviceList[21] = 0x25; //gate access log file
        serviceList[22] = 0x01; //Service key ver
        serviceList[23] = 0x00; //Service key ver
        serviceList[24] = 0x08; //Gate access log for transfer file
        serviceList[25] = 0x26; //gate access log for transfer file
        serviceList[26] = 0x01; //Service key ver
        serviceList[27] = 0x00; //Service key ver

        int ret = mutualAuthV2WithFeliCa(serviceNum, serviceList);
        if (ret == 0) {
            return 0;
        }
        return 1;
    }

    public int readFiles(byte[] readData, int[] readLen) throws Exception {
        long st = System.currentTimeMillis();
        byte serviceNum, blockNum;
        byte[] serviceList = new byte[64], blockList = new byte[128];
        /*serviceNum = 7;
        serviceList[0] = 0x0A; //Issuer info file
        serviceList[1] = 0x11; //Issuer info file
        serviceList[2] = 0x01; //Service key ver
        serviceList[3] = 0x00; //Service key ver
        */
        /*serviceList[4] = 0x0A; //Personal info file
        serviceList[5] = 0x12; //Personal info file
        serviceList[6] = 0x01; //Service key ver
        serviceList[7] = 0x00; //Service key ver*/
        /*serviceList[4] = 0x0A; //Card attribute info file
        serviceList[5] = 0x13; //Card attribute info file
        serviceList[6] = 0x01; //Service key ver
        serviceList[7] = 0x00; //Service key ver
        serviceList[8] = 0x10; //EPurse info file
        serviceList[9] = 0x14; //EPurse info file
        serviceList[10] = 0x01; //Service key ver
        serviceList[11] = 0x00; //Service key ver
        serviceList[12] = 0x0A; //Operator info file
        serviceList[13] = 0x21; //Operator info file
        serviceList[14] = 0x01; //Service key ver
        serviceList[15] = 0x00; //Service key ver
        serviceList[16] = 0x0C; //Stored value log file
        serviceList[17] = 0x22; //Stored value log file
        serviceList[18] = 0x01; //Service key ver
        serviceList[19] = 0x00; //Service key ver
        serviceList[20] = 0x0C; //Gate access log file
        serviceList[21] = 0x25; //gate access log file
        serviceList[22] = 0x01; //Service key ver
        serviceList[23] = 0x00; //Service key ver
        serviceList[24] = 0x08; //Gate access log for transfer file
        serviceList[25] = 0x26; //gate access log for transfer file
        serviceList[26] = 0x01; //Service key ver
        serviceList[27] = 0x00; //Service key ver*/


        int ret = mutualAuthWithFelicaCard();
        if (ret == 0) {
            return 0;
        }
        long et = System.currentTimeMillis();
        Log.d("mutual_auth_time", "mutual auth time: " + (et - st));
        st = System.currentTimeMillis();

        blockNum = 10;
        blockList[0] = (byte) 0x80;    //file 1, issuer file
        blockList[1] = 0x00;    //0th block
        /*blockList[2] = (byte) 0x81;    //file 2, personal info
        blockList[3] = 0x00;    //0th block
        blockList[4] = (byte) 0x81;    //file 2, personal info
        blockList[5] = 0x01;    //1th block
        blockList[6] = (byte) 0x81;    //file 2, personal info
        blockList[7] = 0x02;    //2nd block
        blockList[8] = (byte) 0x81;    //file 2, personal info
        blockList[9] = 0x03;    //3rd block*/
        blockList[2] = (byte) 0x81;    //file 3, card attrib file
        blockList[3] = 0x00;    //0th block
        blockList[4] = (byte) 0x81;    //file 3, card attrib file
        blockList[5] = 0x01;    //1th block
        blockList[6] = (byte) 0x82;    //file 4, ePurse
        blockList[7] = 0x00;    //0th block
        blockList[8] = (byte) 0x83;    //file 5, Operator info file
        blockList[9] = 0x00;    //0th block
        blockList[10] = (byte) 0x84;    //file 6, History file
        blockList[11] = 0x00;    //0th block
        blockList[12] = (byte) 0x84;    //file 6, History file
        blockList[13] = 0x01;    //1th block
        blockList[14] = (byte) 0x84;    //file 6, History file
        blockList[15] = 0x02;    //2th block
        blockList[16] = (byte) 0x85;    //file 7, Gate Access Log file
        blockList[17] = 0x00;    //0th block
        blockList[18] = (byte) 0x86;    //file 7, Gate Access Log for transfer file
        blockList[19] = 0x00;    //0th block

        // log.d("readFiles_time", "readFiles: ----- "+(eTime-sTime));
//        blockList[16] = (byte) 0x86;    //file 7, Gate Access Log for transfer file
//        blockList[18] = 0x01;    //1th block

        ret = readDataBlock(blockNum, blockList, readLen, readData);
        if (ret == 0) {
            return 0;
        }

        et = System.currentTimeMillis();
        Log.d("card_read_time", "card read time: " + (et - st));

        return 1;
    }

    public int readFilesForTransactionHistory(byte[] readData, int[] readLen) throws Exception {
        long sTime = System.currentTimeMillis();
        byte serviceNum, blockNum;
        byte[] serviceList = new byte[64], blockList = new byte[128];
        serviceNum = 3;
        serviceList[0] = 0x0A; //Card attribute info file
        serviceList[1] = 0x13; //Card attribute info file
        serviceList[2] = 0x01; //Service key ver
        serviceList[3] = 0x00; //Service key ver
        serviceList[4] = 0x0A; //Operator info file
        serviceList[5] = 0x21; //Operator info file
        serviceList[6] = 0x01; //Service key ver
        serviceList[7] = 0x00; //Service key ver
        serviceList[8] = 0x0C; //Stored value log file
        serviceList[9] = 0x22; //Stored value log file
        serviceList[10] = 0x01; //Service key ver
        serviceList[11] = 0x00; //Service key ver


        int ret = mutualAuthV2WithFeliCa(serviceNum, serviceList);
        if (ret == 0) {
            return 0;
        }

        blockNum = 4;
        blockList[0] = (byte) 0x80;    //file 3, card attrib file
        blockList[1] = 0x00;    //0th block
        blockList[2] = (byte) 0x80;    //file 3, card attrib file
        blockList[3] = 0x01;    //1th block
        blockList[4] = (byte) 0x81;    //file 5, Operator info file
        blockList[5] = 0x00;    //0th block
        blockList[6] = (byte) 0x82;    //file 6, History file
        blockList[7] = 0x00;    //0th block
        long eTime = System.currentTimeMillis();

        ret = readDataBlock(blockNum, blockList, readLen, readData);
        if (ret == 0) {
            return 0;
        }
        return 1;
    }

    public byte[] readWithoutAuth() throws CardReadException {
        long sTime = System.currentTimeMillis();
        byte[] idm = getIDm();

        byte numOfService = 3;
        byte[] serviceCodes = {
                0x0F, 0x22,
                0x0F, 0x25,
                0x0B, 0x26,
        };
        byte numOfBlock = 3;
        byte[] blocks = {
                (byte) 0x80, 0x00,
                (byte) 0x81, 0x00,
                (byte) 0x82, 0x00,
        };
        byte[] command = new byte[1 + idm.length + 1 + serviceCodes.length + 1 + blocks.length];
        int[] felicaResLen = new int[1];
        byte[] felicaResBuf = new byte[256];
        command[0] = 0x06;
        System.arraycopy(idm, 0, command, 1, idm.length);
        command[9] = numOfService;
        System.arraycopy(serviceCodes, 0, command, 10, serviceCodes.length);
        command[10 + serviceCodes.length] = numOfBlock;
        System.arraycopy(blocks, 0, command, 10 + serviceCodes.length + 1, blocks.length);
        int res = transmitDataToFeliCaCard(command.length, command, felicaResLen, felicaResBuf);
        long eTime = System.currentTimeMillis();
        // log.d("readWithoutAuth_time", "readWithoutAuth: ----- "+(eTime-sTime));
        if (res == 0)
            throw new CardReadException("Can not read card at this moment.Please try again later");
        return Arrays.copyOfRange(felicaResBuf, 12, felicaResLen[0]);
    }

    public void recharge() throws Exception {
        int serviceNumber = 3;
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
        int blockNumber = 8;
        byte[] blockNumberList = new byte[blockNumber * 2];
        /*blockNumberList[0] = (byte) 0x80;
        blockNumberList[1] = (byte) 0x00;
        blockNumberList[2] = (byte) 0x80;
        blockNumberList[3] = (byte) 0x01;
        blockNumberList[4] = (byte) 0x81;
        blockNumberList[5] = (byte) 0x00;
        blockNumberList[6] = (byte) 0x82;
        blockNumberList[7] = (byte) 0x00;*/

        blockNumberList[0] = (byte) 0x80;    //file 1, issuer file
        blockNumberList[1] = 0x00;    //0th block
        blockNumberList[2] = (byte) 0x81;    //file 3, card attrib file
        blockNumberList[3] = 0x00;    //0th block
        blockNumberList[4] = (byte) 0x81;    //file 3, card attrib file
        blockNumberList[5] = 0x01;    //1th block
        blockNumberList[6] = (byte) 0x82;    //file 4, ePurse
        blockNumberList[7] = 0x00;    //0th block
        blockNumberList[8] = (byte) 0x83;    //file 5, Operator info file
        blockNumberList[9] = 0x00;    //0th block
        blockNumberList[10] = (byte) 0x84;    //file 6, History file
        blockNumberList[11] = 0x00;    //0th block
        blockNumberList[12] = (byte) 0x85;    //file 7, Gate Access Log file
        blockNumberList[13] = 0x00;    //0th block
        blockNumberList[14] = (byte) 0x86;    //file 7, Gate Access Log for transfer file
        blockNumberList[15] = 0x00;    //0th block

        byte[] is = felicaCardDetail.getIssuerInfo().getData();
        byte[] attributeInfoData = felicaCardDetail.getAttributeInfo().getData();
        byte[] ePurseData = felicaCardDetail.getEPurseInfo().getData();
        byte[] op = felicaCardDetail.getOperatorInfo().getData();
        byte[] storageInfoData = felicaCardDetail.getStoredLogInformation().getData();
        byte[] g = felicaCardDetail.getGateAccessLogInformation().getData();
        byte[] gc = felicaCardDetail.getGateAccessLogInformationForTransfer().getBlock0Data();
        // make data

        ByteBuffer byteBuffer = ByteBuffer.allocate(is.length + attributeInfoData.length + ePurseData.length + op.length +
                storageInfoData.length + g.length + gc.length);
        byteBuffer.put(is);
        byteBuffer.put(attributeInfoData);
        byteBuffer.put(ePurseData);
        byteBuffer.put(op);
        byteBuffer.put(storageInfoData);
        byteBuffer.put(g);
        byteBuffer.put(gc);
        writeInCard(serviceNumber, serviceCodeList, blockNumber, blockNumberList, byteBuffer.array());
    }

    private int writeBlockData(int blockNum, int blockLen, byte[] blockList, byte[] blockData) throws Exception {
        long _ret;

        byte[] felicaCmdParams = new byte[256];
        int felicaCmdParamsLen;

        byte[] samResBuf = new byte[262];
        byte[] felicaCmd = new byte[262];
        byte[] felicaRes = new byte[262];
        int[] samResLen = new int[1];
        int[] felicaResLen = new int[1];
        int[] felicaCmdLen = new int[1];
        felicaCmdParams[0] = this.idt[0];                        // this.idt
        felicaCmdParams[1] = this.idt[1];                        // IDt
        felicaCmdParams[2] = (byte) blockNum;                    // Number of Blocks
        System.arraycopy(blockList, 0, felicaCmdParams, 3, blockLen);        // block list
        System.arraycopy(blockData, 0, felicaCmdParams, 3 + blockLen, blockNum * 16);        // block list
        felicaCmdParamsLen = 3 + blockLen + blockNum * 16;

        _ret = sam.askFeliCaCmdToSAM(SAMCommandCodes.SAM_COMMAND_CODE_WRITE, felicaCmdParamsLen, felicaCmdParams, felicaCmdLen, felicaCmd);
        Log.d("write_in_card_recharge1", "len: " + felicaCmdLen[0] + ",askFeliCaCmdToSAM: " + Utils.byteToHex(felicaCmd));
        if (_ret == 0) {
            return 0;
        }

        _ret = transmitDataToFeliCaCard(felicaCmdLen[0] - 2, felicaCmd, felicaResLen, felicaRes);
        Log.d("write_in_card_recharge2", "len: " + felicaResLen[0] + ",transmitDataToFeliCaCard: " + Utils.byteToHex(felicaRes));

        if (_ret == 0) {
            return 0;
        }
        _ret = sam.sendCardResultToSAM(felicaResLen[0], felicaRes, samResLen, samResBuf);
        Log.d("write_in_card_recharge3", "len: " + samResLen[0] + ",transmitDataToFeliCaCard: " + Utils.byteToHex(samResBuf));

        if (_ret == 0) {
            return 0;
        }

        return 1;
    }

    public int getBalance() throws CardReadException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        return Utils.convertTwosComplementByteArrayToLittleIndian(felicaCardDetail.getStoredLogInformation().getCardBalance(), 3);
    }

    public int getDeductBalanceOnRide() throws CardReadException, WrongServiceIdException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        String serviceId = Utils.byteToHex(new byte[]{felicaCardDetail.getStoredLogInformation().getServiceClassificationCode(),
                felicaCardDetail.getStoredLogInformation().getContextCode()}).toUpperCase();
        /*if (serviceId.equals(RideAndAlight.RIDE_AND_DEDUCTION_FROM_SV_NOT_NEGATIVE) || serviceId.equals(RideAndAlight.RIDE_AND_DEDUCTION_FROM_SV_NEGATIVE)) {
            return Utils.charArrayToIntLE(felicaCardDetail.getGateAccessLogInformation().getAmountOfDistanceFare(), 3);
        }*/

        if (Arrays.asList(ServiceCode.RIDE.getCodes()).contains(serviceId)) {
            return Utils.charArrayToIntLE(felicaCardDetail.getGateAccessLogInformation().getAmountOfDistanceFare(), 3);
        }
        throw new WrongServiceIdException("Customer not in ride mode");
    }

    public int getRefundBalanceOnAlight() throws CardReadException, WrongServiceIdException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        String serviceId = Utils.byteToHex(new byte[]{felicaCardDetail.getStoredLogInformation().getServiceClassificationCode(),
                felicaCardDetail.getStoredLogInformation().getContextCode()}).toUpperCase();
        /*if (serviceId.equals(RideAndAlight.ALIGHT_AND_REFUND_OF_SV_NOT_NEGATIVE) || serviceId.equals(RideAndAlight.ALIGHT_AND_REFUND_OF_SV_NEGATIVE)) {
            return Utils.charArrayToIntLE(felicaCardDetail.getGateAccessLogInformation().getAmountOfDistanceFare(), 3);
        }*/
        if (Arrays.asList(ServiceCode.ALIGHT.getCodes()).contains(serviceId)) {
            return Utils.charArrayToIntLE(felicaCardDetail.getGateAccessLogInformation().getAmountOfDistanceFare(), 3);
        }
        throw new WrongServiceIdException("Customer not in alight mode");
    }

    public boolean isStatusRide() throws CardReadException, WrongServiceIdException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(felicaCardDetail.getGateAccessLogInformation().getStatusFlag()) & mask;
        return statusFlag != 0;
    }

    public boolean isStatusAlight() throws CardReadException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        /*String serviceId = Utils.byteToHex(new byte[]{felicaCardDetail.getStoredLogInformation().getServiceClassificationCode(),
                felicaCardDetail.getStoredLogInformation().getContextCode()}).toUpperCase();*/
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(felicaCardDetail.getGateAccessLogInformation().getStatusFlag()) & mask;
        return statusFlag == 0;
    }

    // get transaction history

    public List<TransactionHistory> getTransactionHistory() {
        List<TransactionHistory> transactionHistories = new ArrayList<>();
        byte[] serviceCodes = new byte[]{0x0C, 0x22, 0x01, 0x00};
        int serviceNo = 1;
        try {
            int res = mutualAuthV2WithFeliCa((byte) serviceNo, serviceCodes);
            if (res == 0)
                throw new FelicaMutualAuthException("Felica mutual authentication fail. Please try again later");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 20; i++) {
            byte[] block = new byte[]{(byte) 0x80, (byte) i};
            int blockNo = 1;
            byte[] readData = new byte[256];
            int[] readLen = new int[1];
            try {
                int res = readDataBlock((byte) blockNo, block, readLen, readData);
                if (res == 0)
                    throw new CardReadException("Can not read card at this moment.Please try again later");
                byte[] bytes = Arrays.copyOfRange(readData, 3, readData.length);
                StoredLogInformation storedLogInformation = StoredLogInformation.generateData(Arrays.copyOfRange(bytes, 0, readLen[0] - 2));
                String classificationCode = String.format("%02X", storedLogInformation.getServiceClassificationCode());
                String contextCode = String.format("%02X", storedLogInformation.getContextCode());
                String operationName = getOperationName(classificationCode + contextCode);
                if (TextUtils.isEmpty(operationName)) {
                    continue;
                }
                TransactionHistory transactionHistory = new TransactionHistory();
                System.out.println("balance ----- " + Utils.convertByteArrayToBit(storedLogInformation.getCardBalance()));
                transactionHistory.setBalance(Utils.charArrayToIntLE(storedLogInformation.getCardBalance(), 3));
                transactionHistory.setDate(getDate(storedLogInformation.getDate()));


                transactionHistory.setOperation(operationName);

                if (i == 0) {
                    transactionHistory.setAmount(0);
                } else {
                    int amount = transactionHistories.get(i - 1).getBalance() - transactionHistory.getBalance();
                    transactionHistories.get(i - 1).setAmount(amount);
                }
                transactionHistories.add(transactionHistory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (transactionHistories.size() > 1) {
            TransactionHistory history = transactionHistories.get(transactionHistories.size() - 2);
            transactionHistories.get(transactionHistories.size() - 1).setAmount(history.getBalance() - history.getAmount());
        } else if (transactionHistories.size() == 1) {
            transactionHistories.get(0).setAmount(transactionHistories.get(0).getBalance());
        }
        return transactionHistories;
    }

    private String getDate(byte[] bytes) {
        String date = Utils.convertByteArrayToBit(bytes);
        if (date.chars().allMatch(c -> c == '0')) {
            return "";
        }
        int year = Integer.parseInt(date.substring(0, 7), 2);
        int month = Integer.parseInt(date.substring(7, 11), 2);
        int day = Integer.parseInt(date.substring(11, 16), 2);

        return String.format(Locale.ENGLISH, "%02d/%02d/%02d", day, month, year);
    }

    private String getOperationName(String serviceId) {
        for (ServiceCode code : ServiceCode.values()) {
            Optional<String> serviceCode = Arrays.stream(code.getCodes()).filter(c -> c.equalsIgnoreCase(serviceId)).findFirst();
            if (serviceCode.isPresent()) {
                return code.getName().substring(0, 1).toUpperCase() + code.getName().substring(1).toLowerCase();
            }
        }
        return null;
    }

    public List<TransactionHistory> getTransactionHistoryWithoutAuth() throws Exception {
        List<TransactionHistory> transactionHistories = new ArrayList<>();

        byte command = (byte) 0x06;

        byte[] idm = getIDm();

        byte[] serviceCode = {(byte) 0x0F, (byte) 0x22}; //	220F -Stored Value Log Information   (Little endian)
        byte[] serviceCodeForGateAccessLog = {(byte) 0x0F, (byte) 0x25}; //	250F -Gate Access Log Information   (Little endian)
        byte[] blocks = {
                (byte) 0x80, 0x00
        };
        int[] felicaResLen = new int[1];
        byte[] felicaResBuf = new byte[256];
        byte[] commands = new byte[33];
        commands[0] = command;
        System.arraycopy(idm, 0, commands, 1, idm.length);
        commands[9] = 0x01;
        System.arraycopy(serviceCodeForGateAccessLog, 0, commands, 10, serviceCodeForGateAccessLog.length);
        commands[12] = 1;
        System.arraycopy(blocks, 0, commands, 13, blocks.length);


        int responseCode = transmitDataToFeliCaCard(commands.length, commands, felicaResLen, felicaResBuf);
        if (responseCode == 0) {
//                    getTransactionHistoryWithoutAuth();
            throw new CardReadException("Can not read card at this moment.Please try again later");
        }
        byte[] data = Arrays.copyOfRange(felicaResBuf, 12, felicaResLen[0]);
        for (int i = 0; i < data.length; i += 16) {
            GateAccessLogInformation accessLogInformation = GateAccessLogInformation.generateData(Arrays.copyOfRange(data, i, i + 16));
            char[] s = Utils.convertByteArrayToBit(accessLogInformation.getStatusFlag()).toCharArray();
            if (s[0] == '1' && s[4] == '0') {
                TransactionHistory history = new TransactionHistory();
                history.setBalance(0);
                history.setDate(getDate(accessLogInformation.getDate()));
                history.setOperation("Entry");
                transactionHistories.add(history);
            }
        }


        for (int i = 0; i < 2; i++) {

            felicaResLen = new int[1];
            felicaResBuf = new byte[256];

            ByteBuffer byteBuffer = ByteBuffer.allocate(20);
            for (int k = i * 10; k < 10 + i * 10; k++) {
                byteBuffer.put(new byte[]{(byte) 0x80, (byte) k});
            }
            byte[] block = byteBuffer.array();
            byteBuffer.clear();
            byte[] readCommand = new byte[33];
            readCommand[0] = command;
            System.arraycopy(idm, 0, readCommand, 1, idm.length);
            readCommand[9] = 0x01;
            System.arraycopy(serviceCode, 0, readCommand, 10, serviceCode.length);
            readCommand[12] = 10;
            System.arraycopy(block, 0, readCommand, 13, block.length);
            try {
                int res = transmitDataToFeliCaCard(readCommand.length, readCommand, felicaResLen, felicaResBuf);
                if (res == 0) {
//                    getTransactionHistoryWithoutAuth();
                    throw new CardReadException("Can not read card at this moment.Please try again later");
                }
                byte[] bytes = Arrays.copyOfRange(felicaResBuf, 12, felicaResLen[0]);
                // log.d("getTransactionHistoryWithoutAuth", "getTransactionHistoryWithoutAuth: " + bytes.length);
                int j = 0;
                for (; j < bytes.length; j += 16) {
                    StoredLogInformation storedLogInformation = StoredLogInformation.generateData(Arrays.copyOfRange(bytes, j, j + 16));
                    String classificationCode = String.format("%02X", storedLogInformation.getServiceClassificationCode());
                    String contextCode = String.format("%02X", storedLogInformation.getContextCode());
                    String operationName = getOperationName(classificationCode + contextCode);
                    if (Utils.byteToHex(storedLogInformation.getStoredValueLogId()).equals("0000")) {
                        break;
                    }
                    TransactionHistory transactionHistory = new TransactionHistory();
                    System.out.println("balance ----- " + Utils.convertByteArrayToBit(storedLogInformation.getCardBalance()));
                    transactionHistory.setBalance(Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformation.getCardBalance(), 3));
                    transactionHistory.setDate(getDate(storedLogInformation.getDate()));

                    transactionHistory.setOperation(operationName);


                    transactionHistories.add(transactionHistory);
                }
            } catch (Exception e) {
                throw new Exception(e);
            }
        }

        for (int i = 0; i < transactionHistories.size(); i++) {
            TransactionHistory history = transactionHistories.get(i);
            if (i == 0) {
                transactionHistories.get(0).setAmount(0);
            } else {
                int amount = transactionHistories.get(i - 1).getBalance() - history.getBalance();
                transactionHistories.get(i - 1).setAmount(amount);
            }
        }

        if (transactionHistories.size() > 1) {
            TransactionHistory history = transactionHistories.get(transactionHistories.size() - 2);
            transactionHistories.get(transactionHistories.size() - 1).setAmount(history.getBalance() - history.getAmount());
            if (transactionHistories.get(0).getOperation().equalsIgnoreCase("entry")) {
                transactionHistories.get(0).setAmount(0);
                transactionHistories.get(0).setBalance(transactionHistories.get(1).getBalance());
            }
        } else if (transactionHistories.size() == 1) {
            transactionHistories.get(0).setAmount(transactionHistories.get(0).getBalance());
        }

        for (int i = 1; i < transactionHistories.size(); i++) {
            TransactionHistory history = transactionHistories.get(i);
            if (history.getOperation().equalsIgnoreCase("recharge")) {
                if (transactionHistories.get(i - 1).getOperation().equalsIgnoreCase("alight")) {
                    TransactionHistory alightTransaction = transactionHistories.get(i - 1);
                    while (++i < transactionHistories.size()) {
                        if (transactionHistories.get(i).getOperation().equalsIgnoreCase("ride")) {
                            TransactionHistory rideTransaction = transactionHistories.get(i);
                            int cashbackAmount = rideTransaction.getBalance() - rideTransaction.getAmount();
//                            alightTransaction.
                        }
                    }
                }
            }
        }

        return transactionHistories;
    }


    public int getCashbackAmount() {
        List<StoredLogInformation> storedLogInformationList = felicaCardDetail.getStoredLogInformationList();
        for(int i = 0;i < storedLogInformationList.size();i++){
            String serviceId = String.format("%02X%02X",storedLogInformationList.get(i).getServiceClassificationCode(),
                    storedLogInformationList.get(i).getContextCode());
            if(Arrays.asList("D220","D320").contains(serviceId.toUpperCase()) && i+1 < storedLogInformationList.size()){
                return Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformationList.get(i+1).getCardBalance(),3);
            }
        }

        return 0;
    }

    @Override
    public void removeCardIdList() {
        Utils.getInstance().getCardList().clear();
    }
}
