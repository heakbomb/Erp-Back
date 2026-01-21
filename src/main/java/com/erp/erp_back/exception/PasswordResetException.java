// src/main/java/com/erp/erp_back/exception/PasswordResetException.java
package com.erp.erp_back.exception;

public class PasswordResetException extends RuntimeException {
    public PasswordResetException(String message) {
        super(message);
    }
}