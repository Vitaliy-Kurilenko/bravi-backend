package ua.com.bravi.bravi.shared.component;

public record DeviceInfo(
        String deviceClass,
        String osName,
        String osVersion,
        String agentName,
        String agentVersion,
        String rawUserAgent
) {

    public static DeviceInfo unknown(String rawUserAgent) {
        return new DeviceInfo("Unknown", "Unknown", "", "Unknown", "", rawUserAgent == null ? "" : rawUserAgent);
    }
}
