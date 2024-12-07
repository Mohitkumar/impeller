package com.dtc.impeller.controller;

import java.util.Map;

import com.dtc.impeller.entity.Workflow;
import com.dtc.impeller.flow.Context;
import com.dtc.impeller.flow.FlowFailedException;
import com.dtc.impeller.flow.FlowInstance;
import com.dtc.impeller.service.FlowExecutionService;
import com.dtc.impeller.service.WorkflowService;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/workflow/execute")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class WorkflowExecutionController {
    @Inject
    private FlowExecutionService flowExecutionService;

    @Inject
    private WorkflowService workflowService;

    @POST
    @Path("/{id}")
    public Response executeWorkflow(@PathParam("id") Long id, Map<String, Object> input) {
        try {
            Workflow workflow = workflowService.getWorkflow(id);
            if (workflow == null) {
                return Response.status(Status.NOT_FOUND).build();
            }

            FlowInstance flowInstance = flowExecutionService.createFlow(workflow.getDefinition());
            Context context = new Context(input, Map.of("test", "test"));
            flowInstance.execute(context);
            return Response.ok().build();
        } catch (FlowFailedException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                         .entity(e.getMessage())
                         .build();
        }
    }
}
