package com.dtca.busvalidator.busvalidatorsdk;

import android.util.Log;

import com.decard.NDKMethod.BasicOper;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;

public class QRCodeReader {

        private static QRCodeReader qrCodeReader;

        private QRCodeReader(){}

        public static QRCodeReader getInstance(){
            if(qrCodeReader==null){
                qrCodeReader = new QRCodeReader();
            }
            return qrCodeReader;
        }

        public String startQrCodeScaner(){
            String[] result = BasicOper.dc_Scan2DBarcodeStart(0).split("\\|",-1);
            if(!result[0].equalsIgnoreCase("0000")){
                // throw exception
            }
            Log.d("qrcode_reader_start", "QRCodeReader: ");
            return null;
        }

        public String readQrCode(){
            String[] result = BasicOper.dc_Scan2DBarcodeGetData().split("\\|",-1);
            if(result[0].equalsIgnoreCase("0000")){
                return Utils.hexToString(result[1]);
            }
            return null;
        }
}
