package com.marcura.exchangerate.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marcura.exchangerate.repository.ExchangeRateUpsertRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ExchangeControllerIT {

    private static final LocalDate RATE_DATE = LocalDate.of(2024, 3, 15);
    private static final LocalDate OLDER_DATE = LocalDate.of(2024, 3, 10);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateUpsertRepository upsertRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM currency_usage");
        upsertRepository.upsert("EUR", new BigDecimal("0.8"), OLDER_DATE);
        upsertRepository.upsert("PLN", new BigDecimal("3.6"), OLDER_DATE);
        upsertRepository.upsert("EUR", new BigDecimal("0.8"), RATE_DATE);
        upsertRepository.upsert("PLN", new BigDecimal("3.7"), RATE_DATE);
    }

    @Test
    void returnsSpreadAdjustedRateAndIncrementsUsageCounters() throws Exception {
        mockMvc.perform(get("/exchange")
                        .param("from", "EUR")
                        .param("to", "PLN")
                        .param("date", RATE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("EUR"))
                .andExpect(jsonPath("$.to").value("PLN"))
                .andExpect(jsonPath("$.date").value("2024-03-15"))
                .andExpect(jsonPath("$.exchange").value(4.4978125000))
                .andExpect(jsonPath("$.fromQueryCount").value(1))
                .andExpect(jsonPath("$.toQueryCount").value(1));

        mockMvc.perform(get("/exchange")
                        .param("from", "EUR")
                        .param("to", "PLN")
                        .param("date", RATE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromQueryCount").value(2))
                .andExpect(jsonPath("$.toQueryCount").value(2));
    }

    @Test
    void usesLatestRateDateWhenDateOmitted() throws Exception {
        mockMvc.perform(get("/exchange").param("from", "EUR").param("to", "PLN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2024-03-15"))
                .andExpect(jsonPath("$.exchange").value(4.4978125000));
    }

    @Test
    void returnsNotFoundWhenCurrencyMissingForDate() throws Exception {
        mockMvc.perform(get("/exchange")
                        .param("from", "EUR")
                        .param("to", "GBP")
                        .param("date", RATE_DATE.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rejectsInvalidCurrencyCode() throws Exception {
        mockMvc.perform(get("/exchange")
                        .param("from", "EURO")
                        .param("to", "PLN")
                        .param("date", RATE_DATE.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentExchangeRequestsIncrementCountersSafely() throws Exception {
        int requests = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResultActions>> futures = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return mockMvc.perform(get("/exchange")
                        .param("from", "EUR")
                        .param("to", "PLN")
                        .param("date", RATE_DATE.toString()));
            }));
        }

        start.countDown();
        for (Future<ResultActions> future : futures) {
            future.get().andExpect(status().isOk());
        }
        executor.shutdown();

        mockMvc.perform(get("/exchange")
                        .param("from", "EUR")
                        .param("to", "PLN")
                        .param("date", RATE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromQueryCount").value(requests + 1))
                .andExpect(jsonPath("$.toQueryCount").value(requests + 1));
    }
}
