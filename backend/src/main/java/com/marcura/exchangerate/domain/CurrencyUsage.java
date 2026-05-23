package com.marcura.exchangerate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Per-currency query counters; incremented via atomic SQL upsert. */
@Entity
@Table(name = "currency_usage")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CurrencyUsage {

    @Id
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "query_count", nullable = false)
    private long queryCount;

    @Column(name = "last_queried_at")
    private LocalDate lastQueriedAt;
}
