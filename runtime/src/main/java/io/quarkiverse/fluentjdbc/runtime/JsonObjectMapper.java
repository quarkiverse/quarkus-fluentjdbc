package io.quarkiverse.fluentjdbc.runtime;

import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.inject.Singleton;

import org.codejargon.fluentjdbc.api.query.Mapper;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.JsonObject;

@Singleton
@RegisterForReflection
public class JsonObjectMapper implements Mapper<JsonObject> {

    @Override
    public JsonObject map(ResultSet rs) throws SQLException {
        var result = new JsonObject();
        var metadata = rs.getMetaData();
        var columnCount = metadata.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            var columnName = metadata.getColumnLabel(i);
            if (columnName == null || columnName.isBlank() || columnName.matches("\\d+|\\?column\\?")) {
                columnName = "column_%d".formatted(i);
            }
            result.put(columnName, rs.getObject(i));
        }
        return result;
    }
}
