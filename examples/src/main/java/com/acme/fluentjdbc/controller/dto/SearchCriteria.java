package com.acme.fluentjdbc.controller.dto;

import org.jboss.resteasy.reactive.RestQuery;

import java.math.BigDecimal;
import java.util.Optional;

public record SearchCriteria(
        @RestQuery String name,
        @RestQuery String type,
        @RestQuery BigDecimal calories,
        @RestQuery BigDecimal carbohydrates,
        @RestQuery BigDecimal fiber,
        @RestQuery BigDecimal sugars,
        @RestQuery BigDecimal fat,
        @RestQuery BigDecimal protein,
        @RestQuery Optional<Operator> calOp,
        @RestQuery Optional<Operator> carbOp,
        @RestQuery Optional<Operator> fibOp,
        @RestQuery Optional<Operator> sugOp,
        @RestQuery Optional<Operator> fatOp,
        @RestQuery Optional<Operator> protOp
) {
    public enum Operator {
        EQ("="),
        NEQ("<>"),
        GT(">"),
        LT("<"),
        GTE(">="),
        LTE("<=");

        public final String value;

        Operator(String operator) {
            this.value = operator;
        }
    }
}
