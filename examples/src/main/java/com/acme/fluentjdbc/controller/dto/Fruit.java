package com.acme.fluentjdbc.controller.dto;

import java.math.BigDecimal;

public record Fruit(
        int id,
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
