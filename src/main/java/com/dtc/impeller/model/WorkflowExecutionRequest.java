package com.dtc.impeller.model;

import java.util.Map;

public class WorkflowExecutionRequest {
    private String name;

    private Workflow definition;

    private Map<String, Object> input;

    public Workflow getDefinition() {
        return definition;
    }

    public void setDefinition(Workflow definition) {
        this.definition = definition;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
