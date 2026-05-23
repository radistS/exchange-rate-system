package com.marcura.exchangerate.web.api;

import com.marcura.exchangerate.web.dto.ExchangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Exchange")
public interface ExchangeApi {

    @GetMapping("/exchange")
    @Operation(summary = "Spread-adjusted exchange rate for a currency pair")
    @ApiResponse(responseCode = "200", description = "Rate found")
    @ApiResponse(responseCode = "404", description = "Rate not found for date")
    ExchangeResponse getExchange(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);
}
