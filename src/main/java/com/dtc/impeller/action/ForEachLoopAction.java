package com.dtc.impeller.action;


import java.util.List;
import java.util.Map;

import com.dtc.impeller.flow.ActionParam;
import com.dtc.impeller.flow.Result;

public class ForEachLoopAction extends AbstractAction{

    @ActionParam
    private String loopVariable;

    @ActionParam
    private List<Object> loopOn;

    @ActionParam
    private String action;

    public ForEachLoopAction(String name, int retryCount) {
        super(name, retryCount);
    }

    @Override
    public Result<?, ? extends Throwable> run() {
        return Result.ok(Map.of("loopVariable", loopVariable, "loopOn" , loopOn, "action", action));
    }
}
