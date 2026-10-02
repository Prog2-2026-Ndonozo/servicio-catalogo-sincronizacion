package com.ndonozo.serviciocatalogosincronizacion;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.ndonozo.serviciocatalogosincronizacion..")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_no_debe_depender_de_adapter_ni_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter..", "org.springframework..", "jakarta..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapters_out_solo_dependen_de_limites_permitidos =
        classes()
            .that().resideInAPackage("..adapter.out..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..domain..",
                "..adapter..",
                "org.springframework..",
                "jakarta..",
                "java..")
            .allowEmptyShould(true);
}