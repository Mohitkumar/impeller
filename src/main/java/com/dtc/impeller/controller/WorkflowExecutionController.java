package com.dtc.impeller.controller;

import com.dtc.impeller.flow.Context;
import com.dtc.impeller.flow.FlowFailedException;
import com.dtc.impeller.flow.FlowInstance;
import com.dtc.impeller.model.FlowExecutionResponse;
import com.dtc.impeller.model.WorkflowExecutionRequest;
import com.dtc.impeller.service.FlowExecutionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Path("/workflow/execute")
@ApplicationScoped
public class WorkflowExecutionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowExecutionController.class);

    @Inject
    private FlowExecutionService flowExecutionService;


    @POST
    public Response executeWorkflow(WorkflowExecutionRequest workflowExecutionRequest) {
        try {
            FlowInstance flowInstance = flowExecutionService.buildFlow(workflowExecutionRequest.getName(),
                workflowExecutionRequest.getDefinition());
            Context context = new Context(workflowExecutionRequest.getInput(), Map.of("test", "test"));
            FlowExecutionResponse response = flowInstance.execute(context);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOGGER.error("error executing workflow", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity(e.getMessage())
                         .build();
        }
    }
}
