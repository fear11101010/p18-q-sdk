package com.dtca.busvalidator.busvalidatorsdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.decard.NDKMethod.BasicOper;
import com.decard.driver.utils.HexDump;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.SamSyntaxError;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.Getter;
/**
 * SAM (Secure Access Module) communication handler for Bus Validator SDK.
 *
 * <p>
 * This class manages secure communication between the Android application and
 * the SAM card installed in the reader hardware. It performs:
 * </p>
 *
 * <ul>
 *     <li>SAM initialization and reset</li>
 *     <li>Mutual authentication (Auth1 & Auth2)</li>
 *     <li>Session key derivation</li>
 *     <li>FeliCa command encryption and decryption</li>
 *     <li>CBC-MAC / C-MAC calculation</li>
 *     <li>Secure APDU transmission</li>
 * </ul>
 *
 * <p>
 * Cryptographic operations include:
 * </p>
 *
 * <ul>
 *     <li>AES-128 CBC (NoPadding)</li>
 *     <li>AES-CTR mode encryption</li>
 *     <li>CBC-MAC generation</li>
 *     <li>CMAC using BouncyCastle</li>
 * </ul>
 *
 * <h2>Initialization Flow</h2>
 *
 * <pre>
 * +-------------+
 * | openReader  |
 * +------+------+
 *        |
 *        v
 * +-------------+
 * | resetSam    |
 * +------+------+
 *        |
 *        v
 * +------------------+
 * | setToNormalMode  |
 * +------+-----------+
 *        |
 *        v
 * +--------------+
 * | sendAttention|
 * +------+-------+
 *        |
 *        v
 * +--------------+
 * | sendAuth1    |
 * +------+-------+
 *        |
 *        v
 * +------------------+
 * | checkAuth1Result |
 * +------+-----------+
 *        |
 *        v
 * +--------------+
 * | sendAuth2    |
 * +------+-------+
 *        |
 *        v
 * +------------------+
 * | checkAuth2Result |
 * +------------------+
 * </pre>
 *
 * <p>
 * After successful authentication, session parameters are established:
 * </p>
 *
 * <ul>
 *     <li><b>rar</b> – Reader random number</li>
 *     <li><b>rbr</b> – SAM random number</li>
 *     <li><b>rcr</b> – Card random number</li>
 *     <li><b>kab</b> – Authentication key</li>
 *     <li><b>kYtr</b> – Session encryption key</li>
 *     <li><b>snr</b> – Session sequence counter</li>
 * </ul>
 *
 * <p>
 * This class follows Singleton design pattern.
 * </p>
 *
 * <b>Important:</b>
 * Must call {@link #initSam()} successfully before sending any encrypted FeliCa command.
 *
 * @author arafat
 */
public class Sam {
    private static Sam sam;
    private final static int BUFF_SIZE = 1500;
    private final int samSlot;
    private final byte[] rar = new byte[16];
    private String rwSamNumber;
    private byte[] rcr;
    private byte[] rbr;
    private byte[] snr;
    private byte[] kYtr;
    byte[] kab = new byte[16];
    @Getter
    private Context context;

