package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.ExchangeRateService;
import com.marcura.exchangerate.web.api.ExchangeApi;
import com.marcura.exchangerate.web.dto.ExchangeResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class ExchangeController implements ExchangeApi {

    private final ExchangeRateService exchangeRateService;

    @Override
    public ExchangeResponse getExchange(String from, String to, LocalDate date) {
        validateCurrency(from);
        validateCurrency(to);
        return exchangeRateService.getExchange(from, to, date);
    }

    private void validateCurrency(String code) {
        if (code == null || !code.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
    }
}
