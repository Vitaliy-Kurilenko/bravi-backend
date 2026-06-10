package ua.com.bravi.bravi.shared.component;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    private final UserAgentAnalyzer analyzer = UserAgentAnalyzer.newBuilder()
            .hideMatcherLoadStats()
            .withCache(1000)
            .withFields(
                    UserAgent.DEVICE_CLASS,
                    UserAgent.OPERATING_SYSTEM_NAME,
                    UserAgent.OPERATING_SYSTEM_VERSION,
                    UserAgent.AGENT_NAME,
                    UserAgent.AGENT_VERSION
            )
            .build();

    public DeviceInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceInfo.unknown(userAgent);
        }
        UserAgent.ImmutableUserAgent parsed = analyzer.parse(userAgent);
        return new DeviceInfo(
                parsed.getValue(UserAgent.DEVICE_CLASS),
                parsed.getValue(UserAgent.OPERATING_SYSTEM_NAME),
                parsed.getValue(UserAgent.OPERATING_SYSTEM_VERSION),
                parsed.getValue(UserAgent.AGENT_NAME),
                parsed.getValue(UserAgent.AGENT_VERSION),
                userAgent
        );
    }
}
