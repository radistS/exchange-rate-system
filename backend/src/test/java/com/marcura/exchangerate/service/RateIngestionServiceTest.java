package com.marcura.exchangerate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcura.exchangerate.client.FixerClient;
import com.marcura.exchangerate.client.FixerResponse;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import com.marcura.exchangerate.repository.ExchangeRateUpsertRepository;
import com.marcura.exchangerate.service.exception.FixerApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateIngestionServiceTest {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 4, 23);
    private static final LocalDate DAY_TWO = LocalDate.of(2026, 4, 24);

    @Mock
    private FixerClient fixerClient;

    @Mock
    private ExchangeRateUpsertRepository upsertRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    private RateIngestionService rateIngestionService;

    @BeforeEach
    void setUp() {
        rateIngestionService =
                new RateIngestionService(fixerClient, new RateToUsdNormalizer(), upsertRepository, exchangeRateRepository);
    }

    @Test
    void ingestLatestUsesFixerDateNotToday() {
        when(fixerClient.fetchLatest()).thenReturn(fixerResponse(DAY_ONE));

        int count = rateIngestionService.ingestLatest();

        assertThat(count).isEqualTo(2);
        verify(upsertRepository).upsert(eq("EUR"), any(), eq(DAY_ONE));
        verify(upsertRepository).upsert(eq("USD"), any(), eq(DAY_ONE));
    }

    @Test
    void ingestLatestIfAbsentInsertsOnlyMissingRows() {
        when(fixerClient.fetchLatest()).thenReturn(fixerResponse(DAY_ONE));
        when(upsertRepository.insertIfAbsent(any(), any(), eq(DAY_ONE))).thenReturn(1, 0);

        int count = rateIngestionService.ingestLatestIfAbsent();

        assertThat(count).isEqualTo(1);
        verify(upsertRepository, never()).upsert(any(), any(), any());
    }

    @Test
    void ingestRangeSkipsFailedDays() {
        when(fixerClient.fetchForDate(DAY_ONE)).thenReturn(fixerResponse(DAY_ONE));
        doThrow(new FixerApiException("no data")).when(fixerClient).fetchForDate(DAY_TWO);

        int count = rateIngestionService.ingestRange(DAY_ONE, DAY_TWO);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void ingestMissingRangeSkipsExistingDates() {
        when(exchangeRateRepository.existsByRateDate(DAY_ONE)).thenReturn(true);
        when(fixerClient.fetchForDate(DAY_TWO)).thenReturn(fixerResponse(DAY_TWO));
        when(upsertRepository.insertIfAbsent(any(), any(), eq(DAY_TWO))).thenReturn(1, 1);

        int count = rateIngestionService.ingestMissingRange(DAY_ONE, DAY_TWO);

        assertThat(count).isEqualTo(2);
        verify(fixerClient, never()).fetchForDate(DAY_ONE);
        verify(fixerClient).fetchForDate(DAY_TWO);
        verify(upsertRepository, never()).upsert(any(), any(), any());
    }

    @Test
    void ingestRangeRejectsInvertedDates() {
        assertThatThrownBy(() -> rateIngestionService.ingestRange(DAY_TWO, DAY_ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from must be on or before to");
    }

    private static FixerResponse fixerResponse(LocalDate date) {
        return new FixerResponse(
                true,
                null,
                "EUR",
                date,
                Map.of("USD", new BigDecimal("1.08"), "EUR", BigDecimal.ONE));
    }
}
