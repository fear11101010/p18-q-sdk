package com.dtca.busvalidator.busvalidatorsdk.model;

import android.os.Build;

import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;
import com.dtca.busvalidator.busvalidatorsdk.helper.Utils;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardReadException;
import com.dtca.busvalidator.busvalidatorsdk.model.exception.CardWriteException;
import com.dtca.busvalidator.busvalidatorsdk.model.interfac.DataInterface;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlackList {

    private FelicaCard felicaCard;
    private AttributeInfo attributeInfo;

    public BlackList(FelicaCard felicaCard) throws CardReadException {
        try{
            this.felicaCard = felicaCard;
            this.attributeInfo = felicaCard.getFelicaCardDetail().getAttributeInfo();
        }catch (Exception e){
            throw new CardReadException("An error occur while reading card. Please try again later.");
        }
    }
    private void updateAttributeInfo(){
        byte cardControlCode = attributeInfo.getCardControlCode();
        cardControlCode = (byte) (cardControlCode | (1 << 7));
        attributeInfo.setCardControlCode(cardControlCode);
        int transactionId = Integer.parseInt(Utils.byteToHex(attributeInfo.getTxnDataId()),16)+1;
        attributeInfo.setTxnDataId(Utils.hexToByte(String.format("%04X",transactionId)));
    }

    public void writeData(DataInterface dataInterface) throws CardWriteException {
        updateAttributeInfo();

        byte[] serviceCode = new byte[4];
        int numOfService = 1;
        serviceCode[0] = 0x08;
        serviceCode[1] = 0x13;
        serviceCode[2] = 0x01;
        serviceCode[3] = 0x00;

        byte[] blockList = new byte[4];
        int numOfBlock = 2;
        blockList[0] = (byte) 0x80;
        blockList[1] = (byte) 0x00;
        blockList[2] = (byte) 0x80;
        blockList[3] = (byte) 0x01;

        try {
            int i = felicaCard.writeInCard(numOfService,serviceCode,numOfBlock,blockList, attributeInfo.getData());
            if(i!=1) throw new CardWriteException("An error occur while write in card. Please try again later.");
            TransactionData transactionData;
            transactionData = TransactionData.builder()
                    .cardId(Utils.byteToHex(felicaCard.getIdi()))
                    .recycleCounter(Utils.byteToHex(new byte[]{felicaCard.getFelicaCardDetail().getIssuerInfo().getRecycleCounter()}))
                    .transactionDataId(Utils.byteArrayToInt(attributeInfo.getTxnDataId()))
                    .serviceId("B001")
                    .dateTimeStamp(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))
                    .cardFunctionCode(Utils.byteArrayToInt(attributeInfo.getCardFunctionCode()))
                    .cardControlCode(attributeInfo.getCardControlCode() & 0XFF)
                    .discountCode(attributeInfo.getDiscountCode() & 0XFF)
                    .cardExpirationDate(Utils.byteToHex(attributeInfo.getExpiryDate()))
                    .processUnfinishedFlag(0x00)
                    .deviceSerialNumber(Utils.getDeviceSerialNo())
                    .svLogId(0x0000)
                    .svBalance(0x000000)
                    .svSpent(0x000000)
                    .processedLocation1("0000")
                    .processedLocation2("0000")

                    .statusFlag(0x0000)
                    .basicFareAmount(0x000000)
                    .distanceFareAmount(0x000000)
                    .discountFareAmount(0)
                    .inStoppage("0000")
                    .outStoppage("0000")
                    .negativeValue(0x0000)
                    .negativeValueUsed(0x0000)
                    .message("NA")
                    .build();
            dataInterface.receiveTransactionData(transactionData);
        } catch (Exception e) {
            throw new CardWriteException("An error occur while write in card. Please try again later.");
        }
    }
}
