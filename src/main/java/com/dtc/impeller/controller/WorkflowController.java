package com.dtc.impeller.controller;

import java.util.List;

import com.dtc.impeller.entity.Workflow;
import com.dtc.impeller.service.WorkflowService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/workflow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowController {
    @Inject
    private WorkflowService workflowService;

    @POST
    public Response createWorkflow(Workflow workflow) {
        Workflow created = workflowService.createWorkflow(workflow);
        return Response.status(Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/{id}")
    public Response getWorkflow(@PathParam("id") Long id) {
        Workflow workflow = workflowService.getWorkflow(id);
        if (workflow == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(workflow).build();
    }

    @GET
    public List<Workflow> getAllWorkflows() {
        return workflowService.getAllWorkflows();
    }

    @PUT
    @Path("/{id}")
    public Response updateWorkflow(@PathParam("id") Long id, Workflow workflow) {
        Workflow updated = workflowService.updateWorkflow(id, workflow);
        if (updated == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteWorkflow(@PathParam("id") Long id) {
        boolean deleted = workflowService.deleteWorkflow(id);
        if (!deleted) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
