package com.safepe.fraud;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

/**
 * Constructor injection only, in application and test code alike: every
 * dependency is a constructor parameter, normally stored in a final field.
 * (Test classes import all of fraud, including tests, on purpose.)
 */
@AnalyzeClasses(packages = "com.safepe.fraud")
class InjectionRulesTest {

    /** Bans @Autowired, @Value, @Inject and @Resource on fields. */
    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    /** Bans @Autowired on constructors and methods too: a single constructor needs no annotation. */
    @ArchTest
    static final ArchRule noAutowiredAnywhere = noCodeUnits()
            .should().beAnnotatedWith(Autowired.class)
            .because("Spring injects through the only constructor without @Autowired");
}