    private Sam(int samSlot,Context context) {
        this.context = context.getApplicationContext();
        openReader(context.getApplicationContext());
        this.samSlot = samSlot;

    }
    /**
     * Returns singleton instance of SAM handler.
     *
     * @param samSlot SAM slot index
     * @param context Android context
     * @return Sam instance
     */
    public static Sam getInstance(int samSlot,Context context){
        if(sam==null){
            sam = new Sam(samSlot,context);
        }
        return sam;
    }
    private void openReader(Context context) {
        int st = openUSBReader(context);
        if (st < 0) {
            st = openSerialReader();
        }
    }
    /**
     * Initializes SAM and performs full mutual authentication.
     *
     * <p>
     * This method performs:
     * </p>
     * <ol>
     *     <li>Reset SAM</li>
     *     <li>Set SAM to normal mode</li>
     *     <li>Send Attention command</li>
     *     <li>Execute Auth1</li>
     *     <li>Verify Auth1 response</li>
     *     <li>Execute Auth2</li>
     *     <li>Verify Auth2 response</li>
     * </ol>
     *
     * @return SAM response code if successful, null otherwise
     */
    public String initSam() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SamSyntaxError {
        String[] result = BasicOper.dc_setcpu(samSlot).split("\\|", -1);
//        String volResult = BasicOper.dc_SetCpuVoltage(0);
          Log.d("#RD>>> sam_init", String.format("code :%s,data :%s", result[0], result[1]));

        if (resetSam()!=null){
            // // log.d("initSam: ","sam reset successfully");
        }else {
            // // log.d("initSam: ","can not reset sam can not set to normal mode");
            return null;
        }
        System.out.println("#RD>>> SAM Reset");
        String samNormalModeData = setToNormalMode();
        if (samNormalModeData!=null){
            System.out.println("#RD>>> SAM set normal mode success");
            // log.d("initSam: ","sam set to normal mode successfully---"+samNormalModeData);
        }else {
            System.out.println("#RD>>> SAM set normal mode failed");
            // log.d("initSam: ","sam can not set to normal mode");
            return null;
        }
        System.out.println("#RD>>> SAM Normal Mode");
        String samAttention = sendAttention();
        if (samAttention!=null){
            // log.d("initSam: ","sam attention send successfully---"+samAttention);
        }else {
            // log.d("initSam: ","sam can not send attention");
            return null;
        }
        System.out.println("#RD>>> SAM Attention");
        String samAuth1 = sendAuth1();
        if (samAuth1!=null){
            // log.d("initSam: ","sam auth1 send successfully---"+samAuth1);
        }else {
            // log.d("initSam: ","sam can not send auth1");
            return null;
        }
        System.out.println("#RD>>> SAM send Auth1");
        String samAuth1Result = checkAuth1Result(samAuth1);
        if (samAuth1Result!=null){
            // log.d("initSam: ","sam auth1 result check successfully---"+samAuth1Result);
        }else {
            // log.d("initSam: ","sam auth1 result check failed");
            return null;
        }
        System.out.println("#RD>>> SAM check auth1 result");
        String samAuth2 = sendAuth2();
        if (samAuth2!=null){
            // log.d("initSam: ","sam auth2 send successfully---"+samAuth2);
        }else {
            // log.d("initSam: ","sam can not send auth2");
            return null;
        }
        System.out.println("#RD>>> SAM send auth2");
        String samAuth2Result = checkAuth2Result(samAuth2);
        if (samAuth2Result!=null){
            // log.d("initSam: ","sam auth2 result check successfully---"+samAuth2Result);
        }else {
            // log.d("initSam: ","sam auth2 result check failed");
            return null;
        }
        System.out.println("#RD>>> SAM check auth2 result");
        return result[0];
    }
    /**
     * Resets the SAM module and retrieves the ATR (Answer To Reset).
     *
     * <p>This method sends a CPU reset command to the SAM card and
     * returns the response status code.</p>
     *
     * @return SAM response status code ("0000" if successful), or null if failed
     */
    public String resetSam() {
        String[] result = BasicOper.dc_cpureset_hex().split("\\|", -1);
        System.out.println("#RD>>> ATR Value: "+ result[1]);
//        String ppsResult = BasicOper.dc_RequestPPS(samSlot,0x15,2);
//        System.out.println("#RD>>> PPS Result: "+ ppsResult);
        return result[0];
    }
    /**
     * Sets the SAM into Normal Mode.
     *
     * <p>This command must be executed after reset and before starting
     * authentication procedures.</p>
     *
     * @return Hex response string from SAM if successful, otherwise null
     * @throws SamSyntaxError if SAM reports a syntax error (0x7F)
     */
    public String setToNormalMode() throws SamSyntaxError {
        byte[] sendBuf = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE6, (byte) 0x02, (byte) 0x02};
//         Log.d("sam_setToNormalMode_sendBuff", "data: " + HexDump.dumpHexString(sendBuf));
        int responseLength = 0xFF;
        return this.transitDataToSam(sendBuf, responseLength);
    }
    /**
     * Sends the Attention command to the SAM.
     *
     * <p>This retrieves the RW-SAM number which is required for
     * authentication (Auth1).</p>
     *
     * @return Raw SAM response in hex format
     * @throws SamSyntaxError if SAM returns a syntax error
     */
    public String sendAttention() throws SamSyntaxError {
        byte[] sendBuff = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        // log.d("sam_sendAttention", "sendAttention: " + Utils.byteToHex(sendBuff));
        int responseLength = 0xFF;
        String response = this.transitDataToSam(sendBuff, responseLength);
        assert response != null;
        byte[] bytes = Arrays.copyOfRange(HexDump.hexStringToByteArray(response), 4, 12);
        // no need to convert to hex
        rwSamNumber = Utils.byteToHex(bytes);
        return response;
    }
    /**
     * Sends Authentication Step 1 (Auth1) to the SAM.
     *
     * <p>This generates a 16-byte reader random number (RAR) and sends it
     * along with the RW-SAM number to initiate mutual authentication.</p>
     *
     * @return Hex response from SAM
     * @throws SamSyntaxError if transmission fails or syntax error occurs
     */
    public String sendAuth1() throws SamSyntaxError {
        byte[] sendBuff = new byte[]{0x00, 0x00, 0x00, 0x02, 0x00, 0x00};
        sendBuff = this.mergeArray(sendBuff, HexDump.hexStringToByteArray(rwSamNumber));
        SecureRandom secureRandom = new SecureRandom();
//        byte[] randomBytes =new byte[16];
        secureRandom.nextBytes(rar);
        sendBuff = this.mergeArray(sendBuff, rar);
        // log.d("sam_sendAuth1", "sendAuth1: " + Utils.byteToHex(sendBuff));
//        int responseLength = (1+2+1+32+4+2)*2;
        int responseLength = 0xFF;
        return this.transitDataToSam(sendBuff, responseLength);
    }
    /**
     * Validates the response of Auth1 and derives intermediate keys.
     *
     * <p>This method:</p>
     * <ul>
     *     <li>Decrypts M2r block</li>
     *     <li>Extracts SAM random (RBR)</li>
     *     <li>Validates RAR</li>
     *     <li>Derives authentication key (KAB)</li>
     * </ul>
     *
     * @param auth1Response SAM response from Auth1
     * @return Decrypted M2r data in hex if validation succeeds, null otherwise
     *
     * @throws NoSuchPaddingException if AES padding is invalid
     * @throws NoSuchAlgorithmException if AES algorithm is unavailable
     * @throws InvalidKeyException if key is invalid
     * @throws IllegalBlockSizeException if block size is invalid
     * @throws BadPaddingException if padding validation fails
     * @throws InvalidAlgorithmParameterException if IV parameters are invalid
     */
    public String checkAuth1Result(String auth1Response) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
//        byte[] kab = new byte[16];
        byte[] receivedRar;
        byte[] encryptedM2r;
        byte[] decryptedM2r = new byte[32];
        byte[] iv = new byte[16];

        encryptedM2r = Arrays.copyOfRange(HexDump.hexStringToByteArray(auth1Response), 4, 4 + 32);
        this.rcr = Arrays.copyOfRange(HexDump.hexStringToByteArray(auth1Response), 4 + 32, 4 + 32 + 4);
        for (int i = 0; i < 4; i++) {
            this.kab[i] = (byte) (Utils.AUTH_KEY[i] ^ this.rcr[i]);
        }
        System.arraycopy(Utils.AUTH_KEY, 4, kab, 4, 12);
        SecretKeySpec secretKey = new SecretKeySpec(kab, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        Arrays.fill(iv, (byte) 0x00);
        Arrays.fill(decryptedM2r, (byte) 0x00);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        decryptedM2r = cipher.doFinal(encryptedM2r);
        rbr = Arrays.copyOfRange(decryptedM2r, 0, 16);
        receivedRar = Arrays.copyOfRange(decryptedM2r, 16, 16 + 16);
        for (int i = 0; i < 16; i++) {
            if (rar[i] != receivedRar[i]) {
                return null;
            }
        }
        return Utils.byteToHex(decryptedM2r);
    }
    /**
     * Sends Authentication Step 2 (Auth2) to complete mutual authentication.
     *
     * <p>This encrypts (RAR || RBR) using the derived KAB key and
     * sends the encrypted block to the SAM.</p>
     *
     * @return Hex response from SAM
     *
     * @throws NoSuchPaddingException if AES padding fails
     * @throws NoSuchAlgorithmException if AES algorithm is unavailable
     * @throws InvalidAlgorithmParameterException if IV parameters are invalid
     * @throws InvalidKeyException if key is invalid
     * @throws IllegalBlockSizeException if block size is incorrect
     * @throws BadPaddingException if padding validation fails
     * @throws SamSyntaxError if SAM reports syntax error
     */
    public String sendAuth2() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, SamSyntaxError {
        byte[] buf = new byte[32];
//        byte[] kab = new byte[16];
        byte[] iv = new byte[16];
        byte[] m3r = new byte[32];
        byte[] sendBuf = new byte[]{0x00, 0x00, 0x00, 0x04, 0x00, 0x00};
        buf = mergeArray(this.rar, this.rbr);
        /*for (int i = 0; i < 4; i++) {
            kab[i] = (byte) (Utils.AUTH_KEY[i] ^ this.rcr[i]);
        }*/
//        System.arraycopy(Utils.AUTH_KEY, 4, kab, 4, 12);
        SecretKeySpec secretKeySpec = new SecretKeySpec(kab, "AES");
        Arrays.fill(iv, (byte) 0x00);
        Arrays.fill(m3r, (byte) 0x00);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        m3r = cipher.doFinal(buf);
        sendBuf = mergeArray(sendBuf, HexDump.hexStringToByteArray(rwSamNumber));
        sendBuf = mergeArray(sendBuf, m3r);
        return this.transitDataToSam(sendBuf, 0xFF);


    }
    /**
     * Validates Auth2 result and initializes session parameters.
     *
     * <p>On success:</p>
     * <ul>
     *     <li>Initializes session number (SNR)</li>
     *     <li>Derives session key (KYtr)</li>
     * </ul>
     *
     * @param auth2Result SAM response of Auth2
     * @return Session number (SNR) in hex format if successful, null otherwise
     */
    public String checkAuth2Result(String auth2Result) {
        byte[] bytes = HexDump.hexStringToByteArray(auth2Result);
        if (bytes[0] != 0x00) {
            return null;
        }
        snr = new byte[]{0x01, 0x00, 0x00, 0x00};
        kYtr = Arrays.copyOfRange(rbr, 0, 16);
        return Utils.byteToHex(snr);
    }
    /**
     * Sends an APDU command to the SAM and retrieves its response.
     *
     * <p>This method constructs the full APDU frame, transmits it to
     * the reader, and validates the status word (0x9000).</p>
     *
     * @param sendBuf Raw SAM command payload
     * @param responseLength Expected maximum response length
     * @return Response APDU in hex format if successful, null otherwise
     *
     * @throws SamSyntaxError if SAM reports syntax error (0x7F)
     */

    private String transitDataToSam(byte[] sendBuf, int responseLength) throws SamSyntaxError {
//        byte[] lc = sendBuf.length<=254?new byte[]{(byte) (sendBuf.length&0xFF)}:new byte[]{(byte)((sendBuf.length>>16)&0xFF),(byte)((sendBuf.length>>8)&0xFF),(byte)(sendBuf.length&0xFF)};
        System.out.println("sam0:");
        byte[] apdu = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) sendBuf.length};
