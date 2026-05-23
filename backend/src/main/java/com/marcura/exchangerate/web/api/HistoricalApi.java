package com.marcura.exchangerate.web.api;

import com.marcura.exchangerate.web.dto.HistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Exchange")
public interface HistoricalApi {

    @GetMapping("/exchange/history")
    @Operation(summary = "Spread-adjusted historical rates")
    HistoryResponse getHistory(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate);
}
