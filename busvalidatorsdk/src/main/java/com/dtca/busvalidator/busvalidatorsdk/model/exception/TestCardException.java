package com.dtca.busvalidator.busvalidatorsdk.model.exception;


public class TestCardException extends Exception {

    public TestCardException() {
    }

    public TestCardException(String message) {
        super(message);
    }

    public TestCardException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestCardException(Throwable cause) {
        super(cause);
    }
}
