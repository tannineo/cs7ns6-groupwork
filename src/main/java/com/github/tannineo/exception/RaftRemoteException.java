package com.github.tannineo.exception;


public class RaftRemoteException extends RuntimeException {

    public RaftRemoteException() {
        super();
    }

    public RaftRemoteException(String message) {
        super(message);
    }
}

