package com.eshop.app.core.kernel.repository;

import com.eshop.app.core.kernel.entity.DataExportRequest;
import com.eshop.app.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataExportRequestRepository extends JpaRepository<DataExportRequest, Long> {
    List<DataExportRequest> findByUser(User user);
    List<DataExportRequest> findByStatus(DataExportRequest.ExportStatus status);
}
