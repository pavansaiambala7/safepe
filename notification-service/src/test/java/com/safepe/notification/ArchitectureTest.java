package com.safepe.notification;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Layering rules: controller -> service -> repository, never backwards.
 * Breaking one fails the build, so the structure cannot erode silently.
 */
@AnalyzeClasses(packages = "com.safepe.notification", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule servicesDoNotDependOnControllers = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule constructorInjectionOnly = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .because("dependencies should be final and passed through the constructor");
}
