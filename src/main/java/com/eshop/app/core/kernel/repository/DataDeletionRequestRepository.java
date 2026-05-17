package com.eshop.app.core.kernel.repository;

import com.eshop.app.core.kernel.entity.DataDeletionRequest;
import com.eshop.app.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, Long> {
    List<DataDeletionRequest> findByUser(User user);
    List<DataDeletionRequest> findByStatus(DataDeletionRequest.DeletionStatus status);
}
