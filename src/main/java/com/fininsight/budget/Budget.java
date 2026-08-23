package com.fininsight.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Budget entity representing spending targets defined per category for a month/year.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "budgets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_budgets_user_cat_period",
                        columnNames = {"user_id", "category_id", "year", "month"}
                )
        }
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "month", nullable = false)
    private short month;

    @Column(name = "year", nullable = false)
    private short year;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Budget(UUID userId, UUID categoryId, BigDecimal amount, short month, short year) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.month = month;
        this.year = year;
    }

    public Budget(UUID userId, UUID categoryId, BigDecimal amount, int month, int year) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.month = (short) month;
        this.year = (short) year;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Budget budget = (Budget) o;
        return id != null && Objects.equals(id, budget.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
