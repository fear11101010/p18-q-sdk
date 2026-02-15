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

/**
 * Represents a FeliCa card interface for reading and writing operations on bus validator systems.
 * This class implements the ReadWriteInCard and ClearCardId interfaces to manage FeliCa smart card
 * transactions including authentication, balance management, and transaction history.
 *
 * <p>The FelicaCard class provides comprehensive functionality for:
 * <ul>
 *   <li>Card detection and initialization</li>
 *   <li>Mutual authentication with FeliCa cards</li>
 *   <li>Reading card data including issuer info, e-purse info, and transaction logs</li>
 *   <li>Writing data to card blocks</li>
 *   <li>Transaction history retrieval</li>
 *   <li>Balance and cashback calculations</li>
 * </ul>
 *
 * <p>This class follows the Singleton pattern to ensure only one instance manages card operations.
 *
 * @author Bus Validator SDK Team
 * @version 1.0
 * @see ReadWriteInCard
 * @see ClearCardId
 * @see FelicaCardDetail
 */
public class FelicaCard implements ReadWriteInCard, ClearCardId {

    /**
     * Singleton instance of FelicaCard.
     */
    private static FelicaCard felicaCard;
    /**
     * SAM (Secure Access Module) instance for secure operations.
     */
    @Getter
    private final Sam sam;

    /**
     * IDm (Manufacturer ID) - 8 bytes unique identifier of the FeliCa card.
     */
    @Getter
    private byte[] iDm;

    /**
     * PMm (Manufacturer Parameter) - 8 bytes containing card manufacturing information.
     */
    @Getter
    private byte[] pMm;

    /**
     * IDt (Card Identifier during session) - temporary identifier for the current session.
     */
    private byte[] idt;

    /**
     * IDi (Card Individual Number) - unique card identification number.
     */
    @Getter
    private byte[] idi;

    /**
     * Initial IDi value used for card initialization verification. (value always 0)
     */
    private byte[] initIdi;

    /**
     * System code identifying the card system (2 bytes little indian).
     */
    @Getter
    private byte[] systemCode;

    /**
     * Detailed information structure containing all card data blocks.
     */
    @Getter
    @Setter
    private FelicaCardDetail felicaCardDetail;

    /**
     * Current balance stored on the card in the smallest currency unit.
     */
    @Getter
    private int currentBalance;

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the card configuration using BasicOper.
     *
     * @param sam the SAM instance for secure operations
     */
    private FelicaCard(Sam sam) {
        this.sam = sam;
        String[] result = BasicOper.dc_config_card(3).split("\\|", -1);
        if (result[0].equals("0000")) {
            // log.d("FelicaCard", "FelicaCard: Card config successful--" + result[1]);
        }
    }

    /**
     * Returns the singleton instance of FelicaCard.
     * Creates a new instance if one doesn't exist.
     *
     * @param sam the SAM instance required for card operations
     * @return the singleton FelicaCard instance
     */
    public static FelicaCard getInstance(Sam sam) {
        if (felicaCard == null) {
            felicaCard = new FelicaCard(sam);
        }
        return felicaCard;

    }

