package com.d0w0b.phytotrack.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.d0w0b.phytotrack.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  // 用optional處理唯一值可能是null的情況
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByUsername(String username);

  long countByRoleAndActive(User.Role role, boolean active);
}
