package com.acme.fluentjdbc.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static io.quarkus.runtime.util.StringUtil.isNullOrEmpty;

public record FruitPUT(
        @NotNull @Min(1) Long id,
        @Size(max = 255) String name,
        @Size(max = 255) String type,
        BigDecimal calories,
        BigDecimal carbohydrates,
        BigDecimal fiber,
        BigDecimal sugars,
        BigDecimal fat,
        BigDecimal protein
) {

    @AssertTrue(message = "At least one parameter must be provided")
    public boolean paramsNotEmpty() {
        return !(isNullOrEmpty(this.name)
                 && isNullOrEmpty(this.type)
                 && calories == null
                 && carbohydrates == null
                 && fiber == null
                 && sugars == null
                 && fat == null
                 && protein == null);
    }
}
