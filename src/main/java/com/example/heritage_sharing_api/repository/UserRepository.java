package com.example.heritage_sharing_api.repository;

import com.example.heritage_sharing_api.entity.ContributorRequestStatus;
import com.example.heritage_sharing_api.entity.User;
import com.example.heritage_sharing_api.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRoleOrderByUserIdAsc(UserRole role);
    List<User> findByRoleRequestStatusOrderByUserIdAsc(ContributorRequestStatus roleRequestStatus);
}
