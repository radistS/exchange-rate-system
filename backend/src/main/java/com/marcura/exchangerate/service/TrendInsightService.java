package com.marcura.exchangerate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcura.exchangerate.web.dto.HistoryResponse;
import com.marcura.exchangerate.web.dto.InsightResponse;
import com.marcura.exchangerate.web.dto.RatePointDto;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Spring AI insight with JSON rate series in the user prompt; falls back if Ollama is down. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendInsightService {

    private static final String SYSTEM_PROMPT = """
            You are an internal FX dashboard assistant. Given a JSON array of {date, rate} objects \
            for a currency pair, write a brief trend insight in 2-4 sentences. Use only the provided \
            numbers. No investment advice. If fewer than two points, say data is insufficient.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Currency pair: %s/%s
            Period: %s to %s
            ratesJson=%s
            """;

    private final ExchangeRateService exchangeRateService;
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectMapper objectMapper;

    public InsightResponse generateInsight(String from, String to, LocalDate fromDate, LocalDate toDate) {
        String fromCurrency = from.toUpperCase();
        String toCurrency = to.toUpperCase();
        HistoryResponse history =
                exchangeRateService.historicalAdjusted(fromCurrency, toCurrency, fromDate, toDate);
        List<RatePointDto> points = history.rates();

        if (points.size() < 2) {
            return new InsightResponse(
                    fromCurrency,
                    toCurrency,
                    fromDate,
                    toDate,
                    "Insufficient rate data in the selected period to describe a trend.");
        }

        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            log.warn("ChatClient unavailable; using deterministic fallback");
            return new InsightResponse(
                    fromCurrency, toCurrency, fromDate, toDate, deterministicFallback(points));
        }

        try {
            String ratesJson = objectMapper.writeValueAsString(points); // LLM reads actual numbers
            String userMessage = USER_PROMPT_TEMPLATE.formatted(
                    fromCurrency, toCurrency, fromDate, toDate, ratesJson);
            String insight = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();
            if (insight == null || insight.isBlank()) {
                return new InsightResponse(
                        fromCurrency, toCurrency, fromDate, toDate, deterministicFallback(points));
            }
            return new InsightResponse(fromCurrency, toCurrency, fromDate, toDate, insight.trim());
        } catch (Exception ex) {
            log.warn("LLM insight generation failed", ex);
            return new InsightResponse(
                    fromCurrency, toCurrency, fromDate, toDate, deterministicFallback(points));
        }
    }

    /** Used when ChatClient is absent or the model call fails. */
    private String deterministicFallback(List<RatePointDto> points) {
        RatePointDto first = points.getFirst();
        RatePointDto last = points.getLast();
        java.math.BigDecimal change = last.rate()
                .subtract(first.rate())
                .divide(first.rate(), java.math.MathContext.DECIMAL64)
                .multiply(java.math.BigDecimal.valueOf(100));
        return "From %s to %s the adjusted rate moved from %s to %s (approx. %s%% change)."
                .formatted(
                        first.date(),
                        last.date(),
                        first.rate().toPlainString(),
                        last.rate().toPlainString(),
                        change.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
    }
}
