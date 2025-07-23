package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CardUnavailableException extends Exception {

    public CardUnavailableException() {
    }

    public CardUnavailableException(String message) {
        super(message);
    }

    public CardUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardUnavailableException(Throwable cause) {
        super(cause);
    }
}
