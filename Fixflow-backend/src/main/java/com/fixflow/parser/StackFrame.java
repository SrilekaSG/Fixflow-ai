package com.fixflow.parser;

public record StackFrame(
        String className,
        String methodName,
        String fileName,
        Integer lineNumber,
        boolean applicationCode) {
}