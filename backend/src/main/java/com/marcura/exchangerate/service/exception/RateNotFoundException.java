package com.marcura.exchangerate.service.exception;

import java.time.LocalDate;

public class RateNotFoundException extends RuntimeException {

    public RateNotFoundException(String currency, LocalDate date) {
        super("No rate for currency %s on date %s".formatted(currency, date));
    }

    public RateNotFoundException(String message) {
        super(message);
    }
}
