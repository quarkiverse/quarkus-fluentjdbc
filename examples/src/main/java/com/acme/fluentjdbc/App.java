package com.acme.fluentjdbc;

import com.acme.fluentjdbc.controller.dto.Fruit;
import io.quarkiverse.fluentjdbc.runtime.RecordMapper;

import java.time.format.DateTimeFormatter;

public class App {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static class Mappers {
        public static final RecordMapper fruitMapper = new RecordMapper(Fruit.class);
    }

    public static class Queries {
        public static final String INSERT_FRUIT = "insert into fruit(ext_id,name,type,calories,carbohydrates,fiber,sugars,fat,protein) values(?,?,?,?,?,?,?,?,?)";
        public static final String INSERT_FARMER = "insert into farmer(name, city, certificates) values(?,?, ?::jsonb)";
        public static final String INSERT_FRUIT_FARMER = "insert into fruit_farmer(farmer_id, fruit_id, amount) values(?,?,?)";

        public static final String SELECT_FARMER = "select * from farmer";
        public static final String SELECT_FRUIT_FARMER_AMOUNTS = """
                    select fa.name farmer, fr.name fruit, ff.amount
                    from fruit_farmer ff
                    left outer join farmer fa on ff.farmer_id = fa.id 
                    left outer join fruit fr on ff.fruit_id = fr.id
                    order by farmer 
                """;

        public static final String FRUIT_REPORT = """
                    select fa.name farmer, fr.type fruit, sum(ff.amount)
                    from fruit_farmer ff
                    left outer join farmer fa on ff.farmer_id = fa.id 
                    left outer join fruit fr on ff.fruit_id = fr.id
                    group by farmer, fruit
                    order by farmer 
                """;

        public static final String EXPORT_CSV = "copy (select * from fruit order by id) to STDOUT (FORMAT csv, HEADER, DELIMITER ';', ENCODING 'UTF-8')";
    }
}
