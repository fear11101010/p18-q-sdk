package com.dtca.busvalidator.busvalidatorsdk;

import org.junit.Test;

import static org.junit.Assert.*;

import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;

import java.util.Locale;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void twosComplement() {

        byte[] results = Utils.convertToTwosComplementLE(-105,3);
        String bit = Utils.convertByteArrayToBit(Utils.reverseArray(results));
        System.out.println(bit);

        System.out.println(Utils.convertTwosComplementByteArrayToLittleIndian(results,3));
    }
    @Test
    public void twosComplement1() {

        byte[] results = Utils.convertShortToCharArray(10);
        String bit = Utils.convertByteArrayToBit(results);
        System.out.println(bit);
        byte[] result1 = Utils.convertToTwosComplementLE(-Integer.parseInt(Utils.byteToHex(results), 16), 3);
        System.out.println(Utils.convertTwosComplementByteArrayToLittleIndian(result1,3));
    }


    @Test
    public void intToHex() {

        int i = -128 & 0XFFFFFF;
        System.out.println(String.format(Locale.ENGLISH,"%06X",i));
    }


}