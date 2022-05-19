package com.github.xemiru.mcbomberman.framework.exception;

public class StateTransitionException extends RuntimeException {

    public StateTransitionException(String message) {
        super(message);
    }

    public StateTransitionException(String message, Exception cause) {
        super(message, cause);
    }

}
