package com.acme.fluentjdbc.controller;

import com.acme.fluentjdbc.App;
import com.acme.fluentjdbc.controller.dto.AddFruitPOST;
import com.acme.fluentjdbc.controller.dto.Farmer;
import com.acme.fluentjdbc.controller.dto.FarmerPOST;
import com.acme.fluentjdbc.controller.dto.Fruit;
import com.acme.fluentjdbc.controller.dto.FruitPOST;
import com.acme.fluentjdbc.controller.dto.FruitPUT;
import com.acme.fluentjdbc.controller.dto.SearchCriteria;
import io.quarkiverse.fluentjdbc.runtime.DynamicQuery;
import io.quarkiverse.fluentjdbc.runtime.JsonObjectMapper;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.postgresql.copy.CopyManager;
import org.postgresql.jdbc.PgConnection;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.acme.fluentjdbc.App.Mappers.fruitMapper;
import static com.acme.fluentjdbc.controller.dto.SearchCriteria.Operator.EQ;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;


@Path("/fruits")
public class FruitResource {

    @Inject
    FluentJdbc jdbc;

    @Inject
    JsonObjectMapper jsonObjectMapper;

    @POST
    public RestResponse<Void> save(@Valid FruitPOST fruit, @Context UriInfo uriInfo) {
        var id = this.jdbc.query()
                .update(App.Queries.INSERT_FRUIT)
                .params(UUID.randomUUID(), fruit.name(), fruit.type(), fruit.calories(), fruit.carbohydrates(), fruit.fiber(), fruit.sugars(), fruit.fat(), fruit.protein())
                .runFetchGenKeys(Mappers.singleLong())
                .firstKey();

        return RestResponse.created(uriInfo.getAbsolutePathBuilder().path(id.get().toString()).build());
    }

    @GET
    @Path("/search")
    public List<Fruit> search(@BeanParam @Valid SearchCriteria criteria) {
        // dynamic search with operators, e.g. : where calories < 200 and fiber > 50 etc.
        var queryResult = new DynamicQuery()
                .selectClauses(
                        "lower(name) = lower(?)",
                        "lower(type) = lower(?)",
                        "calories %s ?".formatted(criteria.calOp().orElse(EQ).value),
                        "carbohydrates %s ?".formatted(criteria.carbOp().orElse(EQ).value),
                        "fiber %s ?".formatted(criteria.fibOp().orElse(EQ).value),
                        "sugars %s ?".formatted(criteria.sugOp().orElse(EQ).value),
                        "fat %s ?".formatted(criteria.fatOp().orElse(EQ).value),
                        "protein %s ?".formatted(criteria.protOp().orElse(EQ).value)
                )
                .paramsFromDto(criteria, name -> !name.contains("Op"))
                .build();

        return this.jdbc.query()
                .select("select * from fruit %s order by id".formatted(queryResult.query()))
                .params(queryResult.parameters())
                .listResult(fruitMapper);
    }

    @PUT
    @Path("/{id}")
    public RestResponse<Void> update(@RestPath @Min(1) Long id, @Valid FruitPUT fruit) {
        // will create a dynamic query by checking the provided params: set name = ?, type = ?, ... where id = ?
        var queryResult = new DynamicQuery()
                .updateClauses("name", "type", "calories", "carbohydrates", "fiber", "sugars", "fat", "protein")
                .where("id")
                .paramsFromDto(fruit, id)
                .build();

        var count = this.jdbc.query()
                .update("update fruit %s".formatted(queryResult.query()))
                .params(queryResult.parameters())
                .run()
                .affectedRows();

        if (count > 0) {
            Log.infof("%d fruits updated", count);
        }

        return RestResponse.ok();
    }

    @GET
    public List<Fruit> findAll(@RestQuery @DefaultValue("0") @Min(0) int start, @RestQuery @Min(1) @Max(100) @DefaultValue("50") long size) {
        return this.jdbc.query()
                .select("select %s from fruit where id > ? order by id".formatted(fruitMapper.columnNames()))
                .params(start)
                .maxRows(size)
                .listResult(fruitMapper);
    }

    // with jsonb
    @GET
    @Path("/farmers")
    public List<Farmer> findAllFarmers() {
        return this.jdbc.query()
                .select(App.Queries.SELECT_FARMER)
                .maxRows(50L)
                .listResult(Farmer::fromRow);
    }

    // with jsonb
    @POST
    @Path("/farmers")
    public RestResponse<Void> addFarmer(@Valid FarmerPOST farmer, @Context UriInfo uriInfo) throws SQLException {
        var id = this.jdbc.query()
                .update(App.Queries.INSERT_FARMER)
                .params(farmer.name(), farmer.city(), JsonArray.of(farmer.certificates()).encode())
                .runFetchGenKeys(Mappers.singleLong())
                .firstKey();

        return RestResponse.created(uriInfo.getRequestUriBuilder().path(id.get().toString()).build());
    }

    @POST
    @Path("/farmers/fruits")
    public RestResponse<Void> addFruit(@Valid AddFruitPOST fruit) {
        var count = this.jdbc.query()
                .update(App.Queries.INSERT_FRUIT_FARMER)
                .params(fruit.farmerId(), fruit.fruitId(), fruit.amount())
                .run()
                .affectedRows();

        if (count > 0) {
            Log.infof("%d fruits added for farmer(id=%d)", fruit.amount(), fruit.farmerId());
        }

        return RestResponse.ok();
    }

    @GET
    @Path("/farmers/fruits")
    public List<JsonObject> findAllFruitFarmers() {
        return this.jdbc.query()
                .select(App.Queries.SELECT_FRUIT_FARMER_AMOUNTS)
                .maxRows(50L)
                .listResult(this.jsonObjectMapper);
    }

    @GET
    @Path("/reports")
    public List<Map<String, Object>> reports() {
        return this.jdbc.query()
                .select(App.Queries.FRUIT_REPORT)
                .maxRows(50L)
                .listResult(Mappers.map());
    }

    @GET
    @Path("/export")
    @Produces("text/csv")
    @Operation(summary = "Export fruits as CSV", description = "Exports the list of fruits as a CSV file.")
    @APIResponse(
            responseCode = "200",
            description = "A CSV file containing fruit data",
            content = @Content(
                    mediaType = "text/csv",
                    schema = @Schema(type = STRING, format = "binary")
            )
    )
    public Response export() {
        StreamingOutput stream = out -> this.jdbc.query().plainConnection(con -> {
            try {
                return new CopyManager(con.unwrap(PgConnection.class)).copyOut(App.Queries.EXPORT_CSV, out);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        });

        var date = LocalDateTime.now().format(App.DATE_FORMATTER);
        return Response.ok(stream)
                .header(CONTENT_DISPOSITION, "attachment; filename=\"fruits_export_%s.csv\"".formatted(date))
                .build();
    }
}
