package com.acme.fluentjdbc.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FruitPOST(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String type,
        BigDecimal calories,
        BigDecimal carbohydrates,
        BigDecimal fiber,
        BigDecimal sugars,
        BigDecimal fat,
        BigDecimal protein
) {
}
