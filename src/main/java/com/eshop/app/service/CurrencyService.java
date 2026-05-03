package com.eshop.app.service;

import com.eshop.app.dto.response.CurrencyDTO;
import com.eshop.app.entity.Currency;
import com.eshop.app.repository.CurrencyRepository;
import com.eshop.app.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CurrencyService {
    
    private final CurrencyRepository currencyRepository;
    private final AppProperties appProperties;
    
    public Currency createCurrency(Currency currency) {
        if (currencyRepository.existsByCode(currency.getCode())) {
            throw new IllegalArgumentException("Currency with code " + currency.getCode() + " already exists");
        }
        
        if (currency.getIsDefault()) {
            clearDefaultCurrency();
        }
        
        currency.setLastUpdated(LocalDateTime.now());
        return currencyRepository.save(currency);
    }
    
    public Currency updateCurrency(Long id, Currency currency) {
        Currency existing = currencyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Currency not found"));
        
        existing.setName(currency.getName());
        existing.setSymbol(currency.getSymbol());
        existing.setSymbolPosition(currency.getSymbolPosition());
        existing.setDecimalPlaces(currency.getDecimalPlaces());
        existing.setExchangeRate(currency.getExchangeRate());
        existing.setActive(currency.getActive());
        existing.setThousandsSeparator(currency.getThousandsSeparator());
        existing.setDecimalSeparator(currency.getDecimalSeparator());
        existing.setLastUpdated(LocalDateTime.now());
        
        if (currency.getIsDefault() && !existing.getIsDefault()) {
            clearDefaultCurrency();
            existing.setIsDefault(true);
        }
        
        return currencyRepository.save(existing);
    }
    
    @Cacheable("currencies")
    public List<CurrencyDTO> getAllCurrencies() {
        return currencyRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }
    
    @Cacheable("currencies")
    public List<CurrencyDTO> getActiveCurrencies() {
        return currencyRepository.findByActiveTrue().stream()
            .map(this::toDTO)
            .toList();
    }
    
    @Cacheable(value = "currencies", key = "#code")
    public CurrencyDTO getCurrencyByCodeDTO(String code) {
        Currency currency = currencyRepository.findByCode(code)
            .orElseThrow(() -> new RuntimeException("Currency not found: " + code));
        return toDTO(currency);
    }
    
    @Cacheable(value = "currencies", key = "'default'")
    public CurrencyDTO getDefaultCurrencyDTO() {
        Currency currency = currencyRepository.findByIsDefaultTrue()
            .orElseGet(() -> {
                    String defaultCode = appProperties.getBusiness().getDefaultCurrency();
                    log.warn("No default currency found, creating {} as default", defaultCode);
                return createCurrency(Currency.builder()
                            .code(defaultCode)
                            .name(defaultCode + " (Default)")
                            .symbol(defaultCode.equals("USD") ? "$" : (defaultCode.equals("INR") ? "₹" : ""))
                    .symbolPosition("LEFT")
                    .decimalPlaces(2)
                    .exchangeRate(BigDecimal.ONE)
                    .isDefault(true)
                    .active(true)
                    .build());
            });
        return toDTO(currency);
    }
    
    // Non-cached methods still return entities for internal use
    public Currency getCurrencyByCode(String code) {
        return currencyRepository.findByCode(code)
            .orElseThrow(() -> new RuntimeException("Currency not found: " + code));
    }
    
    public Currency getDefaultCurrency() {
        return currencyRepository.findByIsDefaultTrue()
            .orElseGet(() -> {
                    String defaultCode = appProperties.getBusiness().getDefaultCurrency();
                    log.warn("No default currency found, creating {} as default", defaultCode);
                return createCurrency(Currency.builder()
                            .code(defaultCode)
                            .name(defaultCode + " (Default)")
                            .symbol(defaultCode.equals("USD") ? "$" : (defaultCode.equals("INR") ? "₹" : ""))
                    .symbolPosition("LEFT")
                    .decimalPlaces(2)
                    .exchangeRate(BigDecimal.ONE)
                    .isDefault(true)
                    .active(true)
                    .build());
            });
    }
    
    // Helper method to convert entity to DTO (safe for Redis)
    private CurrencyDTO toDTO(Currency currency) {
        return new CurrencyDTO(
            currency.getId(),
            currency.getCode(),
            currency.getName(),
            currency.getSymbol(),
            currency.getSymbolPosition(),
            currency.getDecimalPlaces(),
            currency.getExchangeRate(),
            currency.getIsDefault(),
            currency.getActive()
        );
    }
    
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        Currency from = getCurrencyByCode(fromCurrency);
        Currency to = getCurrencyByCode(toCurrency);
        
        // Convert to base currency first, then to target
        BigDecimal amountInBase = amount.divide(from.getExchangeRate(), 8, RoundingMode.HALF_UP);
        BigDecimal convertedAmount = amountInBase.multiply(to.getExchangeRate());
        
        return convertedAmount.setScale(to.getDecimalPlaces(), RoundingMode.HALF_UP);
    }
    
    public String formatAmount(BigDecimal amount, Currency currency) {
        String formattedNumber = formatNumber(amount, currency);
        
        if ("LEFT".equals(currency.getSymbolPosition())) {
            return currency.getSymbol() + formattedNumber;
        } else {
            return formattedNumber + currency.getSymbol();
        }
    }
    
    private String formatNumber(BigDecimal amount, Currency currency) {
        BigDecimal rounded = amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        String[] parts = rounded.toPlainString().split("\\\\.");
        
        String integerPart = parts[0].replaceAll("\\B(?=(\\d{3})+(?!\\d))", currency.getThousandsSeparator());
        
        if (parts.length > 1 && currency.getDecimalPlaces() > 0) {
            return integerPart + currency.getDecimalSeparator() + parts[1];
        }
        
        return integerPart;
    }
    
    public void updateExchangeRate(String code, BigDecimal newRate) {
        Currency currency = getCurrencyByCode(code);
        currency.setExchangeRate(newRate);
        currency.setLastUpdated(LocalDateTime.now());
        currencyRepository.save(currency);
        log.info("Updated exchange rate for {}: {}", code, newRate);
    }
    
    private void clearDefaultCurrency() {
        currencyRepository.findByIsDefaultTrue().ifPresent(currency -> {
            currency.setIsDefault(false);
            currencyRepository.save(currency);
        });
    }
    
    public void deleteCurrency(Long id) {
        Currency currency = currencyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Currency not found"));
        
        if (currency.getIsDefault()) {
            throw new IllegalStateException("Cannot delete default currency");
        }
        
        currencyRepository.delete(currency);
    }
}
