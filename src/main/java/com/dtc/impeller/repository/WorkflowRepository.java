package com.dtc.impeller.repository;

import com.dtc.impeller.entity.Workflow;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkflowRepository implements PanacheRepository<Workflow>{
    
}
