package io.quarkiverse.fluentjdbc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fluentjdbc.runtime.FluentJdbcConfig;
import io.quarkiverse.fluentjdbc.runtime.RecordMapper;
import io.quarkus.test.QuarkusUnitTest;

public class FluentjdbcTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource("application.properties"));

    @Inject
    FluentJdbc jdbc;

    @Inject
    FluentJdbcConfig config;

    @Test
    @Order(1)
    void notNull() {
        assertNotNull(this.jdbc);
        assertNotNull(this.config);

        assertEquals(100, this.config.batchSize());
        assertEquals(50, this.config.fetchSize());
        assertEquals(Optional.empty(), this.config.transactionIsolation());
    }

    @Test
    @Order(2)
    public void checkSomeQueries() {
        this.jdbc.query().plainConnection(con -> {
            try (var stmt = con.createStatement()) {
                return stmt.execute("""
                            CREATE TABLE fruit (
                                id SERIAL PRIMARY KEY,
                                ext_id UUID NOT NULL,
                                name VARCHAR(255) NOT NULL,
                                type VARCHAR(255) NOT NULL
                            )
                        """);
            }
        });

        this.jdbc.query()
                .update("insert into fruit(ext_id, name, type) values(?,?,?)")
                .params(UUID.randomUUID(), "McIntosh", "apple")
                .run();

        var count = this.jdbc.query().select("select count(*) from fruit").singleResult(Mappers.singleLong());
        assertEquals(1L, count);

        var fruitMapper = new RecordMapper(Fruit.class);
        var fruit = (Fruit) this.jdbc.query()
                .select("select %s from fruit".formatted(fruitMapper.columnNames()))
                .singleResult(fruitMapper);

        assertNotNull(fruit.ext_id());
        assertEquals("McIntosh", fruit.name());
        assertEquals("apple", fruit.type());
    }

    public record Fruit(UUID ext_id, String name, String type) {
    }
}
