package com.dtc.impeller.flow;

public interface Action {

    String getName();

    int getRetryCount();

    Result<?, ? extends Throwable> run();
}
