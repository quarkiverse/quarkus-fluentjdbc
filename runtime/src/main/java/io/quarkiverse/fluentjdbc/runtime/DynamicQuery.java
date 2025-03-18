package io.quarkiverse.fluentjdbc.runtime;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static io.quarkiverse.fluentjdbc.runtime.QueryOperator.AND;
import static io.quarkiverse.fluentjdbc.runtime.QueryOperator.COMMA;
import static io.quarkiverse.fluentjdbc.runtime.QueryParamNamer.UNNUMBERED;
import static java.lang.Math.max;

/**
 * <p>
 * A helper class to produce dynamic queries which adds clauses to the query when the clauses are not null.
 * </p>
 *
 * <p>
 * Example: <br/>
 * <code>
 * - name = :name => with name null, </br>
 * - age > :age => age = 18 </br>
 * </code>
 * <p>
 * Will produce the following query: <code>where age > :age</code>
 * </p>
 */
@RegisterForReflection
public class DynamicQuery {
    // e.g. just "name" without " = :name"
    private static final String UNNAMED_CLAUSE = "\\w+\\s*$";
    private static final Pattern CLAUSE_PATTERN = Pattern.compile("^(.*?)\\s*$");

    protected final List<Object> parameters = new ArrayList<>();

    protected String[] clauses;
    protected QueryOperator operator = AND;
    protected QueryParamNamer paramNamer = UNNUMBERED;

    public DynamicQuery selectClauses(String... clauses) {
        this.clauses = clauses;
        return this;
    }

    public UpdateQuery updateClauses(String... clauses) {
        return new UpdateQuery(clauses);
    }

    public DynamicQuery params(Object... params) {
        this.parameters.addAll(Arrays.asList(params));
        return this;
    }

    public DynamicQuery paramsFromDto(Object dto, Object... otherParams) {
        return paramsFromDto(dto, true, otherParams);
    }

    /**
     * Reads the values from the given DTO.
     *
     * @param dto         the DTO
     * @param nameFilter  the parameters to be ex- or included by checking the name of a field in the DTO.
     * @param otherParams additional parameters that need to be used. These will be added after the parameters of the DTO.
     * @return a list of parameters from the DTO plus the additional parameters
     */
    public DynamicQuery paramsFromDto(Object dto, Predicate<String> nameFilter, Object... otherParams) {
        this.parameters.clear();
        var dtoParams = JsonObject.mapFrom(dto).stream()
                .filter(entry -> nameFilter.test(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        this.parameters.addAll(dtoParams);
        Collections.addAll(this.parameters, otherParams);
        return this;
    }

    /**
     * The naming of the parameters.
     * <ul>
     * <li>numbered: <code>name = ?1 and age > ?2</code>
     * <li>unnumbered: <code>name = ? and age > ?</code>
     * <li>named: <code>name = :age and age > :age</code>
     * </ul>
     *
     * @param paramNamer
     * @return
     */
    public DynamicQuery paramNamer(QueryParamNamer paramNamer) {
        this.paramNamer = paramNamer;
        return this;
    }

    public DynamicQuery operator(QueryOperator op) {
        this.operator = op;
        return this;
    }

    public QueryResult build() {
        var paramCounter = new AtomicInteger(1);
        var finalQuery = processClauses(paramCounter, (exp, list) -> {
            list.add("(%s)".formatted(renameParams(exp, paramCounter)));
        }, this.clauses);

        if (!finalQuery.isBlank()) {
            finalQuery = " where " + finalQuery;
        }

        return new QueryResult(finalQuery, this.parameters);
    }

    protected String processClauses(AtomicInteger paramCounter, BiConsumer<String, List> consumer, String... clauses) {
        var validExpressions = new ArrayList<>();
        var paramIndex = 0;

        for (var clause : clauses) {
            var matcher = CLAUSE_PATTERN.matcher(clause);
            while (matcher.find()) {
                var expression = matcher.group(1).trim();
                var validationResult = validateParams(expression, paramIndex);

                if (validationResult.isValid()) {
                    if (expression.matches(UNNAMED_CLAUSE)) {
                        validExpressions.add(nameExpression(expression, paramCounter));
                    } else {
                        consumer.accept(expression, validExpressions);
                    }
                    paramIndex += validationResult.paramCount();
                } else {
                    removeNullParams(paramIndex, validationResult.paramCount());
                }
            }
        }

        return String.join(this.operator.value, validExpressions.toArray(String[]::new));
    }

    protected String renameParams(String expression, AtomicInteger paramCounter) {
        return switch (this.paramNamer) {
            case NAMED -> expression;
            case NUMBERED -> replaceParams(expression, "?%d".formatted(paramCounter.getAndIncrement()));
            case UNNUMBERED -> replaceParams(expression, "?");
        };
    }

    private void removeNullParams(int paramIndex, int times) {
        for (int i = 0; i < times; i++) {
            this.parameters.remove(paramIndex);
        }
    }

    private ValidParamResult validateParams(String expression, int paramIndex) {
        var paramCounter = 0;
        var matcher = Pattern.compile(":").matcher(expression);
        var isValid = paramIndex < this.parameters.size() && this.parameters.get(paramIndex) != null;

        while (matcher.find()) {
            paramCounter++;
            isValid &= this.parameters.get(paramIndex++) != null;
        }

        return new ValidParamResult(isValid, max(1, paramCounter));
    }

    private String nameExpression(String expression, AtomicInteger paramCounter) {
        return switch (this.paramNamer) {
            case NAMED -> "%s = %s%s".formatted(expression, this.paramNamer.param, expression);
            case NUMBERED -> "%s = ?%d".formatted(expression, paramCounter.getAndIncrement());
            case UNNUMBERED -> "%s = %s".formatted(expression, this.paramNamer.param);
        };
    }

    private static String replaceParams(String expression, String replacement) {
        var result = expression;
        while (result.contains(":")) {
            result = result.replaceFirst(":\\w+", replacement);
        }
        return result;
    }

    private record ValidParamResult(boolean isValid, int paramCount) {
    }

    public record QueryResult(String query, List<Object> parameters) {
    }

    public static class UpdateQuery extends DynamicQuery {
        protected String whereClause;

        public UpdateQuery(String[] clauses) {
            this.clauses = clauses;
        }

        public UpdateQuery params(Object... params) {
            this.parameters.addAll(Arrays.asList(params));
            return this;
        }

        public UpdateQuery paramNamer(QueryParamNamer namer) {
            this.paramNamer = namer;
            return this;
        }

        public UpdateQuery where(String where) {
            this.whereClause = where;
            return this;
        }

        public QueryResult build() {
            if (this.parameters.isEmpty()) {
                throw new IllegalArgumentException("No parameters provided");
            }

            this.operator = COMMA;
            var paramCounter = new AtomicInteger(1);
            BiConsumer<String, List> updateExpAdder = (exp, list) -> list.add(renameParams(exp, paramCounter));
            var query = processClauses(paramCounter, updateExpAdder, this.clauses);

            if (query.isBlank()) {
                throw new IllegalStateException("Invalid UPDATE statement: No fields to update.");
            }

            var whereQuery = this.whereClause != null ? processClauses(paramCounter, updateExpAdder, this.whereClause) : "";
            String finalQuery = "SET " + query;
            if (!whereQuery.isBlank()) {
                finalQuery += " WHERE " + whereQuery;
            }

            return new QueryResult(finalQuery, this.parameters);
        }
    }
}
