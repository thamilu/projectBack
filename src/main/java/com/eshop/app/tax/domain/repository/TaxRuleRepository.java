package com.eshop.app.tax.domain.repository;

import com.eshop.app.tax.domain.entity.TaxClass;
import com.eshop.app.tax.domain.entity.TaxRule;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, Long> {
    List<TaxRule> findByTaxClass(TaxClass taxClass);
    List<TaxRule> findByTaxClassAndActiveTrueOrderByPriorityAsc(TaxClass taxClass);
}
