package com.eshop.app.repository;

import com.eshop.app.entity.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    
    Optional<AttributeDefinition> findByName(String name);
    
    boolean existsByName(String name);
}
