package com.acme.fluentjdbc.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddFruitPOST(@NotNull @Min(1) Long farmerId, @NotNull @Min(1) Long fruitId, @NotNull @Min(1) Integer amount) {
}