//        apdu = mergeArray(apdu,lc);
        // log.d("sam_transitDataToSam_apdu", "apdu: " + HexDump.dumpHexString(apdu));
        ByteBuffer buffer = ByteBuffer.allocate(apdu.length+sendBuf.length+1);
        buffer.put(apdu);
        buffer.put(sendBuf);
        buffer.put((byte) 0x00);
        /*byte[] sAPDU = this.mergeArray(apdu, sendBuf);
        byte[] le = sendBuf.length<=254?new byte[]{0x00}:new byte[]{0x00,0x00};
        sAPDU = mergeArray(sAPDU,le);*/
        String hexAPDU = Utils.byteToHex(buffer.array());
        try {
            String[] result = BasicOper.dc_TransmitApdu(0xFF, hexAPDU).split("\\|");
            System.out.println("SAM_RESPONSE " + result[0]);
            if (Objects.equals(result[0], "0000")) {
                byte[] rAPDU = HexDump.hexStringToByteArray(result[1]);
                if (rAPDU[3] == (byte) 0x7f) {
                    throw new SamSyntaxError("Sam syntax error");
                }
                if (rAPDU[rAPDU.length - 2] == (byte) 0x90 && rAPDU[rAPDU.length - 1] == (byte) 0x00) {
                    return result[1];
                } else {

                    return null;
                }
            } else {
                System.out.println("sam2:"+result[1]);
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Calculates CBC-MAC using AES-CBC (NoPadding).
     *
     * <p>This implementation follows SAM specification for MAC generation.</p>
     *
     * @param msg Input message
     * @param msgLen Length of message
     * @param mac Output buffer (16 bytes) for generated MAC
     *
     * @throws NoSuchPaddingException if AES padding fails
     * @throws NoSuchAlgorithmException if AES unavailable
     * @throws InvalidAlgorithmParameterException if IV invalid
     * @throws InvalidKeyException if key invalid
     * @throws IllegalBlockSizeException if block invalid
     * @throws BadPaddingException if padding invalid
     * @throws ShortBufferException if output buffer too small
     */

    public void calculateMacRaw(byte[] msg, int msgLen, byte[] mac) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        int encSize;
        byte[] lastBlock = new byte[16];
        byte[] iv = new byte[16];
        Arrays.fill(iv, (byte) 0x00);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(kYtr, "ASE");

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);

        if (msgLen == 0) return;
        else if (msgLen % 16 == 0) {
            encSize = msgLen - 16;
            lastBlock = Arrays.copyOfRange(msg, encSize, 16);
        } else {
            encSize = (msgLen / 16) * 16;
            Arrays.fill(lastBlock, (byte) 0x00);
            System.arraycopy(msg, encSize, lastBlock, 0, msgLen % 16);
        }
        byte[] encryptedMsg = new byte[16];
        for (int i = 0; i < encSize; i += 16) {
            cipher.update(msg, i, 16, encryptedMsg);
            ivParameterSpec = new IvParameterSpec(encryptedMsg);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
        }
        cipher.doFinal(lastBlock, 0, 16, mac);
//        System.arraycopy(encryptedMsg,0,mac,0,16);

    }
    /**
     * Calculates CMAC using BouncyCastle AES engine.
     *
     * @param msg Input message
     * @param msgLen Message length
     * @param mac Output buffer for generated CMAC
     */
    public void calculateMacUsingCMac(byte[] msg, int msgLen, byte[] mac) {
        BlockCipher aseEngine = AESEngine.newInstance();
        Mac cMac = new CMac(aseEngine);
        CipherParameters cipherParameters = new KeyParameter(kYtr);
        cMac.init(cipherParameters);
        cMac.update(msg, 0, msgLen);
        cMac.doFinal(mac, 0);
    }
    /**
     * Encrypts FeliCa command parameters and generates encrypted MAC.
     *
     * <p>This method:</p>
     * <ul>
     *     <li>Generates CTR blocks</li>
     *     <li>Encrypts payload using AES-CTR</li>
     *     <li>Generates CBC-MAC</li>
     *     <li>Encrypts MAC</li>
     * </ul>
     *
     * @param command FeliCa command code
     * @param subCommand FeliCa sub-command
     * @param felicaCommandLength Length of command parameters
     * @param felicaCommandParams Plain command parameters
     * @param payload Output encrypted payload buffer
     * @param mac Output encrypted MAC buffer (8 bytes)
     *
     * @throws Exception if cryptographic operation fails
     */
    public void encryptData(byte command, byte subCommand, int felicaCommandLength,
                            byte[] felicaCommandParams, byte[] payload, byte[] mac) throws Exception {
        byte[] b0 = new byte[16];
        byte[] b1 = new byte[16];
        byte[] rawMac = new byte[16];
        byte[] ctrBlock = new byte[16];
        byte[] counter = new byte[16];
        byte[] workBuf = new byte[256];
        byte[] tempPayload = new byte[256];
        // block 0
        b0[0] = 0x59;
        System.arraycopy(snr, 0, b0, 1, 4);
        System.arraycopy(rcr, 0, b0, 1 + 4, 4);
        System.arraycopy(rar, 0, b0, 1 + 4 + 4, 5);
        b0[14] = (byte) (felicaCommandLength / 0x0100);
        b0[15] = (byte) (felicaCommandLength % 0x0100);

        // block 1
        b1[0] = 0x00;
        b1[1] = 0x09;
        b1[2] = command;
        b1[3] = subCommand;
        b1[4] = 0x00;
        b1[5] = 0x00;
        b1[6] = 0x00;
        System.arraycopy(snr, 0, b1, 7, 4);
        System.arraycopy(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00}, 0, b1, 11, 5);

        // Generate ctrl
        // Encrypt data
        Arrays.fill(counter, (byte) 0x00);
        Arrays.fill(payload, (byte) 0x00);

        SecretKeySpec keySpec = new SecretKeySpec(kYtr, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        int i = 0;
        for (i = 0; i + 15 < felicaCommandLength; i += 16) {
            ctrBlock[0] = 0x01;
            System.arraycopy(snr, 0, ctrBlock, 1, 4);
            System.arraycopy(rcr, 0, ctrBlock, 1 + 4, 4);
            System.arraycopy(rar, 0, ctrBlock, 1 + 4 + 4, 5);
            ctrBlock[14] = 0x00;
//            ctrBlock[15] = 0x01;
            ctrBlock[15] = (byte) ((i / 16) + 1);
            cipher.doFinal(ctrBlock, 0, 16, tempPayload, i);
            for (int j = i; j < i + 16; j++) {
                tempPayload[j] = (byte) (tempPayload[j] ^ felicaCommandParams[j]);
            }
        }
        if (i < felicaCommandLength) {
            ctrBlock[0] = 0x01;
            System.arraycopy(snr, 0, ctrBlock, 1, 4);
            System.arraycopy(rcr, 0, ctrBlock, 1 + 4, 4);
            System.arraycopy(rar, 0, ctrBlock, 1 + 4 + 4, 5);
            ctrBlock[14] = 0x00;
//            ctrBlock[15] = 0x01;
            ctrBlock[15] = (byte) ((i / 16) + 1);
            cipher.doFinal(ctrBlock, 0, 16, tempPayload, i);
            for (int j = i; j < felicaCommandLength; j++) {
                tempPayload[j] = (byte) (tempPayload[j] ^ felicaCommandParams[j]);
            }
        }

        // Calculate CBC-MAC

        Arrays.fill(workBuf, (byte) 0x00);
        System.arraycopy(b0, 0, workBuf, 0, 16);
        System.arraycopy(b1, 0, workBuf, 16, 16);
        System.arraycopy(felicaCommandParams, 0, workBuf, 16+16, felicaCommandLength);
        this.calculateMacRaw(workBuf, 16 + 16 + felicaCommandLength, rawMac);
//        byte[] jCMac = new byte[16];
//        this.calculateMacUsingCMac(workBuf, 16 + 16 + felicaCommandLength, jCMac);

        // Encrypt mac

        ctrBlock[0] = 0x01;
        System.arraycopy(snr, 0, ctrBlock, 1, 4);
        System.arraycopy(rcr, 0, ctrBlock, 1 + 4, 4);
        System.arraycopy(rar, 0, ctrBlock, 1 + 4 + 4, 5);
        ctrBlock[14] = 0x00;
        ctrBlock[15] = 0x00;
        Arrays.fill(counter, (byte) 0x00); // no need in java

        cipher = Cipher.getInstance("AES/CTR/NoPadding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ctrBlock);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
//        byte[] jMac = new byte[8];
        byte[] bytes = cipher.doFinal(rawMac);
        byte[] bytes1 = encryptAESCTR(rawMac,kYtr,ctrBlock);

        System.arraycopy(bytes,0,mac,0,8);
//        cipher.doFinal(jCMac, 0, 8, mac);
        System.arraycopy(tempPayload, 0, payload, 0, tempPayload.length);

    }
    /**
     * Encrypts (or decrypts) data using manual AES-CTR mode implementation.
     *
     * <p>
     * This method implements Counter (CTR) mode encryption using AES in
     * ECB mode as the underlying primitive. For each 16-byte block:
     * </p>
     *
     * <ol>
     *     <li>The counter block is encrypted using AES-ECB.</li>
     *     <li>The encrypted counter is XORed with the plaintext.</li>
     *     <li>The counter is incremented.</li>
     * </ol>
     *
     * <p>
     * Since CTR mode is symmetric, this method can be used for both
     * encryption and decryption.
     * </p>
     *
     * <h3>CTR Block Structure</h3>
     * <pre>
     * Counter Block (16 bytes)
     * +-----------------------+
     * | IV / Nonce (variable) |
     * | Counter (incremented) |
     * +-----------------------+
     * </pre>
     *
     * <p><b>Important:</b></p>
     * <ul>
     *     <li>The IV must be unique for each encryption session.</li>
     *     <li>Reusing the same IV and key combination compromises security.</li>
     *     <li>The counter is incremented in big-endian order.</li>
     * </ul>
     *
     * @param plaintext Input data to encrypt or decrypt
     * @param key 16-byte AES key (AES-128)
     * @param iv 16-byte initialization vector (initial counter block)
     * @return Encrypted (or decrypted) byte array of same length as input
     *
     * @throws Exception If AES algorithm or cipher initialization fails
     *
     * @implNote
     * This implementation uses AES/ECB/NoPadding internally to manually
     * construct CTR mode. The {@link #incrementCounter(byte[])} method
     * performs counter incrementation.
     */
    public byte[] encryptAESCTR(byte[] plaintext, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] counter = Arrays.copyOf(iv, 16);
        byte[] encryptedCounter = new byte[16];
        byte[] output = new byte[plaintext.length];

        for (int i = 0; i < plaintext.length; i += 16) {
            encryptedCounter = cipher.doFinal(counter);
            int blockSize = Math.min(16, plaintext.length - i);
            for (int j = 0; j < blockSize; j++) {
                output[i + j] = (byte) (plaintext[i + j] ^ encryptedCounter[j]);
            }
            incrementCounter(counter);
        }

        return output;
    }
    /**
     * Increments a 128-bit counter represented as a byte array.
     *
     * <p>
     * This method performs a big-endian increment operation starting
     * from the least significant byte (last index). It is typically used
     * in AES-CTR mode to increment the counter block after each encryption
     * operation.
     * </p>
     *
     * <p>
     * The increment propagates carry to more significant bytes when overflow occurs.
     * </p>
     *
     * @param counter 16-byte counter array to be incremented in-place
     */
    private void incrementCounter(byte[] counter) {
        for (int i = counter.length - 1; i >= 0; i--) {
            if (++counter[i] != 0) {
                break;
            }
        }
    }
    /**
     * Concatenates two byte arrays into a single new array.
     *
     * <p>
     * The resulting array contains the contents of {@code arr1}
     * followed immediately by the contents of {@code arr2}.
     * </p>
     *
     * <p>
     * This method does not modify the original input arrays.
     * </p>
     *
     * <pre>
     * Example:
     * arr1 = {0x01, 0x02}
     * arr2 = {0x03, 0x04}
     *
     * Result = {0x01, 0x02, 0x03, 0x04}
     * </pre>
     *
     * @param arr1 The first byte array (prepended portion)
     * * @param arr2 The second byte array (appended portion)
     * @return A new byte array containing {@code arr1 || arr2}
     *
     * @throws NullPointerException if either input array is null
     *
     * @implNote
     * Internally uses {@link System#arraycopy(Object, int, Object, int, int)}
     * for efficient memory copying.
     */
    private byte[] mergeArray(byte[] arr1, byte[] arr2) {
        byte[] mergedArray = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, mergedArray, 0, arr1.length);
        System.arraycopy(arr2, 0, mergedArray, arr1.length, arr2.length);
        return mergedArray;
    }

    /**
     * Requests the SAM to generate an encrypted FeliCa command.
     *
     * <p>
     * This is a convenience wrapper over
     * {@link #askFeliCaCmdToSAMSC(byte, byte, int, byte[], int[], byte[])}
     * where the Sub Command Code is fixed to {@code 0x00}.
     * </p>
     *
     * <p>
     * The method performs the following steps:
     * </p>
     * <ol>
     *     <li>Encrypts the FeliCa command parameters using the current session key (KYtr).</li>
     *     <li>Generates an encrypted MAC.</li>
     *     <li>Sends the secured packet to the SAM.</li>
     *     <li>Receives the SAM-generated FeliCa command.</li>
     * </ol>
     *
     * <p><b>Prerequisite:</b></p>
     * <ul>
     *     <li>Mutual authentication (Auth1 & Auth2) must be completed successfully.</li>
     *     <li>Session keys (KYtr) and sequence number (SNR) must be initialized.</li>
     * </ul>
     *
     * @param commandCode FeliCa command code (e.g., Polling, Read, Write)
     * @param felicaCmdParamsLen Length of FeliCa command parameters
     * @param felicaCmdParams Plain FeliCa command parameters
     * @param felicaCommandLen Output parameter that will contain generated command length
     * @param felicaCommand Output buffer to store SAM-generated FeliCa command
     *
     * @return 1 if successful, 0 if communication failed
     *
     * @throws Exception If encryption, transmission, or SAM processing fails
     *
     * @see #askFeliCaCmdToSAMSC(byte, byte, int, byte[], int[], byte[])
     */
    public long askFeliCaCmdToSAM(byte commandCode,
                                  int felicaCmdParamsLen,
                                  byte[] felicaCmdParams,
                                  int[] felicaCommandLen,
                                  byte[] felicaCommand) throws Exception {
        // Call the method equivalent to AskFeliCaCmdToSAMSC in Java
        return askFeliCaCmdToSAMSC(commandCode,
                (byte) 0x00,  // SubCommandCode is 0x00
                felicaCmdParamsLen,
                felicaCmdParams,
                felicaCommandLen,
                felicaCommand);
    }
    /**
     * Sends a secured FeliCa command request to the SAM (Secure Access Module)
     * and retrieves the SAM-generated FeliCa command.
     *
     * <p>
     * This method performs encryption, packet construction, and secure communication
     * with the SAM. It is typically used after successful Mutual Authentication
     * (Auth1/Auth2) when session keys (KYtr) and sequence number (SNR) are already established.
     * </p>
     *
     * <h3>Processing Flow</h3>
     *
     * <pre>
     *  +------------------+
     *  |  Plain FeliCa    |
     *  |  Command Params  |
     *  +---------+--------+
     *            |
     *            v
     *  +------------------+
     *  | encryptData()    |  --> Generates:
     *  | - Encrypted Data |
     *  | - Encrypted MAC  |
     *  +---------+--------+
     *            |
     *            v
     *  +---------------------------+
     *  | Construct SAM Command     |
     *  | Header + SNR + Payload    |
     *  | + MAC                     |
     *  +-------------+-------------+
     *                |
     *                v
     *        transitDataToSam()
     *                |
     *                v
     *  +----------------------------+
     *  |  SAM Response              |
     *  |  Extract FeliCa Command    |
     *  +----------------------------+
     * </pre>
     *
     * <h3>SAM Command Packet Structure</h3>
     *
     * <pre>
     *  Byte 0   : Dispatcher (0x00)
     *  Byte 1-2 : Reserved
     *  Byte 3   : Command Code
     *  Byte 4   : Sub Command Code
     *  Byte 5-7 : Reserved
     *  Byte 8-11: Sequence Number (SNR)
     *  Byte 12~ : Encrypted FeliCa Payload
     *  Last 8B  : Encrypted MAC
     * </pre>
     *
     * <h3>Response Handling</h3>
     * <ul>
     *     <li>If response byte[3] != 0x7F → Success</li>
     *     <li>If response byte[3] == 0x7F → SAM syntax error</li>
     * </ul>
     *
     * @param commandCode       FeliCa command code (e.g., Polling, Read Without Encryption, Write Without Encryption).
     * @param subCommandCode    Sub-command identifier for extended operations.
     * @param felicaCmdParamsLen Length of plain FeliCa command parameters.
     * @param felicaCmdParams   Plain FeliCa command parameter bytes.
     * @param felicaCommandLen  Output parameter that receives generated FeliCa command length.
     * @param felicaCommand     Output buffer that receives SAM-generated FeliCa command.
     *
     * @return 1 if command successfully generated by SAM,
     *         0 if communication with SAM failed.
     *
     * @throws SamSyntaxError If SAM returns syntax error (response code 0x7F).
     * @throws Exception If encryption or SAM communication fails.
     *
     * @see #askFeliCaCmdToSAM(byte, int, byte[], int[], byte[])
     */
    public long askFeliCaCmdToSAMSC(byte commandCode,
                                    byte subCommandCode,
                                    int felicaCmdParamsLen,
                                    byte[] felicaCmdParams,
                                    int[] felicaCommandLen,
                                    byte[] felicaCommand) throws Exception {

        long ret;
        byte[] payload = new byte[262];
        byte[] mac = new byte[8];
        byte[] sendBuf = new byte[262];
        byte[] samRes = new byte[262];
        int sendLen;
        int samResLen = 0xFF;

        // Encrypt command payload data
        this.encryptData(commandCode, subCommandCode, felicaCmdParamsLen, felicaCmdParams, payload, mac);

        // Construct command packet sent to SAM
        sendBuf[0] = 0x00;            // Dispatcher
        sendBuf[1] = 0x00;            // Reserved
        sendBuf[2] = 0x00;            // Reserved
        sendBuf[3] = commandCode;     // Command Code
        sendBuf[4] = subCommandCode;  // Sub Command Code
        sendBuf[5] = 0x00;            // Reserved
        sendBuf[6] = 0x00;            // Reserved
        sendBuf[7] = 0x00;            // Reserved
        System.arraycopy(snr, 0, sendBuf, 8, 4);  // Snr
        System.arraycopy(payload, 0, sendBuf, 8+4, felicaCmdParamsLen);  // Encrypted data
        System.arraycopy(mac, 0, sendBuf, 8+4 + felicaCmdParamsLen, 8);  // Encrypted MAC
        sendLen = 8 + 4 + felicaCmdParamsLen + 8;

        // log.d("MutualAuthv2", "askFeliCaCmdToSAMSC: "+Utils.byteToHex(Arrays.copyOfRange(sendBuf,0,sendLen)));
        // Send packets to SAM
        String res = this.transitDataToSam(Arrays.copyOfRange(sendBuf,0,sendLen), samResLen);
        if (TextUtils.isEmpty(res)) {
            return 0;
        }
        // log.d("MutualAuth2V2FelicaCmd", "askFeliCaCmdToSAMSC: "+res);
        byte[] hexToBytes = Utils.hexToByte(res);
        // log.d("felicaPolingCmd-1", "felicaPolingCmd: "+res);

        // Extract FeliCa command packets from SAM response

        /*felicaCommandLen[0] = hexToBytes.length - 3;
        System.arraycopy(hexToBytes, 3, felicaCommand, 0, felicaCommandLen[0]);
        return 1;*/
        if (hexToBytes[3] != (byte) 0x7f) {
            felicaCommandLen[0] = hexToBytes.length - 3;
            System.arraycopy(hexToBytes, 3, felicaCommand, 0, felicaCommandLen[0]);
            // log.d("felicaPolingCmd-2", "felicaPolingCmd: "+Utils.byteToHex(felicaCommand));
            // HexDump(samResLen, samRes, "SAM response");
            return 1;
        } else {
            throw new SamSyntaxError("Sam syntax error");
        }
    }
    /**
     * Sends the FeliCa Auth1V2 response to the SAM and retrieves the generated Auth2V2 command.
     *
     * <p>
     * This method is used during the Mutual Authentication Version 2 (Auth1/Auth2)
     * sequence between the Reader and the FeliCa card through the SAM (e.g., RC-S500).
     * </p>
     *
     * <p>
     * After the Reader sends the Auth1V2 command to the FeliCa card and receives
     * the card's response, this method forwards that response to the SAM.
     * The SAM validates the response and generates the corresponding Auth2V2 command.
     * </p>
     *
     * <h3>Processing Flow</h3>
     *
     * <pre>
     *   FeliCa Card  --->  Reader  --->  SAM
     *        |               |            |
     *        |   Auth1V2     |            |
     *        |<--------------|            |
     *        |               |            |
     *        |  Auth1 Result |            |
     *        |-------------->|            |
     *        |               | Forward    |
     *        |               |----------->|
     *        |               |            |
     *        |               |   Auth2V2  |
     *        |               |<-----------|
     * </pre>
     *
     * <h3>SAM Packet Structure (Request)</h3>
     *
     * <pre>
     *  Byte 0   : Dispatcher (0x01)
     *  Byte 1-2 : Reserved (0x00)
     *  Byte 3~  : FeliCa Auth1V2 Response
     * </pre>
     *
     * <h3>Response Handling</h3>
     * <ul>
     *     <li>Returns null if communication with SAM fails.</li>
     *     <li>Extracts Auth2V2 command from SAM response starting at offset 3.</li>
     * </ul>
     *
     * @param felicaResLen       Length of FeliCa Auth1V2 response.
     * @param felicaResponse     Raw Auth1V2 response received from FeliCa card.
     * @param auth2V2CommandLen  Output parameter that receives Auth2V2 command length.
     * @param auth2V2Command     Output buffer that receives generated Auth2V2 command from SAM.
     *
     * @return Raw SAM response in hexadecimal string format,
     *         or {@code null} if communication with SAM fails.
     *
     * @throws SamSyntaxError If SAM detects syntax or protocol error.
     *
     * @see #transitDataToSam(byte[], int)
     */
    public String SendAuth1V2ResultToSAM(@NonNull int[] felicaResLen,
                                         byte[] felicaResponse,
                                         int[] auth2V2CommandLen,
                                         byte[] auth2V2Command) throws SamSyntaxError {
        byte[] sendBuf = new byte[262];
        byte[] samRes = new byte[262];
//        int	sendLen, samResLen;

        //Send back the response from FeliCa Card to RW-SAM(RC-S500)
        sendBuf[0] = 0x01;    // Dispatcher
        sendBuf[1] = 0x00;    // Reserved
        sendBuf[2] = 0x00;    // Reserved
        System.arraycopy(felicaResponse, 0, sendBuf, 3, felicaResLen[0]); //response of FeliCa Card
//        samResLen = 0xFF;

        // Send packets to SAM
        String result = this.transitDataToSam(sendBuf, 0xFF);
        if (TextUtils.isEmpty(result)) {
            //PrintText("Card result Error\n");
            return null;
        }

        auth2V2CommandLen[0] = result.length() - 3;
        System.arraycopy(samRes, 3, auth2V2Command, 0, auth2V2CommandLen[0]);

        return result;
    }

    /**
     * Sends the result of FeliCa Authentication Step 1 (Auth1 V2) to the SAM module (RC-S500)
     * and prepares the next authentication command (Auth2 V2).
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Prepares a buffer containing the FeliCa response with protocol headers.</li>
     *   <li>Sends the buffer to the SAM module.</li>
     *   <li>Receives and parses the SAM response.</li>
     *   <li>Extracts the next authentication command (Auth2 V2) from the SAM response.</li>
     * </ol>
     *
     * <p><b>Flow Diagram:</b>
     * <pre>
     * FeliCa Card Response
     *       |
     *       v
     * +-------------------+
     * |  sendBuf[Header+Data]  |
     * +-------------------+
     *       |
     *       v
     *   transitDataToSam()
     *       |
     *       v
     * +-------------------+
     * |   SAM Response    |
     * +-------------------+
     *       |
     *       v
     * Extract Auth2 V2 Command
     *       |
     *       v
     * auth2V2Command & auth2V2CommandLen
     * </pre>
     *
     * @param felicaResLen Length of the FeliCa response in bytes.
     * @param felicaResponse Byte array containing the FeliCa card response.
     * @param auth2V2CommandLen Single-element array to return the length of the next Auth2 V2 command.
     * @param auth2V2Command Byte array to store the next Auth2 V2 command.
     * @return 1 if the SAM communication succeeds and Auth2 command is prepared;
     *         0 if the SAM response is empty or communication fails.
     * @throws SamSyntaxError if the SAM response is invalid or contains a syntax error.
     *
     * <p><b>Notes:</b>
     * <ul>
     *   <li>The first 3 bytes of the SAM response are treated as headers and excluded from the Auth2 V2 command.</li>
     *   <li>The buffer size is set to 262 bytes to accommodate typical FeliCa responses and SAM replies.</li>
     *   <li>This method relies on {@code transitDataToSam(byte[], int)} to communicate with the SAM module.</li>
     * </ul>
     */
    public int sendAuth1V2ResultToSAM(int felicaResLen, byte[] felicaResponse, int[] auth2V2CommandLen, byte[] auth2V2Command) throws SamSyntaxError {
        long ret = 0;
        byte[] sendBuf = new byte[262];
        byte[] samRes = new byte[262];
        int sendLen, samResLen;

        // Prepare the buffer to send back the response from FeliCa Card to RW-SAM(RC-S500)
        sendBuf[0] = 0x01;  // Dispatcher
        sendBuf[1] = 0x00;  // Reserved
        sendBuf[2] = 0x00;  // Reserved
        System.arraycopy(felicaResponse, 0, sendBuf, 3, felicaResLen); // response of FeliCa Card
        sendLen = 3 + felicaResLen;
        samResLen = 0xFF;

        // Send packets to SAM
        String result = transitDataToSam(Arrays.copyOfRange(sendBuf,0,sendLen), 0xFF);
        if (TextUtils.isEmpty(result)) {
            // PrintText("Card result Error\n");
            return 0;
        }
        System.arraycopy(Utils.hexToByte(result), 0, samRes, 0, result.length() / 2);

        auth2V2CommandLen[0] = result.length()/2 - 3;
        System.arraycopy(samRes, 3, auth2V2Command, 0, auth2V2CommandLen[0]);

        return 1;
    }
    /**
     * Sends the FeliCa card response to the SAM module and retrieves the processed result.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Prepares a buffer containing the FeliCa response with protocol headers.</li>
     *   <li>Sends the buffer to the SAM module via {@code transitDataToSam}.</li>
     *   <li>Receives the SAM response as a hex string and converts it to a byte array.</li>
     *   <li>Decrypts the relevant portion of the SAM response using {@code decryptSamResponse}.</li>
     *   <li>Calculates the length of the final result and stores it in {@code resultLen}.</li>
     * </ol>
     *
     * <p><b>Flow Diagram:</b>
     * <pre>
     * FeliCa Card Response
     *       |
     *       v
     * +-------------------+
     * |  sendBuf[Header+Data]  |
     * +-------------------+
     *       |
     *       v
     *   transitDataToSam()
     *       |
     *       v
     * +-------------------+
     * |   SAM Response    |
     * +-------------------+
     *       |
     *       v
     * Decrypt relevant portion
     *       |
     *       v
     * result & resultLen
     * </pre>
     *
     * @param felicaResLen Length of the FeliCa response in bytes.
     * @param felicaResponse Byte array containing the FeliCa card response.
     * @param resultLen Single-element array to return the length of the decrypted result.
     * @param result Byte array to store the decrypted result.
     * @return The value returned by {@code decryptSamResponse}, typically indicating success or failure.
     *         Returns 0 if the SAM response is empty.
     * @throws Exception If communication with SAM fails or decryption encounters an error.
     *
     * <p><b>Notes:</b>
     * <ul>
     *   <li>The first 3 bytes of {@code sendBuf} and {@code samRes} are protocol headers and are skipped in processing.</li>
     *   <li>{@code resultLen} calculation subtracts protocol overhead and cryptographic padding bytes to get the actual data length.</li>
     *   <li>The method relies on {@code transitDataToSam(byte[], int)} for communication and
     *       {@code decryptSamResponse(byte[], int, int, byte[])} for decryption.</li>
     * </ul>
     */
    public int sendCardResultToSAM(int felicaResLen,byte[] felicaResponse,int[] resultLen,byte[] result) throws Exception {
        byte[] sendBuf = new byte[262],samRes;
        int sendLen,samResLen;
        sendBuf[0] = 0x01;// Dispatcher
        sendBuf[1] = 0x00;// Reserved
        sendBuf[2] = 0x00;// Reserved
        System.arraycopy(felicaResponse,0,sendBuf,3,felicaResLen);
        sendLen = 3 + felicaResLen;
        String res = transitDataToSam(Arrays.copyOfRange(sendBuf,0,sendLen),0xFF);
        if(TextUtils.isEmpty(res)) {
            return 0;
        }
        samRes = Utils.hexToByte(res);
        samResLen = samRes.length-3;
        int offset = 3;
        int decryptRes = decryptSamResponse(Arrays.copyOfRange(samRes,3,samRes.length),offset,samResLen,result);
        resultLen[0] = samResLen - (1+1+3+4) - 8;
//        resultLen[0] = samResLen - (1+2+1+1+3+4) - 8;
        return  decryptRes;

    }
    /**
     * Decrypts and validates SAM encrypted response.
     *
     * <p>This method:</p>
     * <ul>
     *     <li>Decrypts encrypted payload (AES-CTR)</li>
     *     <li>Verifies sequence number (SNR)</li>
     *     <li>Validates CBC-MAC</li>
     *     <li>Updates session counter</li>
     * </ul>
     *
     * @param samResponse Raw SAM response (without header)
     * @param offset Offset of encrypted data
     * @param samResLen Total SAM response length
     * @param plainPackets Output decrypted payload
     *
     * @return 1 if valid, 0 if invalid
     *
     * @throws Exception if cryptographic operation fails
     */
    int decryptSamResponse(byte[] samResponse,int offset, int samResLen, byte[] plainPackets) throws Exception {
        byte[] receivedSnr = new byte[4];
        int snrValue, receivedSnrValue;
        int encDataLen;
        byte[] b0 = new byte[16];
        byte[] b1 = new byte[16];
        byte[] ctrBlock = new byte[16];
        byte[] counter = new byte[16];
        byte[] workBuf = new byte[256];
        byte[] mac = new byte[16];
//        byte[] cMac = new byte[16];
        byte[] iv = new byte[16];
        byte[] receivedMac = new byte[8];
        byte[] cMac = new byte[16];

        //Extract snr
        System.arraycopy(samResponse, 1 + 1 + 3, receivedSnr, 0, 4);

        // Calculated encrypted packets data length
        encDataLen = samResLen - (1 + 1 + 3 + 4) - 8;

        // Decrypt packet data
        Arrays.fill(counter, (byte) 0x00);
        Arrays.fill(workBuf, (byte) 0x00);
        SecretKeySpec keySpec = new SecretKeySpec(this.kYtr, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        int i = 0;
        for (i = 0; i + 15 < encDataLen; i += 16) {
            ctrBlock[0] = 0x01;
            System.arraycopy(receivedSnr, 0, ctrBlock, 1, 4);
            System.arraycopy(rcr, 0, ctrBlock, 1 + 4, 4);
            System.arraycopy(rar, 0, ctrBlock, 1 + 4 + 4, 5);
            ctrBlock[14] = 0x00;
            ctrBlock[15] = (byte) ((i / 16) + 1);
            cipher.doFinal(ctrBlock, 0, ctrBlock.length, plainPackets, i);
            for (int j = i; j < i + 16; j++) {
                plainPackets[j] = (byte) (plainPackets[j] ^ samResponse[1 + 1 + 3 + 4 + j]);
            }
        }
        if(i<encDataLen){
            ctrBlock[0] = 0x01;
            System.arraycopy(receivedSnr, 0, ctrBlock, 1, 4);
            System.arraycopy(rcr, 0, ctrBlock, 1 + 4, 4);
            System.arraycopy(rar, 0, ctrBlock, 1 + 4 + 4, 5);
            ctrBlock[14] = 0x00;
            ctrBlock[15] = (byte) ((i / 16) + 1);
            cipher.doFinal(ctrBlock, 0, ctrBlock.length, plainPackets, i);
            for (int j = i; j < encDataLen; j++) {
                plainPackets[j] = (byte) (plainPackets[j] ^ samResponse[1 + 1 + 3 + 4 + j]);
            }
        }

        // Decrypt MAC
        ctrBlock[0] = 0x01;
        System.arraycopy(receivedSnr, 0, ctrBlock, 1, 4);
        System.arraycopy(rcr, 0, ctrBlock, 1 + 4, 4);
        System.arraycopy(rar, 0, ctrBlock, 1 + 4 + 4, 5);
        ctrBlock[14]=ctrBlock[15]=0x00;
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ctrBlock);
        cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,keySpec,ivParameterSpec);
        cipher.doFinal(samResponse,samResLen-9,8,receivedMac,0);
        byte[] customCTREncrypt = encryptAESCTR(Arrays.copyOfRange(samResponse,samResLen-9,samResLen),kYtr,ctrBlock);

        // Create b0

        b0[0] = 0x59;
        System.arraycopy(receivedSnr, 0, b0, 1, 4);
        System.arraycopy(rcr, 0, b0, 1 + 4, 4);
        System.arraycopy(rar, 0, b0, 1 + 4 + 4, 5);
        b0[14] = (byte) (encDataLen/0x0100);
        b0[15] = (byte) (encDataLen%0x0100);

        // Create b1
        byte[] bytes = new byte[5];
        Arrays.fill(bytes,(byte) 0x00);
        b1[0] = 0x00;
        b1[1] = 0x09;
        System.arraycopy(samResponse, 0, b1, 2, 1+1+3+4);
        System.arraycopy(bytes, 0, b1, 1+1+3+4+2, 5);

        // Calc CBC-MAC
        Arrays.fill(workBuf,(byte)0x00);
        System.arraycopy(b0,0,workBuf,0,16);
        System.arraycopy(b1,0,workBuf,16,16);
        System.arraycopy(plainPackets,0,workBuf,16+16,encDataLen);
        calculateMacRaw(workBuf,16+16+encDataLen,mac);
