package com.dtca.busvalidator.busvalidatorsdk.model;


import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;

public interface ReadWriteInCard {
    FelicaCard readData() throws Exception;
    int writeInCard(int serviceNum, byte[] serviceList, int blockNum, byte[] blockList, byte[] blockData) throws Exception;
}