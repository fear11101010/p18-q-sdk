package com.dtca.busvalidator.busvalidatorsdk.model.interfac;

import com.dtca.busvalidator.busvalidatorsdk.model.TransactionData;

public interface DataInterface {
    void receiveTransactionData(TransactionData transactionData);
}
