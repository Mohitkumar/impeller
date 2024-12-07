package com.dtc.impeller.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionGraph {

    private String name;

    private Node root;

    private Map<String, ActionGraph.Node> actionMap = new HashMap<>();

    public Node getRoot() {
        return root;
    }

    public Node getNode(String name){
        return actionMap.get(name);
    }
    public void setRoot(Node root) {
        this.root = root;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setActionMap(Map<String, Node> actionMap) {
        this.actionMap = actionMap;
    }

    public static class Node {
        String name;

        int retryCount;

        Map<String, Object> params;

        private String impl;

        public Node(String name, Map<String, Object> params, String impl,int retryCount) {
            this.name = name;
            this.params = params;
            this.retryCount = retryCount;
            this.impl = impl;
            this.nextMap = new HashMap<>();
        }

        Map<String, List<Node>> nextMap;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        public String getImpl() {
            return impl;
        }

        public void setImpl(String impl) {
            this.impl = impl;
        }

        public Map<String, List<Node>> getNextMap() {
            return nextMap;
        }

        public void setNextMap(Map<String, List<Node>> nextMap) {
            this.nextMap = nextMap;
        }
    }
}
