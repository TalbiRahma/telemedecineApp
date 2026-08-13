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
import com.telemedecine.api.model.user.MfaState;
import com.telemedecine.api.model.user.Patient;
import com.telemedecine.api.model.user.UserEntity;
import com.telemedecine.api.model.user.doctor.Doctor;
import com.telemedecine.api.model.user.doctor.DoctorState;
import com.telemedecine.api.model.user.doctor.DoctorAvailability;
import com.telemedecine.api.model.user.doctor.AvailabilityType;
import com.telemedecine.api.model.slot.Slot;
import com.telemedecine.api.model.appointement.Appointment;
import com.telemedecine.api.model.appointement.AppointmentStatus;
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
import java.time.LocalDate;

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
    private final DoctorAvailabilityRepository availabilityRepository = mock(DoctorAvailabilityRepository.class);
    private final SlotRepository slotRepository = mock(SlotRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);

    private final Set<String> emails = new HashSet<>();
    private final Set<String> licenses = new HashSet<>();
    private final Map<String, Specialty> specialties = new HashMap<>();
    private final List<UserEntity> savedUsers = new ArrayList<>();
    private final List<Doctor> savedDoctors = new ArrayList<>();
    private final List<Patient> savedPatients = new ArrayList<>();
    private final List<DoctorAvailability> savedAvailabilities = new ArrayList<>();
    private final List<Slot> savedSlots = new ArrayList<>();
    private final List<Appointment> savedAppointments = new ArrayList<>();

    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DataSeeder(userRepository, doctorRepository, patientRepository,
                specialtyRepository, passwordEncoder, availabilityRepository, slotRepository,
                appointmentRepository);
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
            doctor.setId((long) savedDoctors.size() + 1);
            emails.add(doctor.getEmail().toLowerCase(Locale.ROOT));
            licenses.add(doctor.getLicenseNumber());
            savedDoctors.add(doctor);
            return doctor;
        });
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId((long) savedPatients.size() + 101);
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

        when(doctorRepository.findByEmailIgnoreCase(any())).thenAnswer(invocation -> savedDoctors.stream()
                .filter(doctor -> doctor.getEmail().equalsIgnoreCase(invocation.getArgument(0)))
                .findFirst());
        when(patientRepository.findByEmailIgnoreCase(any())).thenAnswer(invocation -> savedPatients.stream()
                .filter(patient -> patient.getEmail().equalsIgnoreCase(invocation.getArgument(0)))
                .findFirst());
        when(availabilityRepository.findByDoctorIdAndType(any(), any())).thenAnswer(invocation ->
                savedAvailabilities.stream()
                        .filter(rule -> rule.getDoctor().getId().equals(invocation.getArgument(0)))
                        .filter(rule -> rule.getType() == invocation.getArgument(1)).toList());
        when(availabilityRepository.save(any(DoctorAvailability.class))).thenAnswer(invocation -> {
            DoctorAvailability availability = invocation.getArgument(0);
            availability.setId((long) savedAvailabilities.size() + 201);
            savedAvailabilities.add(availability);
            return availability;
        });
        when(slotRepository.findByAvailability(any())).thenAnswer(invocation -> savedSlots.stream()
                .filter(slot -> slot.getAvailability() == invocation.getArgument(0)).toList());
        when(slotRepository.findFirstByAvailabilityIdAndStartTimeAndEndTime(any(), any(), any()))
                .thenAnswer(invocation -> savedSlots.stream()
                        .filter(slot -> slot.getAvailability().getId().equals(invocation.getArgument(0)))
                        .filter(slot -> slot.getStartTime().equals(invocation.getArgument(1)))
                        .filter(slot -> slot.getEndTime().equals(invocation.getArgument(2))).findFirst());
        when(slotRepository.save(any(Slot.class))).thenAnswer(invocation -> {
            Slot slot = invocation.getArgument(0);
            slot.setId((long) savedSlots.size() + 501);
            savedSlots.add(slot);
            return slot;
        });
        when(appointmentRepository.existsBySlotAvailabilityDoctorIdAndPatientIdAndScheduledStart(any(), any(), any()))
                .thenAnswer(invocation -> savedAppointments.stream()
                        .anyMatch(a -> a.getSlot().getAvailability().getDoctor().getId().equals(invocation.getArgument(0))
                                && a.getPatient().getId().equals(invocation.getArgument(1))
                                && a.getScheduledStart().equals(invocation.getArgument(2))));
        when(appointmentRepository.existsBySlotAvailabilityDoctorIdAndScheduledStart(any(), any()))
                .thenAnswer(invocation -> savedAppointments.stream()
                        .anyMatch(a -> a.getSlot().getAvailability().getDoctor().getId().equals(invocation.getArgument(0))
                                && a.getScheduledStart().equals(invocation.getArgument(1))));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId((long) savedAppointments.size() + 801);
            savedAppointments.add(appointment);
            return appointment;
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
        assertThat(savedAvailabilities).hasSize(50)
                .allSatisfy(rule -> assertThat(rule.getDoctor().getState()).isEqualTo(DoctorState.CONFIRMED));
        assertThat(savedAppointments).hasSize(24);

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
        assertThat(savedAvailabilities).hasSize(50);
        assertThat(savedAppointments).hasSize(24);
    }

    @Test
    void seededAppointmentsProvideDashboardDataAndRespectAvailabilityWithoutOverlap() throws Exception {
        seeder.run(null);
        LocalDate today = LocalDate.now();

        List<Appointment> doctorOne = savedAppointments.stream()
                .filter(a -> a.getSlot().getAvailability().getDoctor().getEmail()
                        .equals("doctor01@medilink.demo")).toList();
        assertThat(doctorOne).filteredOn(a -> a.getScheduledStart().toLocalDate().equals(today)).hasSize(2);
        assertThat(doctorOne).anySatisfy(a -> assertThat(a.getScheduledStart().toLocalDate()).isAfter(today));
        assertThat(doctorOne).filteredOn(a -> a.getStatus() == AppointmentStatus.COMPLETED).hasSize(3);

        List<Appointment> patientOne = savedAppointments.stream()
                .filter(a -> a.getPatient().getEmail().equals("patient01@medilink.demo")).toList();
        assertThat(patientOne).anySatisfy(a -> {
            assertThat(a.getScheduledStart().toLocalDate()).isAfter(today);
            assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            assertThat(a.getSlot().getAvailability().getDoctor().getEmail())
                    .isEqualTo("doctor01@medilink.demo");
        });
        assertThat(patientOne).anyMatch(a -> a.getStatus() == AppointmentStatus.COMPLETED);

        assertThat(savedAppointments).allSatisfy(a -> {
            DoctorAvailability rule = a.getSlot().getAvailability();
            assertThat(a.getScheduledStart().getDayOfWeek()).isEqualTo(rule.getDayOfWeek());
            assertThat(a.getScheduledStart().toLocalTime()).isEqualTo(a.getSlot().getStartTime());
            assertThat(a.getScheduledEnd().toLocalTime()).isEqualTo(a.getSlot().getEndTime());
        });
        assertThat(savedAppointments).extracting(a -> a.getSlot().getAvailability().getDoctor().getId()
                        + "@" + a.getScheduledStart()).doesNotHaveDuplicates();
    }

    @Test
    void disabledSeedDoesNotRegisterSeederBean() {
        new ApplicationContextRunner()
                .withUserConfiguration(DataSeeder.class)
                .withPropertyValues("app.seed.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DataSeeder.class));
    }
}
