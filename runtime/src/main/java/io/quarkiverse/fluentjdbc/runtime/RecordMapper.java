package io.quarkiverse.fluentjdbc.runtime;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.codejargon.fluentjdbc.api.query.Mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

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

    final Constructor<T> constructor;
    final String columnNames;

    public RecordMapper(Class<T> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException("Class %s is not a Record".formatted(type));
        }
        this.constructor = findConstructor(type);
        this.columnNames = columnNamesOfRecord();
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
            var columnName = resultSetMetaData.getColumnName(i).toLowerCase();
            columnNameToIndex.put(columnName, i);
        }

        var parameterNames = this.constructor.getParameters();
        for (int i = 0; i < parameterTypes.length; i++) {
            var parameterName = parameterNames[i].getName().toLowerCase();
            if (columnNameToIndex.containsKey(parameterName)) {
                var columnIndex = columnNameToIndex.get(parameterName);

                params[i] = switch (parameterTypes[i].getName()) {
                    case "long" -> rs.getLong(columnIndex);
                    case "java.lang.String" -> rs.getString(columnIndex);
                    case "double" -> rs.getDouble(columnIndex);
                    case "int" -> rs.getInt(columnIndex);
                    case "boolean" -> rs.getBoolean(columnIndex);
                    case "java.math.BigDecimal" -> rs.getBigDecimal(columnIndex);
                    default -> rs.getObject(columnIndex);
                };
            } else {
                throw new IllegalArgumentException("No matching column found for constructor parameter: " + parameterName);
            }
        }
        return params;
    }

    private String columnNamesOfRecord() {
        return Arrays.stream(this.constructor.getParameters())
                .map(Parameter::getName)
                .collect(Collectors.joining(","));
    }
}
