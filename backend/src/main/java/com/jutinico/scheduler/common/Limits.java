package com.jutinico.scheduler.common;

/**
 * Size and range limits of the request fields, used by the DTO validation annotations. The database constraints
 * ({@code V1__init.sql}) and the frontend forms ({@code frontend/src/app/core/validation/limits.ts}) mirror them.
 */
public final class Limits {

    public static final int TASK_NAME_MAX = 120;
    public static final int WORKER_NAME_MAX = 120;
    public static final int EFFORT_MIN = 1;
    public static final int EFFORT_MAX = 1000;
    public static final int AVAILABILITY_MIN = 1;
    public static final int AVAILABILITY_MAX = 24;
    public static final int TASK_SKILLS_MAX = 20;
    public static final int WORKER_SKILLS_MAX = 30;
    public static final int SKILL_NAME_MAX = 50;
    public static final int TASKS_PER_ASSIGNMENT_MAX = 100;

    private Limits() {
    }
}
