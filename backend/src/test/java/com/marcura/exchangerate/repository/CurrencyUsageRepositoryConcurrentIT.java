package com.marcura.exchangerate.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifies atomic SQL upsert under parallel threads (assessment concurrency requirement). */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CurrencyUsageRepositoryConcurrentIT {

    private static final LocalDate QUERY_DATE = LocalDate.of(2024, 3, 15);
    private static final int THREAD_COUNT = 32;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CurrencyUsageRepository currencyUsageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanUsage() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate.update("DELETE FROM currency_usage");
    }

    @Test
    void parallelIncrementsProduceExactCount() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                transactionTemplate.executeWithoutResult(status -> {
                    currencyUsageRepository.upsertIncrement("EUR", QUERY_DATE);
                    currencyUsageRepository.upsertIncrement("PLN", QUERY_DATE);
                });
                return null;
            }));
        }

        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertThat(currencyUsageRepository.findById("EUR"))
                .isPresent()
                .get()
                .extracting(usage -> usage.getQueryCount())
                .isEqualTo((long) THREAD_COUNT);

        assertThat(currencyUsageRepository.findById("PLN"))
                .isPresent()
                .get()
                .extracting(usage -> usage.getQueryCount())
                .isEqualTo((long) THREAD_COUNT);
    }
}
