package com.dtc.impeller.action;

import com.dtc.impeller.flow.ActionParam;
import com.dtc.impeller.flow.Result;

public class SwitchAction extends AbstractAction {

    @ActionParam
    private String expression;

    public SwitchAction(String name, int retryCount) {
        super(name, retryCount);
    }

    @Override
    public Result<?, ? extends Throwable> run() {
        return Result.ok(expression);
    }

}
