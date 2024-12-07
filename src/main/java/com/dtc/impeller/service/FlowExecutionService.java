package com.dtc.impeller.service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dtc.impeller.flow.ActionGraph;
import com.dtc.impeller.flow.Context;
import com.dtc.impeller.flow.FlowInstance;
import com.dtc.impeller.model.ActionDefinition;
import com.dtc.impeller.model.Workflow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FlowExecutionService {

    private JsonMapper mapper = new JsonMapper();


    public FlowInstance createFlow(String workflowDefinition){
        try {
            Workflow workflow = mapper.readValue(workflowDefinition, Workflow.class);
            return buildFlow(workflow);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("workflow definition is not valid");
        }
    }

    private FlowInstance buildFlow(Workflow workflow){
        ActionGraph actionGraph = new ActionGraph();
        List<ActionDefinition> actions = workflow.getActions();
        Map<String, ActionGraph.Node> actionMap = new HashMap<>();
        for (ActionDefinition action : actions) {
            actionMap.put(action.getName(), new ActionGraph.Node(action.getName(),action.getParameters(), action.getImpl(),action.getRetryCount()));
        }
        actionGraph.setRoot(actionMap.get(workflow.getRoot()));
        actionGraph.setActionMap(actionMap);
        actionGraph.setName(workflow.getCarrier() +"_" + workflow.getType());
        for (ActionDefinition action : actions) {
            Map<String, List<String>> next = action.getNext();
            if(next == null){
                continue;
            }
            ActionGraph.Node current = actionMap.get(action.getName());
            next.forEach((k, v) ->{
                current.getNextMap()
                        .put(k, v.stream().map(actionMap::get).collect(Collectors.toList()));
            });
        }
        FlowInstance flow = new FlowInstance(actionGraph);
        if(!flow.validate()){
            throw new RuntimeException("workflow definition is not valid");
        }
        return flow;
    }
    

    public Context createContext(Object data, Map<String,Object> config){
        Context context = new Context(data, config);
        return context;
    }
}
