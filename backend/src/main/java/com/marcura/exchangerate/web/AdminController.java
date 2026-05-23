package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.RateIngestionService;
import com.marcura.exchangerate.web.api.AdminApi;
import com.marcura.exchangerate.web.dto.IngestionResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class AdminController implements AdminApi {

    private final RateIngestionService rateIngestionService;

    @Override
    public IngestionResponse refreshRates(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return new IngestionResponse(rateIngestionService.ingestLatest());
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both from and to are required for range backfill");
        }
        return new IngestionResponse(rateIngestionService.ingestRange(from, to));
    }
}
