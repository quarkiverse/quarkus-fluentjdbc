package io.quarkiverse.fluentjdbc.runtime;

public enum QueryParamNamer {
    NAMED(':'),
    NUMBERED('?'),
    UNNUMBERED('?');

    public final char param;

    QueryParamNamer(char param) {
        this.param = param;
    }
}
