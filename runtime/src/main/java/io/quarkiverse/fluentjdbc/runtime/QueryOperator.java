package io.quarkiverse.fluentjdbc.runtime;

public enum QueryOperator {
    AND(" and "),
    OR(" or "),
    COMMA(", ");

    public final String value;

    QueryOperator(String value) {
        this.value = value;
    }
}
