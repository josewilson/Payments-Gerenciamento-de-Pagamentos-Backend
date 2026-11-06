package com.josewilson.payments.exception;

public class CotacaoIndisponivelException extends RuntimeException {

    public CotacaoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }

    public CotacaoIndisponivelException(String message) {
        super(message);
    }
}
