package com.marcura.exchangerate.service;

import com.marcura.exchangerate.domain.CurrencyUsage;
import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.web.dto.AnalyticsResponse;
import com.marcura.exchangerate.web.dto.CurrencyUsageDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CurrencyUsageRepository currencyUsageRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {
        List<CurrencyUsageDto> top = currencyUsageRepository.findAllByOrderByQueryCountDesc().stream()
                .filter(u -> u.getQueryCount() > 0)
                .map(this::toDto)
                .toList();
        return new AnalyticsResponse(top);
    }

    private CurrencyUsageDto toDto(CurrencyUsage usage) {
        return new CurrencyUsageDto(
                usage.getCurrencyCode(),
                usage.getQueryCount(),
                usage.getLastQueriedAt());
    }
}
