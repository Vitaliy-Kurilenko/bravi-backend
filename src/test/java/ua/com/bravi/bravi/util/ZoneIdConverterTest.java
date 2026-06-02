package ua.com.bravi.bravi.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneIdConverterTest {

    private final ZoneIdConverter converter = new ZoneIdConverter();

    @Test
    void convertsZoneIdToItsTextualId() {
        assertThat(converter.convertToDatabaseColumn(ZoneId.of("Europe/Kyiv")))
                .isEqualTo("Europe/Kyiv");
    }

    @Test
    void parsesZoneIdFromText() {
        assertThat(converter.convertToEntityAttribute("UTC"))
                .isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void mapsNullBothWays() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
