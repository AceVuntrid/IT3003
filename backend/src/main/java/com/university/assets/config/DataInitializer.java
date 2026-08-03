package com.university.assets.config;

import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.department.Department;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.Faculty;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.location.LocationRepository;
import com.university.assets.role.Role;
import com.university.assets.role.RoleRepository;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates the development accounts on first start. Passwords come from the
 * environment (ADMIN_PASSWORD); nothing is hard-coded in source or migrations.
 * Seeding is idempotent per account so new demo users introduced by later
 * releases are still created on existing databases.
 */
@Component
@Order(10)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                           FacultyRepository facultyRepository, DepartmentRepository departmentRepository,
                           LocationRepository locationRepository,
                           PasswordEncoder passwordEncoder, AppProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = properties.seed().adminEmail();
        String password = properties.seed().adminPassword();

        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            createUser("U-0001", "System", "Administrator", adminEmail, password,
                    "SUPER_ADMIN", null, null);
            log.info("Created super administrator account {}", adminEmail);
        }

        if (properties.seed().demoData()) {
            seedDemoUsers(password);
        }
    }

    private void seedDemoUsers(String password) {
        Faculty science = facultyRepository.findByCodeIgnoreCase("SCI").orElse(null);
        Department chemistry = departmentRepository.findByCodeIgnoreCase("CHEM").orElse(null);
        Department physics = departmentRepository.findByCodeIgnoreCase("PHYS").orElse(null);
        Department statistics = departmentRepository.findByCodeIgnoreCase("STAT").orElse(null);
        Department zoology = departmentRepository.findByCodeIgnoreCase("ZOO").orElse(null);
        Department botany = departmentRepository.findByCodeIgnoreCase("BOT").orElse(null);
        Department mathematics = departmentRepository.findByCodeIgnoreCase("MATH").orElse(null);

        ensureUser("U-1001", "Alice", "Nguyen", "asset.admin@university.local", password,
                "ASSET_ADMIN", science, null);
        ensureUser("U-1002", "Ben", "Carter", "lab.manager@university.local", password,
                "LAB_MANAGER", science, chemistry);
        ensureUser("U-1003", "Chandra", "Patel", "storekeeper@university.local", password,
                "STOREKEEPER", science, chemistry);
        ensureUser("U-1004", "Dana", "Silva", "maintenance@university.local", password,
                "MAINTENANCE_OFFICER", science, null);
        ensureUser("U-1005", "Evan", "Osei", "finance@university.local", password,
                "FINANCE_OFFICER", science, null);
        ensureUser("U-1006", "Farah", "Hassan", "lecturer@university.local", password,
                "LECTURER", science, physics);
        ensureUser("U-1007", "Grace", "Lin", "student@university.local", password,
                "STUDENT", science, physics);
        ensureUser("U-1008", "Hugh", "Byrne", "auditor@university.local", password,
                "AUDITOR", null, null);

        // Custodianship demo accounts: faculty dean, one admin per department and
        // the caretakers responsible for the unowned ILC/SSC buildings.
        ensureUser("U-1009", "Indira", "Weerasinghe", "dean@university.local", password,
                "FACULTY_DEAN", science, null);
        ensureUser("U-1010", "Janaka", "Perera", "physics.admin@university.local", password,
                "DEPT_ADMIN", science, physics);
        ensureUser("U-1011", "Kavya", "Fernando", "chemistry.admin@university.local", password,
                "DEPT_ADMIN", science, chemistry);
        ensureUser("U-1012", "Liyoni", "Ratnayake", "statistics.admin@university.local", password,
                "DEPT_ADMIN", science, statistics);
        ensureUser("U-1013", "Maya", "Jayawardena", "zoology.admin@university.local", password,
                "DEPT_ADMIN", science, zoology);
        ensureUser("U-1014", "Nuwan", "Bandara", "botany.admin@university.local", password,
                "DEPT_ADMIN", science, botany);
        ensureUser("U-1015", "Omala", "Herath", "maths.admin@university.local", password,
                "DEPT_ADMIN", science, mathematics);
        User ilcCaretaker = ensureUser("U-1016", "Piyal", "Gunasekara", "ilc.caretaker@university.local", password,
                "CARETAKER", null, null);
        User sscCaretaker = ensureUser("U-1017", "Quintus", "Dias", "ssc.caretaker@university.local", password,
                "CARETAKER", null, null);

        assignCaretaker("ILC", ilcCaretaker);
        assignCaretaker("SSC", sscCaretaker);

        log.info("Demo users for each role are present (password from ADMIN_PASSWORD env)");
    }

    /** Makes the caretaker the responsible user of the building unless one is already set. */
    private void assignCaretaker(String locationCode, User caretaker) {
        if (caretaker == null) {
            return;
        }
        locationRepository.findByCodeIgnoreCase(locationCode).ifPresent(location -> {
            if (location.getResponsibleUser() == null) {
                location.setResponsibleUser(caretaker);
                locationRepository.save(location);
                log.info("Assigned {} as responsible user of location {}", caretaker.getEmail(), locationCode);
            }
        });
    }

    private User ensureUser(String universityId, String firstName, String lastName, String email,
                            String password, String roleName, Faculty faculty, Department department) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(
                () -> createUser(universityId, firstName, lastName, email, password, roleName, faculty, department));
    }

    private User createUser(String universityId, String firstName, String lastName, String email,
                            String password, String roleName, Faculty faculty, Department department) {
        Role role = roleRepository.findByNameIgnoreCase(roleName).orElseThrow(
                () -> new IllegalStateException("Seed role missing: " + roleName));
        User user = new User();
        user.setUniversityId(universityId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setMustChangePassword(false);
        user.setFaculty(faculty);
        user.setDepartment(department);
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }
}
