package com.bankops.common;

import org.springframework.http.HttpStatus;

public class WorkspaceException extends RuntimeException {
    private final HttpStatus status;
    public WorkspaceException(HttpStatus status, String message) { super(message); this.status=status; }
    public HttpStatus status() { return status; }
    public static WorkspaceException missing() { return new WorkspaceException(HttpStatus.NOT_FOUND,"Resource not found."); }
    public static WorkspaceException conflict(String message) { return new WorkspaceException(HttpStatus.CONFLICT,message); }
}
