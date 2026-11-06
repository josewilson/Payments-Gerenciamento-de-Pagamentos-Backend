package com.josewilson.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.josewilson.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .layer("Client").definedBy("..client..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Client").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule domainIsIndependent = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..controller..", "..service..", "..repository..", "..client..", "..dto..", "..mapper..");

    @ArchTest
    static final ArchRule noPackageCycles = slices().matching("com.josewilson.payments.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule constructorInjectionOnly = noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule controllersLiveInControllerPackage = classes()
            .that().areAnnotatedWith(RestController.class).should().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule servicesLiveInServicePackage = classes()
            .that().areAnnotatedWith(Service.class).should().resideInAPackage("..service..");

    @ArchTest
    static final ArchRule exceptionsLiveInExceptionPackage = classes()
            .that().areAssignableTo(RuntimeException.class).should().resideInAPackage("..exception..");

    @ArchTest
    static final ArchRule noJavaUtilLogging = noClasses().should().dependOnClassesThat()
            .resideInAPackage("java.util.logging..");
}
