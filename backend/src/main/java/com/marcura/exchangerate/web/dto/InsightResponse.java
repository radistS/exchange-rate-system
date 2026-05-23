package com.marcura.exchangerate.web.dto;

import java.time.LocalDate;

public record InsightResponse(
        String from, String to, LocalDate fromDate, LocalDate toDate, String insight) {
}
