package io.quarkiverse.fluentjdbc.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.util.Optional;

import org.codejargon.fluentjdbc.api.query.Transaction.Isolation;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = RUN_TIME)
@ConfigMapping(prefix = "quarkus.fluentjdbc")
public interface FluentJdbcConfig {

    /**
     * the default transaction isolation level.
     *
     * @return the default transaction isolation level.
     */
    Optional<Isolation> transactionIsolation();

    /**
     * The default batch size for batch queries.
     * Default is 50.
     *
     * @return default batch size
     */
    @WithDefault("50")
    Integer batchSize();

    /**
     * The default fetch size for queries.
     * Default is 20.
     *
     * @return default fetch size
     */
    @WithDefault("20")
    Integer fetchSize();

    default boolean isNotEmpty() {
        return batchSize() != null || fetchSize() != null || transactionIsolation() != null;
    }
}
