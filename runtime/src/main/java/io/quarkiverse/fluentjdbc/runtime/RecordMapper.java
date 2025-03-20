package io.quarkiverse.fluentjdbc.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.codejargon.fluentjdbc.api.query.Mapper;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A Java Record mapper for FluentJdbc.
 * <p>
 * Usage:
 * <blockquote>
 * <code>
 * static final RecordMapper fruitMapper = new RecordMapper(Fruit.class);
 * <p>
 * // select all columns<br/>
 * List&lt;Fruit&gt; fruits = query().select("select * from fruit").listMapper(fruitMapper);
 * </p>
 * <p>
 * // select only the columns defined in the record <br/>
 * List&lt;Fruit&gt; fruits = query().select("select %s from fruit", fruitMapper.columnNames()).listMapper(fruitMapper);
 * </code>
 * </blockquote>
 *
 * @param <T> a Record
 */
@RegisterForReflection
public class RecordMapper<T extends Record> implements Mapper<T> {

    final String columnNames;
    final Constructor<T> constructor;
    final boolean dbColumnNameToCamelCase;

    public RecordMapper(Class<T> type) {
        this(type, true);
    }

    public RecordMapper(Class<T> type, boolean toCamelCase) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException("Class %s is not a Record".formatted(type));
        }

        this.dbColumnNameToCamelCase = toCamelCase;
        this.constructor = findConstructor(type);
        this.columnNames = columnNamesOfDb();
    }

    @Override
    public T map(ResultSet rs) throws SQLException {
        try {
            if (this.constructor != null) {
                Object[] parameters = getConstructorParameters(rs);
                return constructor.newInstance(parameters);
            } else {
                throw new IllegalArgumentException("No suitable Record found for class name");
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Record could not be instantiated", e);
        }
    }

    public String columnNames() {
        return this.columnNames;
    }

    private Constructor<T> findConstructor(Class<T> recordType) {
        return (Constructor<T>) recordType.getDeclaredConstructors()[0];
    }

    private Object[] getConstructorParameters(ResultSet rs) throws SQLException {
        var parameterTypes = this.constructor.getParameterTypes();
        var params = new Object[parameterTypes.length];

        var resultSetMetaData = rs.getMetaData();
        var columnCount = resultSetMetaData.getColumnCount();
        var columnNameToIndex = new HashMap<String, Integer>();

        for (int i = 1; i <= columnCount; i++) {
            var dbColumnName = resultSetMetaData.getColumnName(i);
            if (this.dbColumnNameToCamelCase) {
                columnNameToIndex.put(toCamelCase(dbColumnName), i);
            } else {
                columnNameToIndex.put(dbColumnName, i);
            }
        }

        var parameterNames = this.constructor.getParameters();
        for (int i = 0; i < parameterTypes.length; i++) {
            var parameterName = parameterNames[i].getName();
            if (columnNameToIndex.containsKey(parameterName)) {
                var columnIndex = columnNameToIndex.get(parameterName);

                params[i] = switch (parameterTypes[i].getName()) {
                    case "java.lang.String" -> rs.getString(columnIndex);
                    case "java.lang.Long" -> rs.getLong(columnIndex);
                    case "java.lang.Integer" -> rs.getInt(columnIndex);
                    case "java.lang.Double" -> rs.getDouble(columnIndex);
                    case "java.lang.Boolean" -> rs.getBoolean(columnIndex);
                    case "double" -> rs.getDouble(columnIndex);
                    case "int" -> rs.getInt(columnIndex);
                    case "boolean" -> rs.getBoolean(columnIndex);
                    case "java.math.BigDecimal" -> rs.getBigDecimal(columnIndex);
                    case "java.time.LocalDate" -> rs.getDate(columnIndex).toLocalDate();
                    case "java.time.LocalDateTime" -> rs.getTimestamp(columnIndex).toLocalDateTime();
                    case "java.time.LocalTime" -> rs.getTime(columnIndex).toLocalTime();
                    case "java.time.OffsetDateTime" -> rs.getObject(columnIndex, OffsetDateTime.class);
                    case "java.sql.Date" -> rs.getDate(columnIndex);
                    case "java.sql.Timestamp" -> rs.getTimestamp(columnIndex);
                    case "java.util.Date" -> rs.getTimestamp(columnIndex);
                    case "java.util.Calendar" -> {
                        var calendar = Calendar.getInstance();
                        calendar.setTime(rs.getTimestamp(columnIndex));
                        yield calendar;
                    }
                    case "java.sql.Array" -> rs.getArray(columnIndex);
                    case "java.sql.Blob" -> rs.getBlob(columnIndex);
                    case "java.sql.Clob" -> rs.getClob(columnIndex);
                    case "java.sql.NClob" -> rs.getNClob(columnIndex);
                    case "java.io.InputStream" -> rs.getBinaryStream(columnIndex);
                    case "java.io.Reader" -> rs.getCharacterStream(columnIndex);
                    default -> rs.getObject(columnIndex);
                };
            } else {
                throw new IllegalArgumentException("No matching column found for constructor parameter: " + parameterName);
            }
        }
        return params;
    }

    private String columnNamesOfDb() {
        return Arrays.stream(this.constructor.getParameters())
                .map(Parameter::getName)
                .map(column -> {
                    if (this.dbColumnNameToCamelCase) {
                        return toSnakeCase(column);
                    }
                    return column;
                })
                .collect(Collectors.joining(","));
    }

    private static String toSnakeCase(String columnName) {
        var result = new StringBuilder();
        for (var c : columnName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append("_").append(Character.toLowerCase(c));
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private static String toCamelCase(String columnName) {
        var result = new StringBuilder();
        var charsToSkip = List.of('_', '-', '.', ' ');
        var toUpperCase = false;
        var i = 0;

        for (var c : columnName.toCharArray()) {
            i++;
            if (charsToSkip.contains(c)) {
                // skip first: _example_id => should be exampleId and not ExampleId
                toUpperCase = i != 1;
                continue;
            }

            if (toUpperCase) {
                result.append(Character.toUpperCase(c));
                toUpperCase = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
