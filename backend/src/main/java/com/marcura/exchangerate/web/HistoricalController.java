package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.ExchangeRateService;
import com.marcura.exchangerate.web.api.HistoricalApi;
import com.marcura.exchangerate.web.dto.HistoryResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class HistoricalController implements HistoricalApi {

    private final ExchangeRateService exchangeRateService;

    @Override
    public HistoryResponse getHistory(String from, String to, LocalDate fromDate, LocalDate toDate) {
        validateCurrency(from);
        validateCurrency(to);
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        return exchangeRateService.historicalAdjusted(from, to, fromDate, toDate);
    }

    private void validateCurrency(String code) {
        if (code == null || !code.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
    }
}
