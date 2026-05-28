package ua.com.bravi.bravi.component;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceInfoTest {

    @Test
    void unknownNormalizesNullRawUserAgentToEmpty() {
        DeviceInfo info = DeviceInfo.unknown(null);

        assertThat(info.deviceClass()).isEqualTo("Unknown");
        assertThat(info.osName()).isEqualTo("Unknown");
        assertThat(info.osVersion()).isEmpty();
        assertThat(info.agentName()).isEqualTo("Unknown");
        assertThat(info.agentVersion()).isEmpty();
        assertThat(info.rawUserAgent()).isEmpty();
    }

    @Test
    void unknownKeepsProvidedRawUserAgent() {
        DeviceInfo info = DeviceInfo.unknown("curl/8.1.2");

        assertThat(info.rawUserAgent()).isEqualTo("curl/8.1.2");
    }
}
