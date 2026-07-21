package com.iams.common.security;

/** US-SEC-06 (AC-SEC-06-X): a step-up-required action was invoked without a recent step-up confirmation. */
public class StepUpRequiredException extends RuntimeException {

    public StepUpRequiredException() {
        super("This action requires you to re-enter your password first");
    }
}
