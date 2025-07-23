package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class WrongServiceIdException extends Exception {

    public WrongServiceIdException() {
    }

    public WrongServiceIdException(String message) {
        super(message);
    }

    public WrongServiceIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongServiceIdException(Throwable cause) {
        super(cause);
    }
}
