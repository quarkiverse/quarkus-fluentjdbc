package com.acme.fluentjdbc.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record Fruit(
        @JsonProperty("extId") UUID ext_id,
        String name,
        String type,
        BigDecimal calories,
        BigDecimal carbohydrates,
        BigDecimal fiber,
        BigDecimal sugars,
        BigDecimal fat,
        BigDecimal protein
) {
}
