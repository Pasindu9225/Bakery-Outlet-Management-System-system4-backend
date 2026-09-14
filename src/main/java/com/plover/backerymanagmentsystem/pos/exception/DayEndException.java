package com.plover.backerymanagmentsystem.pos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DayEndException extends RuntimeException {
    public DayEndException(String message) {
        super(message);
    }
}
