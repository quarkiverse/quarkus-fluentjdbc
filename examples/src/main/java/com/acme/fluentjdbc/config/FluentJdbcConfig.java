package com.acme.fluentjdbc.config;

import com.acme.fluentjdbc.controller.dto.Fruit;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.ParamSetter;
import org.codejargon.fluentjdbc.api.mapper.ObjectMappers;
import org.codejargon.fluentjdbc.api.query.SqlErrorHandler;
import org.codejargon.fluentjdbc.api.query.listen.AfterQueryListener;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

public class FluentJdbcConfig {

    @Produces
    @Singleton
    SqlErrorHandler errorHandler() {
        return (err, query) -> {
            if (err.getErrorCode() == 123) {
                return SqlErrorHandler.Action.RETRY;
            }
            Log.errorf("Error occured while executing query: %s, state: %s, code: %s",
                    query.orElse("no query found"),
                    err.getSQLState(),
                    err.getErrorCode()
            );
            return null;
        };
    }

    @Produces
    @Singleton
    AfterQueryListener queryListener() {
        return execution -> {
            if (execution.success()) {
                Log.debugf("Query took %s ms to execute: %s",
                        execution.sql(),
                        execution.executionTimeMs()
                );
            }
        };
    }

    @Produces
    @Singleton
    ObjectMappers objectMappers() {
        return ObjectMappers.builder().build();
    }

    @Produces
    @Singleton
    public ParamSetter<UUID> uuidParamSetter() {
        return (uuid, prepStmt, i) -> prepStmt.setString(i, uuid.toString());
    }
}
