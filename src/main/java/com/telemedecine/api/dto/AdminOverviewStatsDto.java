package com.telemedecine.api.dto;

public record AdminOverviewStatsDto(
        long totalUsers,
        long totalDoctors,
        long pendingDoctors,
        long confirmedDoctors,
        long rejectedDoctors,
        long totalPatients,
        long totalAppointments
) {
}
