package com.dtc.impeller.model;

import java.util.List;
import java.util.Map;

public class ActionDefinition {
    private String name;

    private String impl;

    private Map<String, Object> parameters;

    private Map<String, List<String>> next;

    private int retryCount;

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImpl() {
        return impl;
    }

    public void setImpl(String impl) {
        this.impl = impl;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, List<String>> getNext() {
        return next;
    }

    public void setNext(Map<String, List<String>> next) {
        this.next = next;
    }
}
