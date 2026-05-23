package com.marcura.exchangerate.web.api;

import com.marcura.exchangerate.web.dto.IngestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin")
public interface AdminApi {

    @PostMapping("/admin/refresh")
    @Operation(summary = "Trigger Fixer.io ingestion (latest or date range backfill)")
    IngestionResponse refreshRates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);
}
