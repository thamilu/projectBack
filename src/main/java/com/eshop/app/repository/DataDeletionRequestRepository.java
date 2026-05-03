package com.eshop.app.repository;

import com.eshop.app.entity.DataDeletionRequest;
import com.eshop.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, Long> {
    List<DataDeletionRequest> findByUser(User user);
    List<DataDeletionRequest> findByStatus(DataDeletionRequest.DeletionStatus status);
}
