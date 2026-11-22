package com.safepe.payment;

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
@AnalyzeClasses(packages = "com.safepe.payment", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * Controllers go through services. The three named controllers still call
     * repositories directly; Phase 2 moves that logic into services and removes
     * these exceptions.
     */
    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().resideInAPackage("..controller..")
            .and().doNotHaveSimpleName("BankAccountController")
            .and().doNotHaveSimpleName("BillController")
            .and().doNotHaveSimpleName("HistoryController")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule servicesDoNotDependOnControllers = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule repositoriesDependOnNothingAbove = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..");

    @ArchTest
    static final ArchRule entitiesStayPersistenceOnly = noClasses()
            .that().resideInAPackage("..model..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..dto..");

    /**
     * No @Autowired/@Inject fields. Phase 1 replaces the remaining @Value fields
     * with @ConfigurationProperties and switches this to
     * GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION, which also bans @Value.
     */
    @ArchTest
    static final ArchRule constructorInjectionOnly = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .because("dependencies should be final and passed through the constructor");
}
