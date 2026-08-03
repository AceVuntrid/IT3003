package com.university.assets.location;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    Optional<Location> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByParentId(UUID parentId);

    long countByParentIdAndActiveTrue(UUID parentId);

    @EntityGraph(attributePaths = {"parent", "faculty", "department", "responsibleUser"})
    List<Location> findAllByOrderByNameAsc();

    List<Location> findByFacultyIdAndActiveTrueOrderByNameAsc(UUID facultyId);
}
