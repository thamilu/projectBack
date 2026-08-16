package com.eshop.app.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
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

    /**
     * A hardcoded role-name literal in {@code @PreAuthorize} (e.g. {@code hasRole('ADMIN')},
     * {@code hasAnyRole('SELLER','ADMIN')}) can silently typo into a role nothing ever holds —
     * this is exactly how {@code hasRole('USER')} (meant to be {@code CUSTOMER}) and
     * {@code hasRole('MANAGER')} (not a real role at all) went undetected in this codebase. It
     * also duplicates role-name configuration that {@code AppProperties.Security.Roles} already
     * centralizes. Reference a named constant from {@code SecurityExpressions} (routes through
     * {@code @userSecurity}, honoring the configurable role prefix) or, at minimum, the
     * {@code @appProperties.security.roles.*} property instead of a literal string.
     */
    private static final Pattern HARDCODED_ROLE_LITERAL =
            Pattern.compile("has(Role|AnyRole|Authority|AnyAuthority)\\(\\s*['\"]");

    @ArchTest
    static final ArchRule preauthorize_methods_should_not_hardcode_role_literals = methods()
            .that().areAnnotatedWith(PreAuthorize.class)
            .should(new ArchCondition<JavaMethod>("not hardcode a role-name literal in @PreAuthorize") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    checkExpression(method.getAnnotationOfType(PreAuthorize.class).value(), method.getFullName(), method,
                            events);
                }
            });

    @ArchTest
    static final ArchRule preauthorize_classes_should_not_hardcode_role_literals = classes()
            .that().areAnnotatedWith(PreAuthorize.class)
            .should(new ArchCondition<JavaClass>("not hardcode a role-name literal in @PreAuthorize") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    checkExpression(clazz.getAnnotationOfType(PreAuthorize.class).value(), clazz.getFullName(), clazz,
                            events);
                }
            });

    private static void checkExpression(String expression, String location, Object correspondingObject,
            ConditionEvents events) {
        if (HARDCODED_ROLE_LITERAL.matcher(expression).find()) {
            events.add(SimpleConditionEvent.violated(correspondingObject,
                    location + " uses a hardcoded role-name literal in @PreAuthorize(\"" + expression + "\")"
                            + " — use a SecurityExpressions constant or @appProperties.security.roles.* instead"));
        }
    }

}
