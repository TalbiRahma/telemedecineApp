package com.telemedecine.api.config;

import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.DoctorAvailabilityRepository;
import com.telemedecine.api.dao.AppointmentRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.SlotRepository;
import com.telemedecine.api.dao.SpecialtyRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.user.Admin;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.slot.SlotStatus;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.appointement.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${app.seed.admin-email:}")
    private String adminEmail;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.default-password:}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();
        log.info("MediLink database seed started");

        Map<String, Specialty> specialties = seedSpecialties();
        int adminsCreated = seedSuperAdmin();
        int doctorsCreated = seedDoctors(specialties);
        int patientsCreated = seedPatients();
        SeedCount availabilities = seedDoctorAvailabilities();
        SeedCount appointments = seedAppointments();

        log.info("MediLink database seed completed: {} specialties available, {} super admin, {} doctors, "
                        + "{} patients, {} availability rules, {} appointments created ({} availabilities and {} appointments skipped)",
                specialties.size(),
                adminsCreated, doctorsCreated, patientsCreated, availabilities.created(), appointments.created(),
                availabilities.skipped(), appointments.skipped());
    }

    private void validateConfiguration() {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("SEED_ADMIN_EMAIL is required when SEED_ENABLED=true");
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("SEED_ADMIN_PASSWORD is required when SEED_ENABLED=true");
        }
        if (defaultPassword == null || defaultPassword.isBlank()) {
            throw new IllegalStateException("SEED_DEFAULT_PASSWORD is required when SEED_ENABLED=true");
        }
    }

    private Map<String, Specialty> seedSpecialties() {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("General Medicine", "Comprehensive primary care, prevention, diagnosis, and treatment.");
        definitions.put("Cardiology", "Diagnosis and treatment of cardiovascular diseases.");
        definitions.put("Dermatology", "Diagnosis and treatment of conditions affecting skin, hair, and nails.");
        definitions.put("Neurology", "Diagnosis and treatment of disorders of the brain and nervous system.");
        definitions.put("Pediatrics", "Preventive and medical care for infants, children, and adolescents.");
        definitions.put("Gynecology", "Care for women's reproductive and gynecological health.");
        definitions.put("Psychiatry", "Diagnosis, treatment, and prevention of mental health conditions.");
        definitions.put("Ophthalmology", "Medical and surgical care for eye and vision conditions.");
        definitions.put("Orthopedics", "Care for bones, joints, muscles, ligaments, and related injuries.");
        definitions.put("Gastroenterology", "Diagnosis and treatment of digestive system disorders.");
        definitions.put("Endocrinology", "Care for hormonal, metabolic, thyroid, and diabetic conditions.");
        definitions.put("ENT", "Care for ear, nose, throat, and related head and neck conditions.");

        Map<String, Specialty> result = new LinkedHashMap<>();
        int created = 0;
        for (Map.Entry<String, String> entry : definitions.entrySet()) {
            Specialty specialty = specialtyRepository.findByNameIgnoreCase(entry.getKey()).orElse(null);
            if (specialty == null) {
                specialty = Specialty.builder().name(entry.getKey()).description(entry.getValue()).build();
                specialty = specialtyRepository.save(specialty);
                created++;
            } else {
                log.info("Specialty {} already exists - skipped", entry.getKey());
            }
            result.put(entry.getKey(), specialty);
        }
        log.info("Created {} specialties; skipped {} existing specialties", created, definitions.size() - created);
        return result;
    }

    private int seedSuperAdmin() {
        String email = normalizeEmail(adminEmail);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Super admin {} already exists - skipped", email);
            return 0;
        }

        Admin admin = new Admin();
        initializeUser(admin, "MediLink", "Administrator", email, adminPassword, Role.ADMIN, "+216 70 000 001");
        admin.setAdminLevel(Admin.AdminLevel.SUPER_ADMIN);
        admin.setDepartment("Platform Administration");
        admin.setCanManageUsers(true);
        admin.setCanManageSystem(true);
        userRepository.save(admin);
        log.info("Created seeded super admin");
        return 1;
    }

    private int seedDoctors(Map<String, Specialty> specialties) {
        List<DoctorSeed> doctors = List.of(
                new DoctorSeed("Amine", "Ben Salah", "Cardiology", "Tunis", DoctorState.CONFIRMED),
                new DoctorSeed("Sarra", "Trabelsi", "Dermatology", "Sousse", DoctorState.CONFIRMED),
                new DoctorSeed("Youssef", "Mansouri", "Neurology", "Sfax", DoctorState.CONFIRMED),
                new DoctorSeed("Mariem", "Jlassi", "Pediatrics", "Monastir", DoctorState.CONFIRMED),
                new DoctorSeed("Ahmed", "Gharbi", "General Medicine", "Nabeul", DoctorState.CONFIRMED),
                new DoctorSeed("Ines", "Bouazizi", "Gynecology", "Tunis", DoctorState.CONFIRMED),
                new DoctorSeed("Mohamed", "Ayari", "Orthopedics", "Kairouan", DoctorState.CONFIRMED),
                new DoctorSeed("Rania", "Mejri", "Ophthalmology", "Mahdia", DoctorState.CONFIRMED),
                new DoctorSeed("Walid", "Hamdi", "Gastroenterology", "Sousse", DoctorState.CONFIRMED),
                new DoctorSeed("Nour", "Chaabane", "Endocrinology", "Sfax", DoctorState.CONFIRMED),
                new DoctorSeed("Mehdi", "Kammoun", "Psychiatry", "Tunis", DoctorState.CONFIRMED),
                new DoctorSeed("Leila", "Dridi", "ENT", "Monastir", DoctorState.CONFIRMED),
                new DoctorSeed("Omar", "Ben Amor", "Cardiology", "Nabeul", DoctorState.CONFIRMED),
                new DoctorSeed("Hela", "Masmoudi", "Dermatology", "Sousse", DoctorState.CONFIRMED),
                new DoctorSeed("Karim", "Saidi", "General Medicine", "Tunis", DoctorState.CONFIRMED),
                new DoctorSeed("Asma", "Ben Youssef", "Pediatrics", "Mahdia", DoctorState.PENDING),
                new DoctorSeed("Fares", "Chakroun", "Neurology", "Sfax", DoctorState.PENDING),
                new DoctorSeed("Salma", "Rekik", "Cardiology", "Kairouan", DoctorState.REJECTED)
        );

        int created = 0;
        for (int index = 0; index < doctors.size(); index++) {
            int number = index + 1;
            String email = "doctor%02d@medilink.demo".formatted(number);
            String licenseNumber = "MED-%06d".formatted(100000 + number);
            if (userRepository.existsByEmailIgnoreCase(email) || doctorRepository.existsByLicenseNumber(licenseNumber)) {
                log.info("Doctor {} already exists - skipped", email);
                continue;
            }

            DoctorSeed seed = doctors.get(index);
            Doctor doctor = new Doctor();
            initializeUser(doctor, seed.firstname(), seed.lastname(), email, defaultPassword, Role.DOCTOR,
                    "+216 2%07d".formatted(1000000 + number));
            doctor.setLicenseNumber(licenseNumber);
            doctor.setSpecialty(requireSpecialty(specialties, seed.specialty()));
            doctor.setAdresse(seed.city() + ", Tunisia");
            doctor.setCertificationUrl(null);
            doctor.setState(seed.state());
            doctorRepository.save(doctor);
            created++;
        }
        log.info("Created {} doctors; skipped {} existing doctors", created, doctors.size() - created);
        return created;
    }

    private int seedPatients() {
        List<PatientSeed> patients = List.of(
                new PatientSeed("Ali", "Ben Ali", "MALE", LocalDate.of(1985, 2, 14)),
                new PatientSeed("Amina", "Trabelsi", "FEMALE", LocalDate.of(1991, 7, 3)),
                new PatientSeed("Sami", "Gharbi", "MALE", LocalDate.of(1978, 11, 22)),
                new PatientSeed("Emna", "Jebali", "FEMALE", LocalDate.of(1996, 4, 9)),
                new PatientSeed("Houssem", "Mabrouk", "MALE", LocalDate.of(1988, 9, 17)),
                new PatientSeed("Yosra", "Khelifi", "FEMALE", LocalDate.of(1982, 1, 28)),
                new PatientSeed("Bilel", "Nasri", "MALE", LocalDate.of(1999, 6, 12)),
                new PatientSeed("Marwa", "Toumi", "FEMALE", LocalDate.of(1975, 3, 30)),
                new PatientSeed("Anis", "Ben Salem", "MALE", LocalDate.of(1993, 12, 5)),
                new PatientSeed("Rim", "Abid", "FEMALE", LocalDate.of(1987, 8, 19)),
                new PatientSeed("Nader", "Kefi", "MALE", LocalDate.of(1969, 5, 7)),
                new PatientSeed("Sabrine", "Mzoughi", "FEMALE", LocalDate.of(2000, 10, 24)),
                new PatientSeed("Khalil", "Bouzid", "MALE", LocalDate.of(1980, 2, 2)),
                new PatientSeed("Sonia", "Ammar", "FEMALE", LocalDate.of(1994, 7, 16)),
                new PatientSeed("Maher", "Dhaouadi", "MALE", LocalDate.of(1972, 9, 11)),
                new PatientSeed("Fatma", "Ben Romdhane", "FEMALE", LocalDate.of(1989, 11, 4)),
                new PatientSeed("Seif", "Miled", "MALE", LocalDate.of(1997, 1, 21)),
                new PatientSeed("Olfa", "Charfi", "FEMALE", LocalDate.of(1983, 6, 26)),
                new PatientSeed("Tarek", "Lahmar", "MALE", LocalDate.of(1976, 4, 13)),
                new PatientSeed("Meriem", "Ben Hmida", "FEMALE", LocalDate.of(1990, 12, 8))
        );

        int created = 0;
        for (int index = 0; index < patients.size(); index++) {
            int number = index + 1;
            String email = "patient%02d@medilink.demo".formatted(number);
            if (userRepository.existsByEmailIgnoreCase(email)) {
                log.info("Patient {} already exists - skipped", email);
                continue;
            }

            PatientSeed seed = patients.get(index);
            Patient patient = new Patient();
            initializeUser(patient, seed.firstname(), seed.lastname(), email, defaultPassword, Role.PATIENT,
                    "+216 5%07d".formatted(2000000 + number));
            patient.setGender(seed.gender());
            patient.setDateOfBirth(seed.dateOfBirth());
            patientRepository.save(patient);
            created++;
        }
        log.info("Created {} patients; skipped {} existing patients", created, patients.size() - created);
        return created;
    }

    /**
     * Creates five deterministic recurring rules for each of the first ten confirmed demo doctors.
     * The range covers historical dashboard data and the next two weeks, while the day-of-week
     * selection always includes today and the following four days.
     */
    private SeedCount seedDoctorAvailabilities() {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusDays(14);
        LocalDate rangeEnd = today.plusDays(14);
        int created = 0;
        int skipped = 0;

        for (int doctorNumber = 1; doctorNumber <= 10; doctorNumber++) {
            String email = "doctor%02d@medilink.demo".formatted(doctorNumber);
            Doctor doctor = doctorRepository.findByEmailIgnoreCase(email).orElse(null);
            if (doctor == null || doctor.getState() != DoctorState.CONFIRMED) {
                log.info("Availability seed skipped for ineligible or missing demo doctor {}", email);
                continue;
            }

            for (int dayOffset = 0; dayOffset < 5; dayOffset++) {
                LocalDate occurrence = today.plusDays(dayOffset);
                TimeWindow window = availabilityWindow(doctorNumber, dayOffset);
                DoctorAvailability existing = availabilityRepository
                        .findByDoctorIdAndType(doctor.getId(), AvailabilityType.RECURRING).stream()
                        .filter(rule -> rule.getDayOfWeek() == occurrence.getDayOfWeek())
                        .filter(rule -> window.start().equals(rule.getStartTime()))
                        .filter(rule -> window.end().equals(rule.getEndTime()))
                        .filter(rule -> Integer.valueOf(30).equals(rule.getSlotDuration()))
                        .filter(rule -> !today.isBefore(rule.getStartDate()))
                        .filter(rule -> rule.getEndDate() == null || !today.isAfter(rule.getEndDate()))
                        .findFirst().orElse(null);
                if (existing != null) {
                    skipped++;
                    continue;
                }

                DoctorAvailability availability = DoctorAvailability.builder()
                        .doctor(doctor)
                        .dayOfWeek(occurrence.getDayOfWeek())
                        .startDate(rangeStart)
                        .endDate(rangeEnd)
                        .startTime(window.start())
                        .endTime(window.end())
                        .type(AvailabilityType.RECURRING)
                        .slotDuration(30)
                        .build();
                availability = availabilityRepository.save(availability);
                createSlots(availability);
                created++;
            }
        }
        log.info("Created {} availability rules; skipped {} existing rules", created, skipped);
        return new SeedCount(created, skipped);
    }

    private void createSlots(DoctorAvailability availability) {
        for (LocalTime start = availability.getStartTime();
             !start.plusMinutes(availability.getSlotDuration()).isAfter(availability.getEndTime());
             start = start.plusMinutes(availability.getSlotDuration())) {
            LocalTime end = start.plusMinutes(availability.getSlotDuration());
            if (slotRepository.findFirstByAvailabilityIdAndStartTimeAndEndTime(
                    availability.getId(), start, end).isEmpty()) {
                slotRepository.save(Slot.builder().availability(availability).startTime(start)
                        .endTime(end).status(SlotStatus.FREE).build());
            }
        }
    }

    private SeedCount seedAppointments() {
        LocalDate today = LocalDate.now();
        List<AppointmentSeed> seeds = List.of(
                appt(1, 3, 0, 9, 30, AppointmentStatus.CONFIRMED),
                appt(1, 4, 0, 11, 0, AppointmentStatus.BOOKED),
                appt(2, 2, 0, 14, 30, AppointmentStatus.CONFIRMED),
                appt(3, 8, 0, 9, 30, AppointmentStatus.BOOKED),
                appt(1, 1, 1, 10, 0, AppointmentStatus.CONFIRMED),
                appt(1, 2, 2, 14, 30, AppointmentStatus.BOOKED),
                appt(1, 5, 3, 10, 0, AppointmentStatus.CONFIRMED),
                appt(2, 6, 1, 10, 30, AppointmentStatus.BOOKED),
                appt(3, 7, 2, 15, 0, AppointmentStatus.CONFIRMED),
                appt(4, 8, 3, 10, 30, AppointmentStatus.BOOKED),
                appt(5, 9, 4, 9, 30, AppointmentStatus.CONFIRMED),
                appt(6, 10, 1, 10, 0, AppointmentStatus.BOOKED),
                appt(7, 2, 2, 9, 30, AppointmentStatus.CONFIRMED),
                appt(8, 4, 4, 10, 30, AppointmentStatus.BOOKED),
                appt(1, 1, -7, 9, 30, AppointmentStatus.COMPLETED),
                appt(1, 6, -6, 10, 30, AppointmentStatus.COMPLETED),
                appt(1, 7, -5, 14, 0, AppointmentStatus.COMPLETED),
                appt(2, 3, -7, 14, 30, AppointmentStatus.COMPLETED),
                appt(3, 4, -6, 10, 30, AppointmentStatus.COMPLETED),
                appt(4, 5, -5, 9, 30, AppointmentStatus.COMPLETED),
                appt(5, 8, -4, 14, 0, AppointmentStatus.COMPLETED),
                appt(6, 9, -7, 10, 0, AppointmentStatus.COMPLETED),
                appt(1, 1, -4, 11, 0, AppointmentStatus.CANCELED),
                appt(9, 10, -7, 9, 30, AppointmentStatus.CANCELED)
        );

        int created = 0;
        int skipped = 0;
        for (AppointmentSeed seed : seeds) {
            Doctor doctor = doctorRepository.findByEmailIgnoreCase(seed.doctorEmail()).orElse(null);
            Patient patient = patientRepository.findByEmailIgnoreCase(seed.patientEmail()).orElse(null);
            LocalDateTime start = today.plusDays(seed.dayOffset()).atTime(seed.time());
            if (doctor == null || patient == null || doctor.getState() != DoctorState.CONFIRMED) {
                skipped++;
                continue;
            }
            Slot slot = findApplicableSlot(doctor, start);
            if (slot == null) {
                log.warn("Appointment seed skipped because no matching availability exists for doctor {}", doctor.getId());
                skipped++;
                continue;
            }
            if (appointmentRepository.existsBySlotAvailabilityDoctorIdAndPatientIdAndScheduledStart(
                    doctor.getId(), patient.getId(), start)
                    || appointmentRepository.existsBySlotAvailabilityDoctorIdAndScheduledStart(
                    doctor.getId(), start)) {
                skipped++;
                continue;
            }
            appointmentRepository.save(Appointment.builder()
                    .bookedAt(start.minusDays(3))
                    .scheduledStart(start)
                    .scheduledEnd(start.toLocalDate().atTime(slot.getEndTime()))
                    .status(seed.status())
                    .slot(slot)
                    .patient(patient)
                    .build());
            created++;
        }
        log.info("Created {} appointments; skipped {} existing or unavailable appointments", created, skipped);
        return new SeedCount(created, skipped);
    }

    private Slot findApplicableSlot(Doctor doctor, LocalDateTime start) {
        return availabilityRepository.findByDoctorIdAndType(doctor.getId(), AvailabilityType.RECURRING).stream()
                .filter(rule -> rule.getDayOfWeek() == start.getDayOfWeek())
                .filter(rule -> !start.toLocalDate().isBefore(rule.getStartDate()))
                .filter(rule -> rule.getEndDate() == null || !start.toLocalDate().isAfter(rule.getEndDate()))
                .flatMap(rule -> slotRepository.findByAvailability(rule).stream())
                .filter(slot -> slot.getStartTime().equals(start.toLocalTime()))
                .findFirst().orElse(null);
    }

    private TimeWindow availabilityWindow(int doctorNumber, int dayOffset) {
        if (doctorNumber == 1) return new TimeWindow(LocalTime.of(9, 0), LocalTime.of(15, 30));
        int pattern = (doctorNumber + dayOffset) % 3;
        return switch (pattern) {
            case 0 -> new TimeWindow(LocalTime.of(9, 0), LocalTime.of(13, 0));
            case 1 -> new TimeWindow(LocalTime.of(10, 0), LocalTime.of(14, 0));
            default -> new TimeWindow(LocalTime.of(14, 0), LocalTime.of(18, 0));
        };
    }

    private AppointmentSeed appt(int doctor, int patient, int offset, int hour, int minute,
                                 AppointmentStatus status) {
        return new AppointmentSeed("doctor%02d@medilink.demo".formatted(doctor),
                "patient%02d@medilink.demo".formatted(patient), offset,
                LocalTime.of(hour, minute), status);
    }

    private void initializeUser(UserEntity user, String firstname, String lastname, String email,
                                String rawPassword, Role role, String phone) {
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(normalizeEmail(email));
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRole(role);
        user.setMfaEnabled(false);
        user.setMfaEnrollmentPending(false);
        user.setSecret(null);
        user.setAuthenticationVersion(0);
        user.setCredentialsUpdatedAt(Instant.now());
    }

    private Specialty requireSpecialty(Map<String, Specialty> specialties, String name) {
        Specialty specialty = specialties.get(name);
        if (specialty == null) {
            throw new IllegalStateException("Seed specialty is unavailable: " + name);
        }
        return specialty;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record DoctorSeed(String firstname, String lastname, String specialty, String city, DoctorState state) {
    }

    private record PatientSeed(String firstname, String lastname, String gender, LocalDate dateOfBirth) {
    }

    private record TimeWindow(LocalTime start, LocalTime end) { }
    private record AppointmentSeed(String doctorEmail, String patientEmail, int dayOffset,
                                   LocalTime time, AppointmentStatus status) { }
    private record SeedCount(int created, int skipped) { }
}
