package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.TrendInsightService;
import com.marcura.exchangerate.web.api.InsightApi;
import com.marcura.exchangerate.web.dto.InsightResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class InsightController implements InsightApi {

    private final TrendInsightService trendInsightService;

    @Override
    public InsightResponse getInsight(String from, String to, LocalDate fromDate, LocalDate toDate) {
        validateCurrency(from);
        validateCurrency(to);
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        return trendInsightService.generateInsight(from, to, fromDate, toDate);
    }

    private void validateCurrency(String code) {
        if (code == null || !code.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
    }
}
