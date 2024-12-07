package com.dtc.impeller.action;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import com.dtc.impeller.flow.ActionParam;
import com.dtc.impeller.flow.Result;

public class XMLMapperAction extends AbstractAction{

    protected Context context;

    protected Value bindings;

    @ActionParam
    Map<String,Object> data;

    @ActionParam
    String scriptFile ;

    public XMLMapperAction(String name, int retryCount) {
        super(name, retryCount);
        context = Context.newBuilder("js")
                .allowAllAccess(true)
                .build();
    }

    @Override
    public Result<?, ? extends Throwable> run() {
        bindings = context.getBindings("js");
        data.forEach((k,v)-> bindings.putMember(k,v));
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        String path = "mappings/" + scriptFile;
        try (InputStream is = classloader.getResourceAsStream(path)) {
            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Result.ok(parseXml(xml,data));
        }catch (Exception e){
            return Result.error(e);
        }
    }

    private String parseXml(String xml, Map<String, Object> input){
        return xml;
    }
}
