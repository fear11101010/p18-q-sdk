package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CardReadException extends Exception {

    public CardReadException() {
    }

    public CardReadException(String message) {
        super(message);
    }

    public CardReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardReadException(Throwable cause) {
        super(cause);
    }
}
