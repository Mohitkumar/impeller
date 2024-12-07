package com.dtc.impeller.service;

import java.util.List;

import com.dtc.impeller.entity.Workflow;
import com.dtc.impeller.repository.WorkflowRepository;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WorkflowService {

    @Inject
    private WorkflowRepository workflowRepository;

    public Workflow createWorkflow(Workflow workflow) {
        workflowRepository.persist(workflow);
        return workflow;
    }

    public Workflow getWorkflow(Long id) {
        return workflowRepository.findById(id);
    }

    public List<Workflow> getAllWorkflows() {
        return workflowRepository.listAll();
    }

    public Workflow updateWorkflow(Long id, Workflow workflow) {
        Workflow existingWorkflow = workflowRepository.findById(id);
        if (existingWorkflow == null) {
            return null;
        }
        
        workflow.setId(id);
        workflowRepository.persist(workflow);
        return workflow;
    }

    public boolean deleteWorkflow(Long id) {
        return workflowRepository.deleteById(id);
    }
}
