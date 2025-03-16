package io.quarkiverse.fluentjdbc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.fluentjdbc.runtime.DynamicQuery;
import io.quarkiverse.fluentjdbc.runtime.FluentJdbcConfig;
import io.quarkiverse.fluentjdbc.runtime.RecordMapper;
import io.quarkus.test.QuarkusUnitTest;

@TestInstance(PER_CLASS)
public class FluentjdbcTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addAsResource("application.properties"));

    @Inject
    FluentJdbc jdbc;

    @Inject
    FluentJdbcConfig config;

    RecordMapper fruitMapper = new RecordMapper(Fruit.class);
    RecordMapper rawMapper = new RecordMapper(FruitRaw.class, false);

    @BeforeAll
    public void init() {
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
    }

    @BeforeEach
    public void cleanUp() {
        this.jdbc.query()
                .transaction()
                .inNoResult(() -> {
                    this.jdbc.query().update("delete from fruit").run();
                    this.jdbc.query()
                            .update("insert into fruit(ext_id, name, type) values(?,?,?)")
                            .params(UUID.randomUUID(), "McIntosh", "apple")
                            .run();
                });

        var count = this.jdbc.query().select("select count(*) from fruit").singleResult(Mappers.singleLong());
        assertEquals(1L, count);
    }

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
    public void checkRecordMapper() {
        var fruitAll = (Fruit) this.jdbc.query()
                .select("select * from fruit")
                .singleResult(fruitMapper);

        assertNotNull(fruitAll.extId());
        assertEquals("McIntosh", fruitAll.name());
        assertEquals("apple", fruitAll.type());

        var fruitByColumn = (Fruit) this.jdbc.query()
                .select("select %s from fruit".formatted(fruitMapper.columnNames()))
                .singleResult(fruitMapper);

        assertNotNull(fruitByColumn.extId());
        assertEquals("McIntosh", fruitByColumn.name());
        assertEquals("apple", fruitByColumn.type());

        var fruitRawAll = (FruitRaw) this.jdbc.query()
                .select("select * from fruit")
                .singleResult(rawMapper);

        assertNotNull(fruitRawAll.ext_id());
        assertEquals("McIntosh", fruitRawAll.name());
        assertEquals("apple", fruitRawAll.type());

        var fruitRaw = (FruitRaw) this.jdbc.query()
                .select("select %s from fruit".formatted(fruitMapper.columnNames()))
                .singleResult(rawMapper);

        assertNotNull(fruitRaw.ext_id());
        assertEquals("McIntosh", fruitRaw.name());
        assertEquals("apple", fruitRaw.type());
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            ,,1
            mcIntosh,,1
            ,apple,1
            ,Apple,0
            ,peer,0
            """)
    void dynamicSearch(String name, String type, int expectedCount) {
        var queryResult = new DynamicQuery()
                .selectClauses("lower(name) like lower(:name)", "type")
                .params(name, type)
                .build();

        var fruits = this.jdbc.query()
                .select("select * from fruit %s order by id".formatted(queryResult.query()))
                .params(queryResult.parameters())
                .listResult(this.fruitMapper);

        assertEquals(expectedCount, fruits.size());
    }

    public record Fruit(UUID extId, String name, String type) {
    }

    public record FruitRaw(UUID ext_id, String name, String type) {
    }
}
