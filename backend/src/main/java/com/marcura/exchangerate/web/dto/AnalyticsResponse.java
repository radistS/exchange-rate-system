package com.marcura.exchangerate.web.dto;

import java.util.List;

public record AnalyticsResponse(List<CurrencyUsageDto> topCurrencies) {
}
