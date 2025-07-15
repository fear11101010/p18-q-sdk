package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CardBlackListException extends Exception {

    public CardBlackListException() {
    }

    public CardBlackListException(String message) {
        super(message);
    }

    public CardBlackListException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardBlackListException(Throwable cause) {
        super(cause);
    }
}
