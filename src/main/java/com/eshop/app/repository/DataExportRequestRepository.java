package com.eshop.app.repository;

import com.eshop.app.entity.DataExportRequest;
import com.eshop.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataExportRequestRepository extends JpaRepository<DataExportRequest, Long> {
    List<DataExportRequest> findByUser(User user);
    List<DataExportRequest> findByStatus(DataExportRequest.ExportStatus status);
}