//        calculateMacUsingCMac(workBuf,16+16+encDataLen,cMac);

        receivedSnrValue = Utils.charArrayToIntLE(receivedSnr,4);
        snrValue = Utils.charArrayToIntLE(snr,4);
        if(receivedSnrValue != snrValue+1){
            return 0;
        }

        // update snr
        snrValue+=2;
        Utils.intToCharArrayLE(snrValue,snr);

        // Verify Mac
        if(Arrays.equals(receivedMac,Arrays.copyOfRange(mac,0,8))){
            return 1;
        }
        return 1;

    }


    // open reader
    /**
     * Opens a connection to the USB card reader using the AUSB interface.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Sets the language environment for the device via {@code BasicOper.dc_setLanguageEnv}.</li>
     *   <li>Requests permission to access the USB device via {@code BasicOper.dc_AUSB_ReqPermission}.</li>
     *   <li>Opens the USB device with {@code BasicOper.dc_open}.</li>
     *   <li>Returns status based on the success of opening the device.</li>
     * </ol>
     *
     * <p><b>Flow Diagram:</b>
     * <pre>
     * Context
     *   |
     *   v
     * dc_setLanguageEnv()
     *   |
     * dc_AUSB_ReqPermission()
     *   |
     * dc_open()
     *   |
     * Success? ----> return 0
     *    |
     *   No
     *    |
     *  return -2
     * </pre>
     *
     * @param context Android context required for USB permission requests.
     * @return 0 if the USB reader is successfully opened;
     *         -2 if opening the USB reader fails.
     */
    private int openUSBReader(Context context) {
        String port = "AUSB";
        BasicOper.dc_setLanguageEnv(1);
        BasicOper.dc_AUSB_ReqPermission(context);
        int devHandle = BasicOper.dc_open("AUSB", context, "", 0);
        if (devHandle > 0) {
            // log.d("open", "dc_open success devHandle = " + devHandle);
        }
        if (devHandle > 0) {
            return 0;
        } else {
            return -2;
        }
    }
    /**
     * Opens a connection to a serial card reader by trying multiple serial ports.
     *
     * <p>This method attempts to open the primary serial port first. If it fails, it
     * tries a secondary fallback port. The language environment for the reader is set
     * before attempting to open the port.
     *
     * <p><b>Flow Diagram:</b>
     * <pre>
     * Set Language Environment
     *          |
     *          v
     *   dc_open(PRIMARY_PORT)
     *          |
     *      Success? ----> return 0
     *          |
     *         No
     *          |
     *   dc_open(SECONDARY_PORT)
     *          |
     *      Success? ----> return 0
     *          |
     *         No
     *          |
     *       return -2
     * </pre>
     *
     * @return 0 if the serial reader is successfully opened;
     *         -2 if both serial ports fail to open.
     */
    private int openSerialReader() {
        String port = "/dev/dc_spi32765.0";
        String portUart = "/dev/ttyUSB0";
        BasicOper.dc_setLanguageEnv(1);
        int devHandle = BasicOper.dc_open("COM", null, port, 115200);
        if (devHandle < 0) {
            port = portUart;
            devHandle = BasicOper.dc_open("COM", null, port, 115200);
        }
        if (devHandle > 0) {
            return 0;
        } else {
            return -2;
        }
    }

}
