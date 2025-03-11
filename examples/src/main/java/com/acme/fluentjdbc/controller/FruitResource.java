package com.acme.fluentjdbc.controller;

import com.acme.fluentjdbc.App;
import com.acme.fluentjdbc.controller.dto.AddFruitPOST;
import com.acme.fluentjdbc.controller.dto.Farmer;
import com.acme.fluentjdbc.controller.dto.FarmerPOST;
import com.acme.fluentjdbc.controller.dto.Fruit;
import com.acme.fluentjdbc.controller.dto.FruitPOST;
import com.acme.fluentjdbc.controller.dto.FruitPUT;
import com.acme.fluentjdbc.controller.dto.SearchCriteria;
import io.quarkiverse.fluentjdbc.runtime.JsonObjectMapper;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
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
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.postgresql.copy.CopyManager;
import org.postgresql.jdbc.PgConnection;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.acme.fluentjdbc.controller.dto.SearchCriteria.Operator.EQ;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;


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
//                .params(fruit.name(), fruit.type(), fruit.calories(), fruit.carbohydrates(), fruit.fiber(), fruit.sugars(), fruit.fat(), fruit.protein())
                .params(paramsFromDto(fruit))
                .runFetchGenKeys(Mappers.singleLong())
                .firstKey();

        return RestResponse.created(uriInfo.getAbsolutePathBuilder().path(id.get().toString()).build());
    }

    @PUT
    public RestResponse<Void> update(@Valid FruitPUT fruit) {
        // dynamically create update stmt, by checking the provided params
        var params = new LinkedHashMap<String, Object[]>();
        addParamIfNotNull(params, new Statement("name", fruit.name()));
        addParamIfNotNull(params, new Statement("type", fruit.type()));
        addParamIfNotNull(params, new Statement("calories", fruit.calories()));
        addParamIfNotNull(params, new Statement("carbohydrates", fruit.carbohydrates()));
        addParamIfNotNull(params, new Statement("fiber", fruit.fiber()));
        addParamIfNotNull(params, new Statement("sugars", fruit.sugars()));
        addParamIfNotNull(params, new Statement("fat", fruit.fat()));
        addParamIfNotNull(params, new Statement("protein", fruit.protein()));

        var query = toQuery(params, ", ");
        var values = toParams(params, fruit.id());

        var count = this.jdbc.query()
                .update("update fruit set %s where id = ?".formatted(query))
                .params(values)
                .run()
                .affectedRows();

        if (count > 0) {
            Log.infof("%d fruits updated", count);
        }

        return RestResponse.ok();
    }

    @GET
    @Path("/search")
    public List<Fruit> search(@BeanParam @Valid SearchCriteria criteria) {
        // dynamic search with operators, e.g. : where calories < 200 and fiber > 50 etc.
        var params = new LinkedHashMap<String, Object[]>();
        addParamIfNotNull(params, new Statement("lower(name) = lower(?)", criteria.name()));
        addParamIfNotNull(params, new Statement("lower(type) = lower(?)", criteria.type()));
        addParamIfNotNull(params, new Statement("calories %s ?".formatted(criteria.calOp().orElse(EQ).value), criteria.calories()));
        addParamIfNotNull(params, new Statement("carbohydrates %s ?".formatted(criteria.carbOp().orElse(EQ).value), criteria.carbohydrates()));
        addParamIfNotNull(params, new Statement("fiber %s ?".formatted(criteria.fibOp().orElse(EQ).value), criteria.fiber()));
        addParamIfNotNull(params, new Statement("sugars %s ?".formatted(criteria.sugOp().orElse(EQ).value), criteria.sugars()));
        addParamIfNotNull(params, new Statement("fat %s ?".formatted(criteria.fatOp().orElse(EQ).value), criteria.fat()));
        addParamIfNotNull(params, new Statement("protein %s ?".formatted(criteria.protOp().orElse(EQ).value), criteria.protein()));

        var query = toQuery(params, " and ");
        var values = toParams(params);

        return this.jdbc.query()
                .select("select %s from fruit where %s".formatted(App.Mappers.fruitMapper.columnNames(), query))
                .params(values)
                .listResult(App.Mappers.fruitMapper);
    }

    @GET
    public List<Fruit> findAll(@RestQuery @DefaultValue("0") @Min(0) int start, @RestQuery @Min(1) @Max(100) @DefaultValue("50") long size) {
        return this.jdbc.query()
                .select("select %s from fruit where id > ? order by id".formatted(App.Mappers.fruitMapper.columnNames()))
                .params(start)
                .maxRows(size)
                .listResult(App.Mappers.fruitMapper);
    }

    @GET
    @Path("/farmers")
    public List<Farmer> findAllFarmers() {
        return this.jdbc.query()
                .select(App.Queries.SELECT_FARMER)
                .maxRows(50L)
                .listResult(App.Mappers.farmerMapper);
    }

    @POST
    @Path("/farmers")
    public RestResponse<Void> addFarmer(@Valid FarmerPOST farmer, @Context UriInfo uriInfo) {
        var id = this.jdbc.query()
                .update(App.Queries.INSERT_FARMER)
                .params(farmer.name(), farmer.city())
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
    public List<Fruit> findAllFruitFarmers() {
        return this.jdbc.query()
                .select(App.Queries.SELECT_FRUIT_FARMER_AMOUNTS)
                .maxRows(50L)
                .listResult(App.Mappers.fruitMapper);
    }

    @GET
    @Path("/reports")
    public List<Map<String, Object>> reports() {
        return this.jdbc.query()
                .select(App.Queries.FRUIT_REPORT)
                .maxRows(50L)
                .listResult(Mappers.map());
        // or use the JsonObject mapper
//                .listResult(this.jsonObjectMapper);

    }

    @GET
    @Path("/export")
    @Produces("text/csv")
    public Response export() {
        StreamingOutput stream = out -> this.jdbc.query().plainConnection(con -> {
            try {
                return new CopyManager(con.unwrap(PgConnection.class)).copyOut(App.Queries.EXPORT_CSV, out);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        });

        var date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return Response.ok(stream)
                .header(CONTENT_DISPOSITION, "attachment; filename=\"fruits_export_%s.csv\"".formatted(date))
                .build();
    }

    private String toQuery(LinkedHashMap<String, Object[]> params, String operator) {
        var result = params.keySet()
                .stream()
                .map(stmt -> {
                    if (!stmt.contains("?")) {
                        return "%s = ?".formatted(stmt);
                    }
                    return stmt;
                })
                .collect(Collectors.joining(operator));

        Log.debugf("Built query: %s", result);
        return result;
    }

    private void addParamIfNotNull(LinkedHashMap<String, Object[]> params, Statement stmt) {
        if (stmt.values() != null) {
            for (Object val : stmt.values()) {
                if (val != null && !val.toString().isBlank()) {
                    params.put(stmt.stmt(), stmt.values());
                    break;
                }
            }
        }
    }

    private Object[] toParams(LinkedHashMap<String, Object[]> params, Object... otherParams) {
        return Stream.concat(
                params.values().stream().flatMap(Arrays::stream),
                Stream.of(otherParams)
        ).toArray();
    }

    private static List<Object> paramsFromDto(Object obj) {
        // shorthand for getting all params, handy if you have a large object
        // unfortunately jsonObject.toMap().values() doesn't work
        return JsonObject.mapFrom(obj).stream().map(Map.Entry::getValue).toList();
    }

    public record Statement(String stmt, Object... values) {
    }
}
