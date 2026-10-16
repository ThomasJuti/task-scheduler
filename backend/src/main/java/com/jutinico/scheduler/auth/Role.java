package com.jutinico.scheduler.auth;

public enum Role {
    /** Full access: CRUD of tasks and workers, auto-assignment, data reset. */
    ADMIN,
    /** Read access plus "moving" tasks: change status and assign / reassign / unassign. */
    USER
}
