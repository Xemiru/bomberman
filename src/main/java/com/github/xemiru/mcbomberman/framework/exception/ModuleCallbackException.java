package com.github.xemiru.mcbomberman.framework.exception;

public class ModuleCallbackException extends RuntimeException {

    public ModuleCallbackException(String message) {
        super(message);
    }

    public ModuleCallbackException(String message, Exception cause) {
        super(message, cause);
    }

}
