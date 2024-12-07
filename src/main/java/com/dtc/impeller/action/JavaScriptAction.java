package com.dtc.impeller.action;

import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import com.dtc.impeller.flow.ActionParam;
import com.dtc.impeller.flow.Result;

public class JavaScriptAction extends AbstractAction {
    protected Context context;

    protected Value bindings;

    @ActionParam
    private String script;

    @ActionParam
    protected Map<String,Object> data;

    public JavaScriptAction(String name, int retryCount) {
        super(name, retryCount);
        context = Context.newBuilder("js")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        bindings = context.getBindings("js");
    }

    @Override
    public Result<?, ? extends Throwable> run() {
        data.forEach((k,v)-> bindings.putMember(k,v));
        try {
            context.eval("js", script);
            Value value = bindings.getMember("output");
            Object out = null;
            if(value.isString()){
                out = value.asString();
            } else if (value.isBoolean()) {
                out = value.asBoolean();
            } else if (value.isNumber()) {
                double v = value.asDouble();
                if(v % 1 == 0){
                    out = value.asLong();
                }else {
                    out = value.asDouble();
                }
            } else if(value.hasArrayElements()){
                out = value.as(List.class);
            }else {
                out = value.as(Map.class);
            }
            return Result.ok(out);
        }catch (Exception e){
            context.close();
            context = Context.newBuilder("js")
                    .allowAllAccess(true)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();
            bindings = context.getBindings("js");
            return Result.error(e);
        }
    }
}
