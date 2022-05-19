package com.github.xemiru.mcbomberman.framework.exception;

public class StateCallbackException extends RuntimeException {

    public StateCallbackException(String message) {
        super(message);
    }

    public StateCallbackException(String message, Exception cause) {
        super(message, cause);
    }

}
