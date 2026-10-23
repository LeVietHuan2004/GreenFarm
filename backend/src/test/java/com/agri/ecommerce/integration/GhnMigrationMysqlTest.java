package com.agri.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class GhnMigrationMysqlTest {
    @Container static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.44");

    @Test void flywayCreatesGhnTablesWithoutChangingOldOrders() throws Exception {
        String legacyDump = Files.readString(Path.of("..", "database", "local", "veggie-main.sql"));
        var createTable = Pattern.compile("(?s)CREATE TABLE `[^`]+` \\(.*?\\) ENGINE=.*?;").matcher(legacyDump);
        try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            while (createTable.find()) statement.execute(createTable.group());
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
        }
        Flyway flyway = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
            .locations("classpath:db/migration").baselineOnMigrate(true).baselineVersion("1").load();
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("24");
        try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword());
             var statement = connection.createStatement()) {
            try (var tables = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('shipments','shipment_events')")) {
                tables.next(); assertThat(tables.getInt(1)).isEqualTo(2);
            }
            try (var columns = statement.executeQuery("SELECT COLUMN_DEFAULT FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'shipping_provider'")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString(1)).isEqualTo("internal");
            }
        }
    }
}
