package com.telemedecine.api.config;

import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.SpecialtyRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.user.Admin;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.Role;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorState;
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

        log.info("MediLink database seed completed: {} specialties available, {} super admin, {} doctors, {} patients created",
                specialties.size(),
                adminsCreated, doctorsCreated, patientsCreated);
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
}
