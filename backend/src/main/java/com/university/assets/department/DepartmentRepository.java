package com.university.assets.department;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Department> findByFacultyIdOrderByNameAsc(UUID facultyId);

    List<Department> findAllByOrderByNameAsc();
}
