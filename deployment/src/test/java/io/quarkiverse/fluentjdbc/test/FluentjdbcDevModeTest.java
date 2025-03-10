package io.quarkiverse.fluentjdbc.test;

import io.quarkiverse.fluentjdbc.runtime.FluentJdbcConfig;
import io.quarkus.test.QuarkusDevModeTest;
import jakarta.inject.Inject;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.codejargon.fluentjdbc.api.query.Transaction.Isolation.READ_COMMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FluentjdbcDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties")
                    .addClass(FluentJdbcConfig.class)
                    .addClass(FluentJdbc.class));

    @Inject
    FluentJdbc jdbc;

    @Inject
    FluentJdbcConfig config;

    @Test
    void notNull() {
        assertNotNull(this.jdbc);
        assertNotNull(this.config);

        assertEquals(100, this.config.batchSize());
        assertEquals(50, this.config.fetchSize());
        assertNull(this.config.transactionIsolation());
    }

    @Test
    public void writeYourOwnDevModeTest() {
        this.jdbc.query().plainConnection(con -> {
            try (var stmt = con.createStatement()) {
                return stmt.execute("""
                            CREATE TABLE fruit (
                                id SERIAL PRIMARY KEY,
                                name VARCHAR(255) NOT NULL,
                                type VARCHAR(255) NOT NULL
                            )
                        """);
            }
        });

        this.jdbc.query()
                .update("insert into fruit(name, type) values(?,?)")
                .params("golden roger", "apple")
                .run();

        var count = this.jdbc.query().select("select count(*) from fruit").singleResult(Mappers.singleLong());
        assertEquals(1L, count);

        // restart app, then the data should still stay in the db
        var updateAppProperties = """
                quarkus.fluentjdbc.batch-size=10
                quarkus.fluentjdbc.fetch-size=1
                quarkus.fluentjdbc.transaction-isolation=READ_COMMITTED
                """;
        devModeTest.modifyResourceFile("application.properties", content -> content.concat(updateAppProperties));

        count = this.jdbc.query().select("select count(*) from fruit").singleResult(Mappers.singleLong());
        assertEquals(1L, count);
        assertEquals(10, this.config.batchSize());
        assertEquals(1, this.config.fetchSize());
        assertEquals(READ_COMMITTED, this.config.transactionIsolation());
    }
}
