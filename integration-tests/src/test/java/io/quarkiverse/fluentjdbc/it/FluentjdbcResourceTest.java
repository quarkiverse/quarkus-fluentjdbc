package io.quarkiverse.fluentjdbc.it;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(FluentjdbcResource.class)
public class FluentjdbcResourceTest {

    @Test
    void fluentJdbcStartUpCheck() {
        var response = when().get().then().extract().body().asString();
        assertEquals(1, Integer.parseInt(response));
    }
}
