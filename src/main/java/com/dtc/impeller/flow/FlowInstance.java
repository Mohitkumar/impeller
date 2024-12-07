package com.dtc.impeller.flow;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.util.Base64;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtc.impeller.action.ForEachLoopAction;
import com.dtc.impeller.action.SwitchAction;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class FlowInstance{
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowInstance.class);

    private final ActionGraph actionGraph;

    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    public FlowInstance(ActionGraph actionGraph) {
        this.actionGraph = actionGraph;
    }

    public void execute(Context context) throws FlowFailedException {
        try{
            LOGGER.info("starting flow {}", actionGraph.getName());
            execute(actionGraph.getRoot(), context);
            LOGGER.info("completed flow {}, status {}", actionGraph.getName(), "SUCCESS");
        }catch (FlowFailedException e){
            e.printStackTrace();
            LOGGER.info("completed flow {}, status {}", actionGraph.getName(), "FAILED");
            throw e;
        } catch (Exception e){
            e.printStackTrace();
            throw new FlowFailedException(e);
        }finally {
            try {
                context.close();
            } catch (IOException e) {
                //ignore
            }
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    private void execute(ActionGraph.Node node, Context context) throws FlowFailedException{
        Action action = createAction(node, context);
        execute(action, context);
        Map<String, List<ActionGraph.Node>> nextMap = node.getNextMap();
        List<ActionGraph.Node> defaultNextActions;
        if(action instanceof SwitchAction){
            Map<String, Object> switchOutput = context.getActionOutput(action.getName());
            String aCase = (String)switchOutput.get("output");
            defaultNextActions = nextMap.get(aCase);
        } else if (action instanceof ForEachLoopAction) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actionOutput = (Map<String, Object>)context.getActionOutput(action.getName()).get("output");
            String loopVariable = (String) actionOutput.get("loopVariable");
            @SuppressWarnings("unchecked")
            List<Object> loopOn = (List<Object>) actionOutput.get("loopOn");
            String actionToCall = (String) actionOutput.get("action");
            ActionGraph.Node actionNode = actionGraph.getNode(actionToCall);
            for (Object o : loopOn) {
                context.addActionOutput(action.getName(), Map.of(loopVariable, o));
                execute(actionNode, context);
            }
            defaultNextActions = nextMap.get("default");
        } else{
            defaultNextActions = nextMap.get("default");
        }
        if(nextMap.get("joinOn") != null){
            ActionGraph.Node joinOn = (ActionGraph.Node) nextMap.get("joinOn").get(0);
            context.setJoinAction(joinOn);
            return;
        }
        if(defaultNextActions == null){
            return;
        }
        if(defaultNextActions.size() == 1){
            execute(defaultNextActions.get(0), context);
        } else if (defaultNextActions.size() > 1) {
            List<Context> threadContexts = new ArrayList<>();
            List<Callable<Boolean>> callabels = new ArrayList<>();
            for (ActionGraph.Node defaultNextAction : defaultNextActions) {
                Context contextCopy = context.copy();
                threadContexts.add(contextCopy);
                callabels.add(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        execute(defaultNextAction, contextCopy);
                        return true;
                    }
                });
            }
            try {
                List<Future<Boolean>> results = executorService.invokeAll(callabels);
                for (Future<Boolean> result : results) {
                    result.get();
                }
                context.merge(threadContexts);
                if(context.getJoinAction() != null){
                    ActionGraph.Node currentJonAction = context.getJoinAction();
                    context.setJoinAction(null);
                    execute(currentJonAction, context);
                }
            } catch (ExecutionException e) {
                throw new FlowFailedException(e);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }
    private void  execute(Action action,  Context context) throws FlowFailedException{
        if(action instanceof SwitchAction || action instanceof ForEachLoopAction){
            executeWithoutRetry(action,context);
        }else{
             executeWithRetry(action, context);
        }
    }

    private void executeWithoutRetry(Action action,  Context context) throws FlowFailedException{
        LOGGER.info("running action {}", action.getName());
        Result<?, ? extends Throwable> result = action.run();
        if(result.isOk()){
            context.addActionOutput(action.getName(), result.ok());
            LOGGER.info("action {}, output {}", action.getName(),result.ok());
        }else {
            throw new FlowFailedException(result.error());
        }
    }
    private void executeWithRetry(Action action, Context context) throws FlowFailedException{
        int retryCount = action.getRetryCount();
        int count = 0;
        Result<?, ? extends Throwable> executionResult;
        do{
            LOGGER.info("running action {}", action.getName());
            long startTime = System.currentTimeMillis();
            executionResult = action.run();
            if(executionResult.isOk()){
                LOGGER.info("Success- action {}, output {} timeTaken {}", action.getName(),executionResult.ok(), System.currentTimeMillis()-startTime);
                context.addActionOutput(action.getName(), executionResult.ok());
                return;
            }else {
                executionResult.error().printStackTrace();
                LOGGER.info("Failed- action {}, error {} timeTaken {}, retry {}", action.getName(),executionResult.error(), System.currentTimeMillis()-startTime, count);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //ignore
            }
            count++;
        }while (count < retryCount);
        throw new FlowFailedException(executionResult.error());
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String,Object> resolveParams(Map<String, Object> parameters, Context context){
        Map<String, Object> out = new HashMap<>();
        parameters.forEach((name, p) ->{
            if(p instanceof String){
                if(((String) p).startsWith("{{") && ((String) p).endsWith("}}")){
                    Value value = context.eval(getExpression((String)p));
                    if(value.isString()){
                        out.put(name, value.asString());
                    } else if (value.isNumber()) {
                        double v = value.asDouble();
                        if(v % 1 == 0){
                            out.put(name, value.asLong());
                        }else {
                            out.put(name, value.asDouble());
                        }
                    }else if (value.isBoolean()) {
                        out.put(name, value.asBoolean());
                    }else if(value.hasArrayElements()){
                        out.put(name, value.as(List.class));
                    }else {
                        out.put(name, value.as(Map.class));
                    }
                } else if (((String) p).startsWith("$") ) {
                    try{
                        Object pathContent = JsonPath.read(context.toJson(), (String) p);
                        out.put(name, pathContent);
                    }catch (PathNotFoundException e){
                        LOGGER.warn("json path not present {}",e.getMessage());
                    }
                } else{
                    out.put(name, p);
                }
            } else if (p instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> internalOut = resolveParams((Map<String, Object>) p, context);
                out.put(name, internalOut);
            } else if (p instanceof List) {
                resolveParams((List)p, context);
                out.put(name, p);
            }else{
                out.put(name, p);
            }
        });
        return out;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void resolveParams(List<Object> parameters, Context context){
        for (int i = 0; i < parameters.size(); i++) {
            Object p = parameters.get(i);
            if(p instanceof String){
                if(((String) p).startsWith("{{") && ((String) p).endsWith("}}")){
                    Value value = context.eval(getExpression((String)p));
                    if(value.isString()){
                        parameters.set(i, value.asString());
                    } else if (value.isNumber()) {
                        double v = value.asDouble();
                        if(v % 1 == 0){
                            parameters.set(i, value.asLong());
                        }else {
                            parameters.set(i, value.asDouble());
                        }
                    }else if (value.isBoolean()) {
                        parameters.set(i, value.asBoolean());
                    }else if(value.hasArrayElements()){
                        parameters.set(i, value.as(List.class));
                    }else {
                        parameters.set(i, value.as(Map.class));
                    }
                }else if (((String) p).startsWith("$") ) {
                    try{
                        Object pathContent = JsonPath.read(context.toJson(), (String) p);
                        parameters.set(i, pathContent);
                    }catch (PathNotFoundException e){
                        LOGGER.warn("json path not present {}",e.getMessage());
                    }
                }else{
                    parameters.set(i, p);
                }
            } else if (p instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> internalOut = resolveParams((Map<String, Object>) p, context);
                parameters.set(i, internalOut);
            } else if (p instanceof List) {
                resolveParams((List)p, context);
            }
        }
    }

    private String getExpression(String s){
        return s.substring(2,s.length()-2);
    }

    private Action createAction(ActionGraph.Node node, Context context){
        LOGGER.info("creating action {} impl {}", node.getName(), node.getImpl());
        Map<String, Object> parameters = node.getParams();
        Map<String, Object> resolvedParams = resolveParams(parameters, context);
        String impl = node.getImpl();
        try {
            Class<?> aClass = Class.forName(impl);
            Action action = (Action)aClass.getConstructor(String.class, int.class)
                    .newInstance(node.getName(), node.getRetryCount());
            List<Field> actionParams = getActionParams(aClass, true);
            for (Field field : actionParams) {
                String name = field.getName();
                Object value = resolvedParams.get(name);
                if(value == null && field.getAnnotation(ActionParam.class).optional()){
                    continue;
                }
                if(!field.getAnnotation(ActionParam.class).optional() && value == null){
                    throw new RuntimeException("field "+ name + " not present in action definition");
                }
                if(field.getType() == Long.class || field.getType() == long.class){
                    value = Long.valueOf(value.toString());
                }else if(field.getType() == Integer.class || field.getType() == int.class){
                    value = Integer.valueOf(value.toString());
                }else if(field.getType() == Double.class || field.getType() == double.class){
                    value = Double.valueOf(value.toString());
                }else if(field.getType() == byte[].class && value instanceof String){
                    value = Base64.decodeBase64(((String)value).getBytes());
                }
                field.setAccessible(true);
                field.set(action,value);
            }
            return action;
        } catch (Exception e) {
            LOGGER.error("error creating action {} error {}", node.getImpl(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean validate(){
        ActionGraph.Node root = actionGraph.getRoot();
        return validate(root);
    }

    private boolean validate(ActionGraph.Node node){
        String impl = node.getImpl();
        Map<String, Object> params = node.getParams();
        try{
            Class<?> aClass = Class.forName(impl);
            List<Field> actionParams = getActionParams(aClass, false);
            for (Field field : actionParams) {
                if(!params.containsKey(field.getName())){
                    LOGGER.info("class {} field {} not present in action definition",aClass, field.getName());
                    return false;
                }
            }
        }catch (Exception e){
            LOGGER.error("error validating workflow",e);
            return false;
        }
        Map<String, List<ActionGraph.Node>> nextMap = node.getNextMap();
        for (Map.Entry<String, List<ActionGraph.Node>> entry : nextMap.entrySet()) {
            List<ActionGraph.Node> nodes = entry.getValue();
            for (ActionGraph.Node nextNode : nodes) {
                if(!validate(nextNode)){
                    return false;
                }
            }
        }
        return true;
    }
    
    private List<Field> getActionParams(Class<?> aClass, boolean optional){
        List<Field> fields = new ArrayList<>();
        for (Class<?> acls = aClass; acls != null; acls = acls.getSuperclass()) {
            try {
                Field[] declaredFields = acls.getDeclaredFields();
                for (Field field : declaredFields) {
                    if(field.isAnnotationPresent(ActionParam.class)){
                        ActionParam annotation = field.getAnnotation(ActionParam.class);
                        if(optional || !annotation.optional()){
                            fields.add(field);
                        }
                    }
                }
            } catch (Exception e){
                //ignore
            }
        }
        return fields;
    }
}
