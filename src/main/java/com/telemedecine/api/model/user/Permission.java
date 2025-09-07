package com.telemedecine.api.model.user;

import lombok.Getter;

public enum Permission {
    // Patient permissions
    PATIENT_READ("patient:read"),
    PATIENT_UPDATE("patient:update"),
    PATIENT_CREATE("patient:create"),
    PATIENT_DELETE("patient:delete"),

    // Appointment permissions
    APPOINTMENT_CREATE("appointment:create"),
    APPOINTMENT_READ("appointment:read"),
    APPOINTMENT_CANCEL("appointment:cancel"),
    APPOINTMENT_MANAGE("appointment:manage"),

    // Medical records
    MEDICAL_RECORD_READ("medical_record:read"),
    MEDICAL_RECORD_UPDATE("medical_record:update"),

    // Prediagnostic
    PREDIAGNOSTIC_CREATE("prediagnostic:create"),
    PREDIAGNOSTIC_READ("prediagnostic:read"),

    // Consultation
    CONSULTATION_CREATE("consultation:create"),
    CONSULTATION_JOIN("consultation:join"),

    // Prescription
    PRESCRIPTION_CREATE("prescription:create"),

    // Doctor permissions
    DOCTOR_READ("doctor:read"),
    DOCTOR_UPDATE("doctor:update"),
    DOCTOR_CREATE("doctor:create"),
    DOCTOR_DELETE("doctor:delete"),


    // Admin permissions
    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),
    USER_MANAGE("user:manage"),
    DOCTOR_MANAGE("doctor:manage"),
    SPECIALTY_MANAGE("specialty:manage"),
    SYSTEM_CONFIG_UPDATE("system_config:update");

    @Getter
    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }
}