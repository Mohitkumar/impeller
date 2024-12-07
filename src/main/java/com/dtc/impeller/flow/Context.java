package com.dtc.impeller.flow;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Context implements Closeable {
    private Value bindings;

    private org.graalvm.polyglot.Context jsCtx;

    private Object input;

    private Map<String,Object> config;

    private Map<String, Map<String, Object>> actionOutput = new HashMap<>();

    private ActionGraph.Node joinAction;

    private Map<String, Object> extraData;

    public Context(Object input) {
        this.input = input;
        jsCtx = org.graalvm.polyglot.Context.newBuilder("js")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        bindings = jsCtx.getBindings("js");
        bindings.putMember("input",input);
    }

    public Context(Object input,
                   Map<String,Object> config) {
        this.input = input;
        this.config = config;
        jsCtx = org.graalvm.polyglot.Context.newBuilder("js")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        bindings = jsCtx.getBindings("js");
        bindings.putMember("input",input);
        bindings.putMember("config", config);
    }

    public ActionGraph.Node  getJoinAction() {
        return joinAction;
    }

    public void setJoinAction(ActionGraph.Node  joinAction) {
        this.joinAction = joinAction;
    }

    public Map<String, Object> getActionOutput(String actionName){
        return actionOutput.get(actionName);
    }

    public void addActionOutput(String name, Map<String,Object> output){
        actionOutput.put(name, output);
        bindings.putMember(name, output);
    }

    public String toJson(){
        JsonMapper mapper = new JsonMapper();
        ObjectNode root = mapper.createObjectNode();
        root.putPOJO("input", this.input);
        root.putPOJO("config", config);
        actionOutput.forEach((o, data) ->{
            root.putPOJO(o, data);
        });
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Value eval(String expression){
        return jsCtx.eval("js", expression);
    }
    @Override
    public void close() throws IOException {
        try{
            jsCtx.close();
        }catch (Exception e){
            throw new IOException(e);
        }
    }

    public Context copy(){
        Context context = new Context(this.input, this.config);
        context.actionOutput.putAll(this.actionOutput);
        context.actionOutput.forEach((k,v) -> context.bindings.putMember(k,v));
        context.joinAction = this.joinAction;
        return context;
    }

    public void merge(List<Context> contexts){
        for (Context toMerge : contexts) {
            if (toMerge.getJoinAction() != null) {
                this.setJoinAction(toMerge.getJoinAction());
            }
            this.actionOutput.putAll(toMerge.actionOutput);
            toMerge.actionOutput.forEach((k, v) -> this.bindings.putMember(k, v));
        }
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    public Object getInput() {
        return input;
    }
}
