package io.quarkiverse.fluentjdbc.runtime;

import java.lang.reflect.ParameterizedType;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.ParamSetter;
import org.codejargon.fluentjdbc.api.query.SqlErrorHandler;
import org.codejargon.fluentjdbc.api.query.listen.AfterQueryListener;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FluentJdbcRecorder {

    FluentJdbcConfig config;

    @Inject
    public FluentJdbcRecorder(FluentJdbcConfig config) {
        this.config = config;
    }

    public RuntimeValue<FluentJdbc> createFluentJdbc() {
        var dataSource = Arc.container().instance(DataSource.class);
        var sqlErrorHandler = Arc.container().instance(SqlErrorHandler.class);
        var afterQueryListener = Arc.container().instance(AfterQueryListener.class);
        var paramSetters = Arc.container().select(ParamSetter.class).handlesStream()
                .map(InstanceHandle::get)
                .collect(Collectors.toMap(
                        this::getParamSetterType,
                        paramSetter -> paramSetter));

        if (!dataSource.isAvailable()) {
            throw new IllegalStateException("No datasource was configured");
        }

        System.out.println("config values: fetch: " + this.config.fetchSize());
        var builder = new FluentJdbcBuilder().connectionProvider(dataSource.get());
        builder.defaultFetchSize(this.config.fetchSize());
        builder.defaultBatchSize(this.config.batchSize());

        if (this.config.transactionIsolation().isPresent()) {
            builder.defaultTransactionIsolation(this.config.transactionIsolation().get());
        }

        if (sqlErrorHandler.isAvailable()) {
            builder.defaultSqlHandler(() -> sqlErrorHandler.get());
        }

        if (afterQueryListener.isAvailable()) {
            builder.afterQueryListener(afterQueryListener.get());
        }

        if (!paramSetters.isEmpty()) {
            builder.paramSetters(paramSetters);
        }

        return new RuntimeValue<>(builder.build());
    }

    private Class getParamSetterType(ParamSetter<?> paramSetter) {
        var superClass = paramSetter.getClass().getGenericInterfaces()[0];

        if (superClass instanceof ParameterizedType) {
            var typeArgument = ((ParameterizedType) superClass).getActualTypeArguments()[0];
            if (typeArgument instanceof Class<?>) {
                return (Class) typeArgument;
            }
        }
        throw new IllegalStateException("Unable to determine generic type for ParamSetter: " + paramSetter.getClass());
    }
}
