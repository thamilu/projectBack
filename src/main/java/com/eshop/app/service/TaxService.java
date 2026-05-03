package com.eshop.app.service;

import com.eshop.app.entity.*;
import com.eshop.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaxService {
    
    private final TaxClassRepository taxClassRepository;
    private final TaxRateRepository taxRateRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final GeoZoneRepository geoZoneRepository;
    private final OrderTaxRepository orderTaxRepository;
    
    public TaxClass createTaxClass(TaxClass taxClass) {
        return taxClassRepository.save(taxClass);
    }
    
    public TaxRate createTaxRate(TaxRate taxRate) {
        return taxRateRepository.save(taxRate);
    }
    
    public TaxRule createTaxRule(TaxRule taxRule) {
        return taxRuleRepository.save(taxRule);
    }
    
    public GeoZone createGeoZone(GeoZone geoZone) {
        return geoZoneRepository.save(geoZone);
    }
    
    public List<TaxClass> getAllTaxClasses() {
        return taxClassRepository.findAll();
    }
    
    public List<TaxRate> getAllTaxRates() {
        return taxRateRepository.findAll();
    }
    
    public List<GeoZone> getMatchingGeoZones(String country, String state, String city) {
        return geoZoneRepository.findMatchingGeoZones(country, state, city);
    }
    
    /**
     * Calculate tax for a product based on customer location
     * Time Complexity: O(n*m) where n = number of geo zones, m = number of tax rules
     */
    public BigDecimal calculateProductTax(Product product, String country, String state, String city, BigDecimal amount) {
        if (product.getTaxClass() == null) {
            return BigDecimal.ZERO;
        }
        
        // Find matching geo zones
        List<GeoZone> geoZones = getMatchingGeoZones(country, state, city);
        if (geoZones.isEmpty()) {
            log.debug("No geo zones found for location: {}, {}, {}", country, state, city);
            return BigDecimal.ZERO;
        }
        
        // Get tax rules for this product's tax class
        List<TaxRule> taxRules = taxRuleRepository.findByTaxClassAndActiveTrueOrderByPriorityAsc(product.getTaxClass());
        if (taxRules.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal baseAmount = amount;
        
        // Apply taxes in order of priority
        for (TaxRule rule : taxRules) {
            TaxRate taxRate = rule.getTaxRate();
            
            // Check if tax rate applies to this geo zone
            if (geoZones.contains(taxRate.getGeoZone()) && taxRate.getActive()) {
                BigDecimal taxAmount;
                
                if (taxRate.getType() == TaxRate.TaxType.PERCENTAGE) {
                    // Calculate percentage tax
                    taxAmount = baseAmount.multiply(taxRate.getRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    // Fixed tax amount
                    taxAmount = taxRate.getRate();
                }
                
                totalTax = totalTax.add(taxAmount);
                
                // If compound tax, add to base for next calculation
                if (taxRate.getCompound()) {
                    baseAmount = baseAmount.add(taxAmount);
                }
                
                log.debug("Applied tax {}: {} ({})", taxRate.getName(), taxAmount, taxRate.getType());
            }
        }
        
        return totalTax.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate and save taxes for an order
     * Time Complexity: O(n) where n = number of order items
     */
    public List<OrderTax> calculateOrderTaxes(Order order, String country, String state, String city) {
        List<OrderTax> orderTaxes = new ArrayList<>();
        
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            
            BigDecimal taxAmount = calculateProductTax(product, country, state, city, itemTotal);
            
            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Find applied tax rates to create order tax records
                if (product.getTaxClass() != null) {
                    List<GeoZone> geoZones = getMatchingGeoZones(country, state, city);
                    List<TaxRule> taxRules = taxRuleRepository.findByTaxClassAndActiveTrueOrderByPriorityAsc(product.getTaxClass());
                    
                    for (TaxRule rule : taxRules) {
                        TaxRate taxRate = rule.getTaxRate();
                        if (geoZones.contains(taxRate.getGeoZone()) && taxRate.getActive()) {
                            BigDecimal itemTaxAmount;
                            
                            if (taxRate.getType() == TaxRate.TaxType.PERCENTAGE) {
                                itemTaxAmount = itemTotal.multiply(taxRate.getRate())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            } else {
                                itemTaxAmount = taxRate.getRate();
                            }
                            
                            OrderTax orderTax = OrderTax.builder()
                                .order(order)
                                .taxRate(taxRate)
                                .title(taxRate.getName())
                                .amount(itemTaxAmount)
                                .build();
                            
                            orderTaxes.add(orderTax);
                        }
                    }
                }
            }
        }
        
        // Save all order taxes
        if (!orderTaxes.isEmpty()) {
            orderTaxRepository.saveAll(orderTaxes);
        }
        
        return orderTaxes;
    }
    
    /**
     * Get total tax amount for an order
     */
    public BigDecimal getOrderTotalTax(Order order) {
        List<OrderTax> orderTaxes = orderTaxRepository.findByOrder(order);
        return orderTaxes.stream()
            .map(OrderTax::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public void updateTaxClass(Long id, TaxClass taxClass) {
        TaxClass existing = taxClassRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tax class not found"));
        existing.setName(taxClass.getName());
        existing.setDescription(taxClass.getDescription());
        existing.setActive(taxClass.getActive());
        taxClassRepository.save(existing);
    }
    
    public void updateTaxRate(Long id, TaxRate taxRate) {
        TaxRate existing = taxRateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tax rate not found"));
        existing.setName(taxRate.getName());
        existing.setRate(taxRate.getRate());
        existing.setType(taxRate.getType());
        existing.setPriority(taxRate.getPriority());
        existing.setCompound(taxRate.getCompound());
        existing.setActive(taxRate.getActive());
        taxRateRepository.save(existing);
    }
}
