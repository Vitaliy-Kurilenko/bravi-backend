package ua.com.bravi.bravi.component;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentParserTest {

    private final UserAgentParser parser = new UserAgentParser();

    @Test
    void returnsUnknownForNull() {
        DeviceInfo info = parser.parse(null);

        assertThat(info.deviceClass()).isEqualTo("Unknown");
        assertThat(info.osName()).isEqualTo("Unknown");
        assertThat(info.agentName()).isEqualTo("Unknown");
        assertThat(info.rawUserAgent()).isEmpty();
    }

    @Test
    void returnsUnknownForBlank() {
        DeviceInfo info = parser.parse("   ");

        assertThat(info.deviceClass()).isEqualTo("Unknown");
        assertThat(info.rawUserAgent()).isEqualTo("   ");
    }

    @Test
    void parsesChromeUserAgent() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

        DeviceInfo info = parser.parse(ua);

        assertThat(info.rawUserAgent()).isEqualTo(ua);
        assertThat(info.agentName()).contains("Chrome");
        assertThat(info.osName()).contains("Windows");
        assertThat(info.deviceClass()).isNotEqualTo("Unknown");
    }
}
