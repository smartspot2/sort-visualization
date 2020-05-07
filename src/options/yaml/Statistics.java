package options.yaml;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Map;

public class Statistics {
    public Map<String, String> timeComplexity;
    public String spaceComplexity;
    public Boolean stable;

    public String toJSObject() {
        return "{" +
                "\"timeComplexity\": {" +
                "\"best\": \"" + StringEscapeUtils.escapeJava(timeComplexity.get("best")) + "\", " +
                "\"avg\": \"" + StringEscapeUtils.escapeJava(timeComplexity.get("avg")) + "\", " +
                "\"worst\": \"" + StringEscapeUtils.escapeJava(timeComplexity.get("worst")) + "\"}, " +
                "\"spaceComplexity\": \"" + StringEscapeUtils.escapeJava(spaceComplexity) + "\", " +
                "\"stable\": " + stable + "}";
    }
}