    /**
     * Detects and initializes a FeliCa card by performing a reset operation.
     * Extracts IDm, PMm, and system code from the card response.
     *
     * @throws CardNotFoundException if no card is detected or card response is invalid
     * @throws Exception if card detection fails
     */
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
            else {
                throw new CardNotFoundException("No Card Detected. Please tap card");
            }
        }

    }

    /**
     * Reads complete card data including issuer info, e-purse info, operator info,
     * stored value logs, and gate access logs. Validates card status and checks
     * against blacklist.
     *
     * @param dataInterface callback interface for transaction data handling
     * @return 1 if read operation is successful
     * @throws CardReadException if card cannot be read
     * @throws CardUnissuedException if card is not second issued
     * @throws CardIdSameException if card is not properly initialized
     * @throws CardNotActiveException if card is not active
     * @throws CardBlackListException if card is blacklisted
     * @throws VoidCardException if card is void
     * @throws Exception for other card reading errors
     */
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
        /*if (ValidateCard.isMRTCard(felicaCardDetail.getIssuerInfo().getCardIssuerID())) {
            throw new MRTCardNotAllowedException("MRT Card is not allowed");
        }*/
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

    /**
     * Reads card data specifically for transaction history retrieval with mutual auth.
     * This is a lighter operation compared to full card read.
     *
     * @param dataInterface callback interface for transaction data handling
     * @return 1 if read operation is successful
     * @throws CardReadException if card cannot be read
     * @throws Exception for other card reading errors
     */
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
    /**
     * Populates the FelicaCardDetail object with data read from the card.
     * Parses various information blocks including issuer info, attribute info,
     * e-purse info, operator info, and transaction logs.
     *
     * @param data byte array containing card data
     * @param len length of valid data in the array
     */
    private void populateFelicaCard(byte[] data, int len) {
        byte[] bytes = Arrays.copyOfRange(data, 0, len - 2);
        IssuerInfo issuerInfo = IssuerInfo.generateData(Arrays.copyOfRange(bytes, 0, 16));
//        PersonalInfo personalInfo = PersonalInfo.generateData(Arrays.copyOfRange(bytes, 16, 16 * 5));
        AttributeInfo attributeInfo = AttributeInfo.generateData(Arrays.copyOfRange(bytes, 16, 16 * 3));
        EPurseInfo ePurseInfo = EPurseInfo.generateData(Arrays.copyOfRange(bytes, 16 * 3, 16 * 4));
        OperatorInfo operatorInfo = OperatorInfo.generateData(Arrays.copyOfRange(bytes, 16 * 4, 16 * 5));
        StoredLogInformation storedLogInformation = StoredLogInformation.generateData(Arrays.copyOfRange(bytes, 16 * 5, 16 * 6));
        List<StoredLogInformation> storedLogInformationList = getStoredLogInformationList(Arrays.copyOfRange(bytes, 16 * 5, 16 * 10));
        GateAccessLogInformation gateAccessLogInformation = GateAccessLogInformation.generateData(Arrays.copyOfRange(bytes, 16 * 10, 16 * 11));
//        GateAccessLogInformation gateAccessLogInformation = GateAccessLogInformation.generateData(Arrays.copyOfRange(bytes, 16 * 6, 16 * 7));
        GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer = GateAccessLogInformationForTransfer.generateData(Arrays.copyOfRange(bytes, 16 * 11, 16 * 12));
//        GateAccessLogInformationForTransfer gateAccessLogInformationForTransfer = GateAccessLogInformationForTransfer.generateData(Arrays.copyOfRange(bytes, 16 * 7, 16 * 8));

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
        /*this.felicaCardDetail = new FelicaCardDetail(issuerInfo, attributeInfo, ePurseInfo,
                operatorInfo, storedLogInformation,gateAccessLogInformation, gateAccessLogInformationForTransfer);*/

    }

    /**
     * Extracts stored log information list from card data.
     * Stored log information contain 20 blocks
     * Each log entry is 16 bytes.
     *
     * @param bytes byte array containing stored log data
     * @return list of StoredLogInformation objects
     */
    public List<StoredLogInformation> getStoredLogInformationList(byte[] bytes) {
        List<StoredLogInformation> storedLogInformationList = new ArrayList<>();
        for(int i = 0;i < bytes.length;i += 16){
            storedLogInformationList.add(StoredLogInformation.generateData(Arrays.copyOfRange(bytes,i,i+16)));
        }
        return storedLogInformationList;
    }

    /**
     * Populates card details specifically for transaction history viewing.
     * Performs validation checks but doesn't load complete card information.
     *
     * @param data byte array containing card data
     * @param len length of valid data
     * @param dataInterface callback interface for transaction data
     * @throws CardUnissuedException if card is not second issued
     * @throws CardIdSameException if card is not initialized
     * @throws CardNotActiveException if card is not active
     * @throws CardBlackListException if card is blacklisted
     * @throws VoidCardException if card is void
     * @throws Exception for other validation errors
     */
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

    /**
     * Performs mutual authentication version 2 with the FeliCa card.
     * This establishes a secure session for reading/writing operations.
     *
     * @param serviceCodeNum number of service codes to authenticate
     * @param serviceCodeKeyVerList array containing service codes and key versions
     * @return 1 if authentication successful, 0 otherwise
     * @throws Exception if authentication process fails
     */
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

    /**
     * Transmits a command to the FeliCa card and receives the response.
     *
     * @param felicaCmdLen length of the command to send
     * @param felicaCmdBuf buffer containing the command
     * @param felicaResLen array to store response length
     * @param felicaResBuf buffer to store response data
     * @return 1 if transmission successful, 0 otherwise
     */
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

    /**
     * Reads card data and returns the FelicaCard instance.
     * Implements ReadWriteInCard interface method.
     *
     * @return FelicaCard instance with populated data, or null if read fails
     * @throws Exception if card reading fails
     */
    @Override
    public FelicaCard readData() throws Exception {
        if (readCard(transactionData -> Log.d("transaction_data", new Gson().toJson(transactionData))) == 0) {
            return null;
        }
        return this;
    }

    /**
     * Writes data to specified blocks on the FeliCa card.
     *
     * @param serviceNum number of services involved
     * @param serviceList array of service codes
     * @param blockNum number of blocks to write
     * @param blockList array specifying which blocks to write
     * @param blockData actual data to write
     * @return 1 if write successful, 0 otherwise
     * @throws Exception if write operation fails
     */
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

    /**
     * Reads specified data blocks from the card.
     *
     * @param blockNum number of blocks to read
     * @param blockList array specifying which blocks to read
     * @param readLen array to store length of read data
     * @param readData buffer to store read data
     * @return 1 if read successful, 0 otherwise
     * @throws Exception if read operation fails
     */
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

    /**
     * Performs mutual authentication with the FeliCa card for standard operations.
     * Authenticates access to issuer info, card attributes, e-purse info,
     * operator info, stored value logs, and gate access logs.
     *
     * @return 1 if authentication successful, 0 otherwise
     * @throws Exception if authentication fails
     */
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

    /**
     * Reads all necessary files from the card after successful authentication.
     * Reads issuer info, card attributes, e-purse info, operator info,
     * stored value logs, gate access logs, and transfer logs.
     *
     * @param readData buffer to store read data
     * @param readLen array to store length of read data
     * @return 1 if read successful, 0 otherwise
     * @throws Exception if read operation fails
     */
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

        blockNum = 12;
        blockList[0] = (byte) 0x80;    //file 1, issuer file
        blockList[1] = 0x00;    //0th block
        blockList[2] = (byte) 0x81;    //file 2, personal info
        blockList[3] = 0x00;    //0th block
        blockList[4] = (byte) 0x81;    //file 2, personal info
        blockList[5] = 0x01;    //1th block
        blockList[6] = (byte) 0x81;    //file 2, personal info
        blockList[7] = 0x02;    //2nd block
        blockList[8] = (byte) 0x81;    //file 2, personal info
        blockList[9] = 0x03;    //3rd block
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
        blockList[16] = (byte) 0x84;    //file 6, History file
        blockList[17] = 0x03;    //3th block
        blockList[18] = (byte) 0x84;    //file 6, History file
        blockList[19] = 0x04;    //2th block
        blockList[20] = (byte) 0x85;    //file 7, Gate Access Log file
        blockList[21] = 0x00;    //0th block
        blockList[22] = (byte) 0x86;    //file 7, Gate Access Log for transfer file
        blockList[23] = 0x00;    //0th block
        /*blockNum = 8;
        blockList[0] = (byte) 0x80;    //file 1, issuer file
        blockList[1] = 0x00;    //0th block
        *//*blockList[2] = (byte) 0x81;    //file 2, personal info
        blockList[3] = 0x00;    //0th block
        blockList[4] = (byte) 0x81;    //file 2, personal info
        blockList[5] = 0x01;    //1th block
        blockList[6] = (byte) 0x81;    //file 2, personal info
        blockList[7] = 0x02;    //2nd block
        blockList[8] = (byte) 0x81;    //file 2, personal info
        blockList[9] = 0x03;    //3rd block*//*
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
        blockList[12] = (byte) 0x85;    //file 7, Gate Access Log file
        blockList[13] = 0x00;    //0th block
        blockList[14] = (byte) 0x86;    //file 7, Gate Access Log for transfer file
        blockList[15] = 0x00;    //0th block*/
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

    /**
     * Reads card files specifically for transaction history retrieval.
     * Lighter operation that only reads necessary blocks for history.
     *
     * @param readData buffer to store read data
     * @param readLen array to store length of read data
     * @return 1 if read successful, 0 otherwise
     * @throws Exception if read operation fails
     */
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

    /**
     * Reads card data without authentication.
     * Used for accessing open blocks that don't require secure access.
     *
     * @return byte array containing read data
     * @throws CardReadException if card cannot be read
     */
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

    /**
     * Writes data to specified blocks on the card.
     * Internal method that handles the actual write operation through SAM.
     *
     * @param blockNum number of blocks to write
     * @param blockLen total length of block list data
     * @param blockList array specifying which blocks to write
     * @param blockData actual data to write
     * @return 1 if write successful, 0 otherwise
     * @throws Exception if write operation fails
     */
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

    /**
     * Gets the current balance from the stored value log.
     *
     * @return current balance in smallest currency unit
     * @throws CardReadException if card data is not available
     */
    public int getBalance() throws CardReadException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        return Utils.convertTwosComplementByteArrayToLittleIndian(felicaCardDetail.getStoredLogInformation().getCardBalance(), 3);
    }

    /**
     * Gets the deducted balance amount when card is in ride mode.
     *
     * @return amount deducted during ride
     * @throws CardReadException if card data is not available
     * @throws WrongServiceIdException if card is not in ride mode
     */
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

    /**
     * Gets the refund balance amount when card is in alight mode.
     *
     * @return amount to be refunded during alight
     * @throws CardReadException if card data is not available
     * @throws WrongServiceIdException if card is not in alight mode
     */
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

    /**
     * Checks if the card status indicates ride mode (entry without exit).
     *
     * @return true if card is in ride status, false otherwise
     * @throws CardReadException if card data is not available
     * @throws WrongServiceIdException if service ID validation fails
     */
    public boolean isStatusRide() throws CardReadException, WrongServiceIdException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(felicaCardDetail.getGateAccessLogInformation().getStatusFlag()) & mask;
        return statusFlag != 0;
    }

    /**
     * Checks if the card status indicates alight mode (exit after entry).
     *
     * @return true if card is in alight status, false otherwise
     * @throws CardReadException if card data is not available
     */
    public boolean isStatusAlight() throws CardReadException {
        if (felicaCardDetail == null) throw new CardReadException("Please read card first");
        /*String serviceId = Utils.byteToHex(new byte[]{felicaCardDetail.getStoredLogInformation().getServiceClassificationCode(),
                felicaCardDetail.getStoredLogInformation().getContextCode()}).toUpperCase();*/
        int mask = 1 << 15;
        int statusFlag = Utils.byteArrayToInt(felicaCardDetail.getGateAccessLogInformation().getStatusFlag()) & mask;
        return statusFlag == 0;
    }

    // get transaction history

    /**
     * Retrieves complete transaction history from the card with authentication.
     * Reads all 20 stored value log entries and constructs transaction history objects.
     *
     * @return list of TransactionHistory objects containing all transactions
     * @throws RuntimeException if transaction history retrieval fails
     */
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

    /**
     * Converts date byte array from card format to readable date string.
     *
     * @param bytes date in card binary format
     * @return formatted date string in DD/MM/YY format, or empty string if invalid
     */
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

    /**
     * Gets the operation name from service ID code.
     * Maps service classification and context codes to operation names.
     *
     * @param serviceId combined service classification and context code
     * @return operation name (e.g., "Ride", "Alight", "Recharge"), or null if not found
     */
    private String getOperationName(String serviceId) {
        for (ServiceCode code : ServiceCode.values()) {
            Optional<String> serviceCode = Arrays.stream(code.getCodes()).filter(c -> c.equalsIgnoreCase(serviceId)).findFirst();
            if (serviceCode.isPresent()) {
                return code.getName().substring(0, 1).toUpperCase() + code.getName().substring(1).toLowerCase();
            }
        }
        return null;
    }

    /**
     * Retrieves transaction history without authentication.
     * Uses open access blocks to read stored value logs and gate access logs.
     * Calculates transaction amounts based on balance differences.
     *
     * @return list of TransactionHistory objects
     * @throws Exception if transaction history retrieval fails
     */
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

    /**
     * Calculates the cashback amount based on stored value log entries.
     * Identifies recharge transactions (service IDs D220, D320) and calculates
     * the cashback by comparing balances before and after recharge.
     *
     * @return cashback amount in smallest currency unit, or 0 if no cashback
     */
    public int getCashbackAmount() {
        List<StoredLogInformation> storedLogInformationList = felicaCardDetail.getStoredLogInformationList();
        for(int i = 0;i < storedLogInformationList.size();i++){
            String serviceId = String.format("%02X%02X",storedLogInformationList.get(i).getServiceClassificationCode(),
                    storedLogInformationList.get(i).getContextCode());
            if(Arrays.asList("D220","D320").contains(serviceId.toUpperCase()) && i+1 < storedLogInformationList.size()){
                int balance = Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformationList.get(i).getCardBalance(),3);
                int svBalance = Utils.charArrayToIntLE(felicaCardDetail.getEPurseInfo().getBinRemainingSV(), 4);
                return Math.abs(balance - Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformationList.get(i+1).getCardBalance(),3))
                        +(Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformationList.get(0).getCardBalance(),3) - svBalance);
                /*if(balance < 0) {
                    return Math.abs(balance - Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformationList.get(i+1).getCardBalance(),3))
                            +Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformationList.get(0).getCardBalance(),3);
                } else {
                    return Math.abs(balance - Utils.convertTwosComplementByteArrayToLittleIndian(storedLogInformationList.get(i+1).getCardBalance(),3));
                }*/
            }
        }

        return 0;
    }

    /**
     * Removes all card IDs from the internal card list.
     * Implements ClearCardId interface method.
     */
    @Override
    public void removeCardIdList() {
        Utils.getInstance().getCardList().clear();
    }
}
