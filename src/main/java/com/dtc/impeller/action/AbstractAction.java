package com.dtc.impeller.action;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtc.impeller.flow.Action;
import com.dtc.impeller.flow.ActionParam;

public abstract class AbstractAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAction.class);

    @ActionParam(optional = true)
    boolean debug;

    private String name;

    private int retryCount;

    public AbstractAction(String name, int retryCount) {
        this.name = name;
        this.retryCount = retryCount;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    protected void debugMessage(String message, Map<String,Object> data){
        try{
            if(debug){
                LOGGER.info(message, data.values());
            }
        }catch (Exception e){
            //ignore - we do not want to break the normal code flow just for logging
        }
    }
}
