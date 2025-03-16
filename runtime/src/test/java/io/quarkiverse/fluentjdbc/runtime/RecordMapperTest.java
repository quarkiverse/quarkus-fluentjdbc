package io.quarkiverse.fluentjdbc.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecordMapperTest {

    @Test
    void columnNames() {
        var mapper = new RecordMapper(Fruit.class);

        assertThat(mapper.dbColumnNameToCamelCase).isTrue();
        assertThat(mapper.columnNames()).isEqualTo("ext_id,name");
    }

    record Fruit(String extId, String name) {
    }

}
