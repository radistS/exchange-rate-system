package com.marcura.exchangerate.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FixerResponse(
        boolean success,
        @JsonProperty("error") FixerError error,
        String base,
        LocalDate date,
        Map<String, BigDecimal> rates) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FixerError(Integer code, String type, String info) {
    }
}
