package com.marcura.exchangerate.web.dto;

import java.time.LocalDate;

public record CurrencyUsageDto(String currency, long totalCount, LocalDate lastQueried) {
}
