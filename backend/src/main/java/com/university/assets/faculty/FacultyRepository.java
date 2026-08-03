package com.university.assets.faculty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacultyRepository extends JpaRepository<Faculty, UUID> {

    Optional<Faculty> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Faculty> findAllByOrderByNameAsc();
}
