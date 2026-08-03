package com.university.assets.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "faculty", "department"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "faculty", "department"})
    Optional<User> findWithRolesById(UUID id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUniversityId(String universityId);

    long countByRoles_Name(String roleName);
}
