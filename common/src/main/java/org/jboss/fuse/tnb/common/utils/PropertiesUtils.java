package org.jboss.fuse.tnb.common.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public final class PropertiesUtils {
    private PropertiesUtils() {
    }

    public static Properties toCamelCaseProperties(Properties underscoreProperties) {
        Properties camelCaseProperties = new Properties();
        underscoreProperties.forEach((key, value) -> camelCaseProperties.put(StringUtils.replaceUnderscoreWithCamelCase(key.toString()), value));
        return camelCaseProperties;
    }

    /**
     * Dumps the properties as new-line separated string of key=value.
     *
     * @param properties properties
     * @return string
     */
    public static String toString(Properties properties) {
        return toString(properties, "");
    }

    public static String toString(Properties properties, String prefix) {
        return properties.entrySet().stream().map(entry -> prefix + entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Converts properties object into a map.
     *
     * @param properties of service, obtained from credentials.yaml
     * @param prefix e.g. "camel.kamelet.aws-s3-source."
     * @return map with keys in camelCase
     */
    public static Map<String, Object> toMap(Properties properties, String prefix) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (properties != null) {
            properties.forEach((key, value) -> map.put(Optional.ofNullable(prefix).orElse("")
                + StringUtils.replaceUnderscoreWithCamelCase(key.toString()), value));
        }
        return map;
    }

    public static String toUriParameters(Properties properties) {
        return toString(properties).replaceAll("\n", "&");
    }
}