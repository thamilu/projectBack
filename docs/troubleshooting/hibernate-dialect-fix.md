# CRITICAL-006 FIX: Hibernate Dialect Deprecation

## Problem
```
HHH90000025: PostgreSQLDialect does not need to be specified explicitly using 'hibernate.dialect' 
(remove the property setting and it will be selected by default)
```

## Root Cause
- Hibernate 7 (used in Spring Boot 4.x) auto-detects database dialect from the datasource
- Explicit `spring.jpa.database-platform` and `spring.jpa.properties.hibernate.dialect` are deprecated
- Configuration causes startup warnings

## Files Modified
1. ✅ `application-dev.properties` - Removed both occurrences
2. ✅ `application-prod.properties` - Removed dialect configuration  
3. ⚠️ `application-test.properties` - Has duplicate entries (lines 29 and 130) - **MANUAL REMOVAL NEEDED**
4. ⚠️ `src/test/resources/application.properties` - Has H2 dialect - **MANUAL REMOVAL NEEDED**

## Manual Cleanup Required

### application-test.properties
Remove these lines (appears twice in file):
```properties
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

### src/test/resources/application.properties  
Remove this line:
```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

## Result After Fix
- No more HHH90000025 warnings
- Cleaner configuration
- Hibernate automatically selects correct dialect based on JDBC URL

## Verification
Start application and check logs - should not see any dialect warnings:
```bash
./gradlew bootRun --args="--spring.profiles.active=dev"
```

Look for absence of:
```
WARN org.hibernate.orm.deprecation - HHH90000025: PostgreSQLDialect does not need to be specified explicitly
```
