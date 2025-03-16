package com.acme.fluentjdbc.controller.dto;

import io.vertx.core.json.Json;
import jakarta.ws.rs.WebApplicationException;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Farmer(Long id, String name, String city, String[] certificates) {

    public static Farmer fromRow(ResultSet row) {
        try {
            return new Farmer(
                    row.getLong("id"),
                    row.getString("name"),
                    row.getString("city"),
                    Json.decodeValue(row.getString("certificates"), String[].class)
            );
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        }
    }
}
