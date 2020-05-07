package options.yaml;

import org.apache.commons.text.StringEscapeUtils;

public class SortInfo {
    public String name;
    public String methodName;
    public String description;
    public Statistics statistics;
    public String algorithm;
    public String algorithmDescription;

    public String asJSObject() {
        return "{" +
                "\"name\": \"" + StringEscapeUtils.escapeJava(name) + "\", " +
                "\"methodName\": \"" + StringEscapeUtils.escapeJava(methodName) + "\", " +
                "\"description\": \"" + StringEscapeUtils.escapeJava(description) + "\", " +
                "\"statistics\": " + statistics.toJSObject() + ", " +
                "\"algorithm\": \"" + StringEscapeUtils.escapeJava(algorithm) + "\", " +
                "\"algorithmDescription\": \"" + StringEscapeUtils.escapeJava(algorithmDescription) + "\"" +
                "}";
    }
}
