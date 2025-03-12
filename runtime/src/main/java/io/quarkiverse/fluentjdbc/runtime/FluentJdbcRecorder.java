package io.quarkiverse.fluentjdbc.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.enterprise.util.TypeLiteral;

import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.ParamSetter;
import org.codejargon.fluentjdbc.api.query.SqlErrorHandler;
import org.codejargon.fluentjdbc.api.query.Transaction.Isolation;
import org.codejargon.fluentjdbc.api.query.listen.AfterQueryListener;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.SmallRyeConfig;

@Recorder
public class FluentJdbcRecorder {

    static final String CONFIG_PREFIX = "quarkus.fluentjdbc.";

    public RuntimeValue<FluentJdbc> createFluentJdbc() {
        var dataSource = Arc.container().instance(DataSource.class);
        var sqlErrorHandler = Arc.container().instance(SqlErrorHandler.class);
        var afterQueryListener = Arc.container().instance(AfterQueryListener.class);
        var paramSetters = Arc.container().select(new TypeLiteral<ParamSetter<?>>() {
        }).handlesStream()
                .collect(Collectors.toMap(
                        param -> paramType(param.getBean().getTypes().iterator()),
                        param -> (ParamSetter) param.get()));

        if (!dataSource.isAvailable()) {
            throw new IllegalStateException("No datasource was configured");
        }

        var config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        var builder = new FluentJdbcBuilder().connectionProvider(dataSource.get());

        builder.defaultFetchSize(config.getValue(CONFIG_PREFIX + "fetch-size", Integer.class));
        builder.defaultBatchSize(config.getValue(CONFIG_PREFIX + "batch-size", Integer.class));

        var txIsolation = config.getOptionalValue(CONFIG_PREFIX + "transaction-isolation", Isolation.class);
        if (txIsolation.isPresent()) {
            Log.infof("FluentJdbc - setting default transaction isolation: %s", txIsolation.get());
            builder.defaultTransactionIsolation(txIsolation.get());
        }

        if (sqlErrorHandler.isAvailable()) {
            Log.info("FluentJdbc - setting default SqlErrorHandler");
            builder.defaultSqlHandler(() -> sqlErrorHandler.get());
        }

        if (afterQueryListener.isAvailable()) {
            Log.info("FluentJdbc - setting default AfterQueryListener");
            builder.afterQueryListener(afterQueryListener.get());
        }

        if (!paramSetters.isEmpty()) {
            Log.infof("FluentJdbc - setting ParamSetters for type(s): %s", paramSetters.keySet());
            builder.paramSetters(paramSetters);
        }

        return new RuntimeValue<>(builder.build());
    }

    private static Class paramType(Iterator<Type> types) {
        while (types.hasNext()) {
            Type type = types.next();

            if (type instanceof ParameterizedType pt) {
                Type rawType = pt.getRawType();

                if (rawType instanceof Class<?> clazz && clazz.equals(ParamSetter.class)) {
                    Type typeArgument = pt.getActualTypeArguments()[0];

                    if (typeArgument instanceof Class<?> result) {
                        return result;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Could not determine parameter type for ParamSetter.");
    }
}
