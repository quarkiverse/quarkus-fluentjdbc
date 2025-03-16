package com.acme.fluentjdbc.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record Fruit(
        UUID extId,
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
