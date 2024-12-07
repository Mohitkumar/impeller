package com.dtc.impeller.entity;

import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_WORKFLOW")
public class Workflow {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "definition",columnDefinition = "text", nullable = false)
    private String definition;

	@Column(name = "create_date", nullable = false)
	private Date createDate;
	
	@Column(name = "update_date", nullable = true)
	private Date updateDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    
}
