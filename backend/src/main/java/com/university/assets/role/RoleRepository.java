package com.university.assets.role;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(UUID id);
}
