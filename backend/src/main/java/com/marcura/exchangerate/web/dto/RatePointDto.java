package com.marcura.exchangerate.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RatePointDto(LocalDate date, BigDecimal rate) {
}
