package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class BalanceNotAvailableException extends Exception {

    public BalanceNotAvailableException() {
    }

    public BalanceNotAvailableException(String message) {
        super(message);
    }

    public BalanceNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public BalanceNotAvailableException(Throwable cause) {
        super(cause);
    }
}
