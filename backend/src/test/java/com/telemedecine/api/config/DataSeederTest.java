package com.telemedecine.api.config;

import com.telemedecine.api.dao.DoctorRepository;
import com.telemedecine.api.dao.PatientRepository;
import com.telemedecine.api.dao.SpecialtyRepository;
import com.telemedecine.api.dao.UserRepository;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.model.user.Admin;
import com.telemedecine.api.model.user.MfaState;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DoctorRepository doctorRepository = mock(DoctorRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final SpecialtyRepository specialtyRepository = mock(SpecialtyRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final Set<String> emails = new HashSet<>();
    private final Set<String> licenses = new HashSet<>();
    private final Map<String, Specialty> specialties = new HashMap<>();
    private final List<UserEntity> savedUsers = new ArrayList<>();
    private final List<Doctor> savedDoctors = new ArrayList<>();
    private final List<Patient> savedPatients = new ArrayList<>();

    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DataSeeder(userRepository, doctorRepository, patientRepository,
                specialtyRepository, passwordEncoder);
        ReflectionTestUtils.setField(seeder, "adminEmail", "superadmin@medilink.demo");
        ReflectionTestUtils.setField(seeder, "adminPassword", "admin-demo-secret");
        ReflectionTestUtils.setField(seeder, "defaultPassword", "user-demo-secret");

        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));
        when(userRepository.existsByEmailIgnoreCase(any())).thenAnswer(invocation ->
                emails.contains(invocation.<String>getArgument(0).toLowerCase(Locale.ROOT)));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            emails.add(user.getEmail().toLowerCase(Locale.ROOT));
            savedUsers.add(user);
            return user;
        });

        when(doctorRepository.existsByLicenseNumber(any())).thenAnswer(invocation ->
                licenses.contains(invocation.getArgument(0)));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor doctor = invocation.getArgument(0);
            emails.add(doctor.getEmail().toLowerCase(Locale.ROOT));
            licenses.add(doctor.getLicenseNumber());
            savedDoctors.add(doctor);
            return doctor;
        });
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            emails.add(patient.getEmail().toLowerCase(Locale.ROOT));
            savedPatients.add(patient);
            return patient;
        });

        when(specialtyRepository.findByNameIgnoreCase(any())).thenAnswer(invocation ->
                Optional.ofNullable(specialties.get(invocation.<String>getArgument(0).toLowerCase(Locale.ROOT))));
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> {
            Specialty specialty = invocation.getArgument(0);
            specialties.put(specialty.getName().toLowerCase(Locale.ROOT), specialty);
            return specialty;
        });
    }

    @Test
    void enabledSeedCreatesExpectedDataWithHashedPasswordsAndDisabledMfa() throws Exception {
        seeder.run(null);

        assertThat(specialties).hasSize(12);
        assertThat(savedUsers).singleElement().isInstanceOf(Admin.class);
        assertThat(savedDoctors).hasSize(18);
        assertThat(savedPatients).hasSize(20);
        assertThat(savedDoctors).filteredOn(doctor -> doctor.getState() == DoctorState.CONFIRMED).hasSize(15);
        assertThat(savedDoctors).filteredOn(doctor -> doctor.getState() == DoctorState.PENDING).hasSize(2);
        assertThat(savedDoctors).filteredOn(doctor -> doctor.getState() == DoctorState.REJECTED).hasSize(1);

        List<UserEntity> allUsers = new ArrayList<>(savedUsers);
        allUsers.addAll(savedDoctors);
        allUsers.addAll(savedPatients);
        assertThat(allUsers).allSatisfy(user -> {
            assertThat(user.getPassword()).startsWith("encoded:").doesNotMatch("^(admin|user)-demo-secret$");
            assertThat(user.getMfaState()).isEqualTo(MfaState.DISABLED);
            assertThat(user.getSecret()).isNull();
            assertThat(user.getAuthenticationVersion()).isZero();
            assertThat(user.getCredentialsUpdatedAt()).isNotNull();
        });
    }

    @Test
    void runningSeederTwiceDoesNotCreateDuplicates() throws Exception {
        seeder.run(null);
        seeder.run(null);

        assertThat(specialties).hasSize(12);
        assertThat(savedUsers).hasSize(1);
        assertThat(savedDoctors).hasSize(18);
        assertThat(savedPatients).hasSize(20);
    }

    @Test
    void disabledSeedDoesNotRegisterSeederBean() {
        new ApplicationContextRunner()
                .withUserConfiguration(DataSeeder.class)
                .withPropertyValues("app.seed.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DataSeeder.class));
    }
}
