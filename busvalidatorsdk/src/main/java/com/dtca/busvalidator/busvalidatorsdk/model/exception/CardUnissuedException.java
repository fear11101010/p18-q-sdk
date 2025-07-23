package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CardUnissuedException extends Exception {

    public CardUnissuedException() {
    }

    public CardUnissuedException(String message) {
        super(message);
    }

    public CardUnissuedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardUnissuedException(Throwable cause) {
        super(cause);
    }
}
