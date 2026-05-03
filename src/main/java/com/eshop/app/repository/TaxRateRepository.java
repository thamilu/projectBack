package com.eshop.app.repository;

import com.eshop.app.entity.GeoZone;
import com.eshop.app.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    List<TaxRate> findByActiveTrue();
    List<TaxRate> findByGeoZone(GeoZone geoZone);
    List<TaxRate> findByGeoZoneAndActiveTrueOrderByPriorityAsc(GeoZone geoZone);
}
