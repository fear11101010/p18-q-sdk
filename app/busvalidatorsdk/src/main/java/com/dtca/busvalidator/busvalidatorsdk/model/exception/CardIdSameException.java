package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class CardIdSameException extends Exception {

    public CardIdSameException() {
    }

    public CardIdSameException(String message) {
        super(message);
    }

    public CardIdSameException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardIdSameException(Throwable cause) {
        super(cause);
    }
}
