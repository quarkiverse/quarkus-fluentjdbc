package com.acme.fluentjdbc.config;

import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.codejargon.fluentjdbc.api.mapper.ObjectMappers;
import org.codejargon.fluentjdbc.api.query.SqlErrorHandler;
import org.codejargon.fluentjdbc.api.query.listen.AfterQueryListener;

public class FluentJdbcConfig {

    @Produces
    @Singleton
    @Unremovable
    SqlErrorHandler errorHandler() {
        return (err, query) -> {
            Log.errorf("Error occured while executing query: %s, state: %s, code: %s",
                    query.orElse("no query found"),
                    err.getSQLState(),
                    err.getErrorCode()
            );
            if (err.getErrorCode() == 123) {
                return SqlErrorHandler.Action.RETRY;
            }
            throw err;
        };
    }

    @Produces
    @Singleton
    @Unremovable
    AfterQueryListener queryListener() {
        return execution -> {
            if (execution.success()) {
                Log.debugf("Query took %s ms to execute: %s",
                        execution.executionTimeMs(),
                        execution.sql()
                );
            }
        };
    }

    @Produces
    @Singleton
    ObjectMappers objectMappers() {
        return ObjectMappers.builder().build();
    }
}
