package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CardNotActiveException extends Exception {

    public CardNotActiveException() {
    }

    public CardNotActiveException(String message) {
        super(message);
    }

    public CardNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardNotActiveException(Throwable cause) {
        super(cause);
    }
}
