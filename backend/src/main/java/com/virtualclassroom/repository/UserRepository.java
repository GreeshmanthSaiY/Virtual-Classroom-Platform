package com.virtualclassroom.repository;

import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleAndStatus(Role role, UserStatus status);
    Page<User> findByRole(Role role, Pageable pageable);
    long countByRoleAndStatus(Role role, UserStatus status);
    long countByRole(Role role);
    long countByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:query IS NULL OR :query = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query,
                           @Param("role") Role role,
                           @Param("status") UserStatus status,
                           Pageable pageable);
}

