package com.marcura.exchangerate.web;

import com.marcura.exchangerate.service.AnalyticsService;
import com.marcura.exchangerate.web.api.AnalyticsApi;
import com.marcura.exchangerate.web.dto.AnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class AnalyticsController implements AnalyticsApi {

    private final AnalyticsService analyticsService;

    @Override
    public AnalyticsResponse getAnalytics() {
        return analyticsService.getAnalytics();
    }
}
