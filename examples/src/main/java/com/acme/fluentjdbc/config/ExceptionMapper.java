package com.acme.fluentjdbc.config;

import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import org.codejargon.fluentjdbc.api.FluentJdbcException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.Map;
import java.util.Objects;

import static org.jboss.resteasy.reactive.RestResponse.Status.BAD_REQUEST;
import static org.jboss.resteasy.reactive.RestResponse.Status.INTERNAL_SERVER_ERROR;

public class ExceptionMapper {

    @ServerExceptionMapper(priority = 1)
    public RestResponse<Map<String, String>> toResponse(WebApplicationException e) {
        var message = Objects.requireNonNullElse(e.getMessage(), getMessage(e));
        var status = Objects.requireNonNullElse(e.getResponse().getStatusInfo(), BAD_REQUEST);
        Log.error(message);

        return RestResponse.status(status, Map.of(
                "message", message,
                "exception", e.getClass().getName()
        ));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> toResponse(Exception e) {
        var message = Objects.requireNonNullElse(e.getMessage(), getMessage(e));
        Log.error(message);
        return RestResponse.status(INTERNAL_SERVER_ERROR, Map.of(
                "message", message,
                "exception", e.getClass().getName()
        ));
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> toResponse(FluentJdbcException e) {
        var expMsg = e.getMessage();
        var causeMsg = getMessage(e);

        var message = "%s, %s".formatted(expMsg, causeMsg);
        if (expMsg.startsWith("Error running query")) {
            message = causeMsg;
        }

        Log.error(message);
        return RestResponse.status(INTERNAL_SERVER_ERROR, Map.of(
                "message", message,
                "exception", e.getClass().getName()
        ));
    }

    private String getMessage(Exception e) {
        var result = e.getClass().getName();
        var cause = e.getCause();
        var maxDepth = 3;
        while (cause != null && maxDepth-- > 0) {
            result = cause.getMessage();
            cause = cause.getCause();
        }
        return result;
    }
}

