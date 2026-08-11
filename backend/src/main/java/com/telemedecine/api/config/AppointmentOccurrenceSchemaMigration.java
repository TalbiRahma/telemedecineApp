package com.telemedecine.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Compatibility migration for databases created while Appointment.slot was one-to-one. */
@Component
@RequiredArgsConstructor
public class AppointmentOccurrenceSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String constraintQuery =
                "select tc.constraint_name " +
                "from information_schema.table_constraints tc " +
                "join information_schema.key_column_usage kcu " +
                "on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema " +
                "where tc.table_schema = current_schema() and tc.table_name = 'appointment' " +
                "and tc.constraint_type = 'UNIQUE' group by tc.constraint_name " +
                "having count(*) = 1 and max(kcu.column_name) = 'slot_id'";
        List<String> legacyConstraints = jdbcTemplate.queryForList(constraintQuery, String.class);

        for (String constraint : legacyConstraints) {
            if (!constraint.matches("[A-Za-z0-9_]+")) {
                throw new IllegalStateException("Unexpected database constraint name");
            }
            jdbcTemplate.execute("alter table appointment drop constraint " + constraint);
        }

        jdbcTemplate.update(
                "update appointment appointment " +
                "set scheduled_start = availability.date + slot.start_time, " +
                "scheduled_end = availability.date + slot.end_time " +
                "from slot slot join doctor_availability availability " +
                "on availability.id = slot.availability_id " +
                "where appointment.slot_id = slot.id and availability.date is not null " +
                "and appointment.scheduled_start is null");
    }
}
