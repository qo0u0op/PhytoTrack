package com.d0w0b.phytotrack.repository;

import com.d0w0b.phytotrack.models.DeactivateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeactivateRequestRepository extends JpaRepository<DeactivateRequest, Long> {
  Optional<DeactivateRequest> findByUserUserIdAndStatus(Long userId, DeactivateRequest.Status status);

  boolean existsByUserUserIdAndStatus(Long userId, DeactivateRequest.Status status);

  List<DeactivateRequest> findAllByStatusOrderByCreatedAtDesc(DeactivateRequest.Status status);

  List<DeactivateRequest> findAllByOrderByCreatedAtDesc();
}
