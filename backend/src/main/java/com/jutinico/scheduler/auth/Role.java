package com.jutinico.scheduler.auth;

public enum Role {
    /** Acceso total: CRUD de tareas y trabajadores, asignación automática, reinicio de datos. */
    ADMIN,
    /** Lectura más "mover" tareas: cambiar estado y asignar / reasignar / desasignar. */
    USER
}
