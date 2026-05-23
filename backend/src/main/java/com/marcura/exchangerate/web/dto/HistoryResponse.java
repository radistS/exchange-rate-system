package com.marcura.exchangerate.web.dto;

import java.util.List;

public record HistoryResponse(String from, String to, List<RatePointDto> rates) {
}
