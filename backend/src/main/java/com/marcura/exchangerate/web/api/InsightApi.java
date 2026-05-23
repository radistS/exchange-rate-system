package com.marcura.exchangerate.web.api;

import com.marcura.exchangerate.web.dto.InsightResponse;
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
public interface InsightApi {

    @GetMapping("/exchange/insight")
    @Operation(summary = "AI-generated trend insight for a period")
    InsightResponse getInsight(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate);
}
