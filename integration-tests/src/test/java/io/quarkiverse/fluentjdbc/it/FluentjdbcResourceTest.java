package io.quarkiverse.fluentjdbc.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FluentjdbcResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/fluentjdbc")
                .then()
                .statusCode(200)
                .body(is("Hello fluentjdbc"));
    }
}
