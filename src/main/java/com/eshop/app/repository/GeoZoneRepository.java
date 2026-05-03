package com.eshop.app.repository;

import com.eshop.app.entity.GeoZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoZoneRepository extends JpaRepository<GeoZone, Long> {
    List<GeoZone> findByActiveTrue();
    List<GeoZone> findByCountry(String country);
    List<GeoZone> findByCountryAndState(String country, String state);
    
    @Query("SELECT gz FROM GeoZone gz WHERE gz.country = :country " +
           "AND (gz.state IS NULL OR gz.state = :state) " +
           "AND (gz.city IS NULL OR gz.city = :city) " +
           "AND gz.active = true")
    List<GeoZone> findMatchingGeoZones(@Param("country") String country, 
                                       @Param("state") String state, 
                                       @Param("city") String city);
}
