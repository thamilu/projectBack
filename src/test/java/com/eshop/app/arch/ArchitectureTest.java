package com.eshop.app.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * [HARDEN] Automated Architecture Enforcement.
 * Ensures the project follows enterprise layered standards and prevents logic leakage.
 */
@AnalyzeClasses(packages = "com.eshop.app", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    // Specific architecture constraints are enforced below via focused rules.

    /**
     * Controllers must not access Repositories directly.
     * Business logic must be encapsulated in the Service layer.
     */
    @ArchTest
    static final ArchRule controllers_should_not_access_repositories_directly = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    /**
     * Services should not depend on Controllers.
     * Downward dependency rule only.
     */
    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    /**
     * Entities must not leak to the Controller layer.
     * Use DTOs for all API interactions to protect the internal data model.
     * [HARDEN] Exception: BaseEntity/Common entities might be allowed if unavoidable, 
     * but specific domain entities are forbidden.
     */
    @ArchTest
    static final ArchRule entities_should_not_be_exposed_in_controllers = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..entity..");

}
