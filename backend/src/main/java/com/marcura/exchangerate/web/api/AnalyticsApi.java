package com.marcura.exchangerate.web.api;

import com.marcura.exchangerate.web.dto.AnalyticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Analytics")
public interface AnalyticsApi {

    @GetMapping("/analytics")
    @Operation(summary = "Currency query usage statistics")
    AnalyticsResponse getAnalytics();
}
