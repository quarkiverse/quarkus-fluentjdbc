package com.acme.fluentjdbc.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FarmerPOST(@NotBlank String name, @NotBlank String city, @NotNull @Size(max = 10) String[] certificates) {
}
