package com.dtc.impeller.action;

import com.dtc.impeller.flow.ActionParam;
import com.dtc.impeller.flow.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

public class JSONToXMLAction extends AbstractAction{
    private static final Logger logger = LoggerFactory.getLogger(JSONToXMLAction.class);

    @ActionParam
    Map<String, Object> jsonObj;

    public JSONToXMLAction(Map<String, Object> jsonObj, String name, int retryCount) {
        super(name, retryCount);
        this.jsonObj = jsonObj;
    }

    public JSONToXMLAction(String name, int retryCount) {
        super(name, retryCount);
    }

    @Override
    public Result<?, ? extends Throwable> run() {
        try {
            String xml = objToXML(jsonObj);
            return Result.ok(xml);
        } catch (Exception e) {
            logger.error("Failed to convert JSON to XML", e);
            return Result.error(e);
        }
    }

    public static String objToXML(Map<String, Object> obj) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(objToXMLNode(obj));
        return xml.toString();
    }

    @SuppressWarnings("unchecked")
    private static String objToXMLNode(Map<String, Object> obj) {
        StringBuilder xml = new StringBuilder();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String prop = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                xml.append("<").append(prop).append("/>");
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    xml.append("<").append(prop).append(">");
                    if (item instanceof Map) {
                        xml.append(objToXMLNode((Map<String, Object>) item));
                    } else {
                        xml.append(alphaNumericOnly(String.valueOf(item)));
                    }
                    xml.append("</").append(prop).append(">");
                }
            } else if (value instanceof Map) {
                xml.append("<").append(prop).append(">");
                xml.append(objToXMLNode((Map<String, Object>) value));
                xml.append("</").append(prop).append(">");
            } else {
                xml.append("<").append(prop).append(">");
                xml.append(alphaNumericOnly(String.valueOf(value)));
                xml.append("</").append(prop).append(">");
            }
        }
        return xml.toString();
    }

    private static String alphaNumericOnly(String str) {
        // Remove all characters except letters, numbers, whitespace, and (@ # * + . - :)
        return str.replaceAll("[^a-zA-Z0-9\\s@#*.+-:]", "");
    }

    public static void main(String[] args) {
        Map<String, Object> exampleMap = Map.of(
            "name", "John Doe",
            "age", 30,
            "email", "john.doe@example.com",
            "tags", List.of("developer", "java", "xml"),
            "address", Map.of(
                "street", "123 Main St",
                "city", "Anytown",
                "zipcode", "12345"
            )
        );
        
        String xml = objToXML(exampleMap);
        System.out.println(xml);
    }
}
