package com.agri.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class LoyaltyConcurrencyMysqlTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.44");

    @Test
    void pessimisticLockPreventsTwoCheckoutsFromOverspendingPoints() throws Exception {
        try (Connection setup = connection()) {
            setup.createStatement().execute("CREATE TABLE users (id BIGINT PRIMARY KEY, loyalty_points_balance INT NOT NULL CHECK (loyalty_points_balance >= 0))");
            setup.createStatement().execute("INSERT INTO users VALUES (1,100)");
        }
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successful = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            for (int i=0;i<2;i++) executor.submit(() -> {
                ready.countDown(); start.await();
                try (Connection connection = connection()) {
                    connection.setAutoCommit(false);
                    try (var select=connection.prepareStatement("SELECT loyalty_points_balance FROM users WHERE id=1 FOR UPDATE")) {
                        var result=select.executeQuery(); result.next(); int balance=result.getInt(1);
                        if (balance >= 80) {
                            try (var update=connection.prepareStatement("UPDATE users SET loyalty_points_balance=loyalty_points_balance-80 WHERE id=1")) { update.executeUpdate(); }
                            successful.incrementAndGet();
                        }
                    }
                    connection.commit();
                }
                return null;
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            executor.shutdown(); assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
        try (Connection check=connection(); var statement=check.createStatement(); var result=statement.executeQuery("SELECT loyalty_points_balance FROM users WHERE id=1")) {
            result.next(); assertThat(result.getInt(1)).isEqualTo(20);
        }
        assertThat(successful).hasValue(1);
    }

    private static Connection connection() throws Exception { return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()); }
}
