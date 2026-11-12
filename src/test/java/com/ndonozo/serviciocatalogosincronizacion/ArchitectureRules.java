package com.ndonozo.serviciocatalogosincronizacion;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

final class ArchitectureRules {

    static final String DOMINIO = "..domain..";
    static final String APLICACION = "..application..";
    static final String INFRAESTRUCTURA = "..infrastructure..";
    static final String CONFIG = "..config..";
    static final String PUERTOS_DE_ENTRADA = "..domain.ports.in..";
    static final String PUERTOS_DE_SALIDA = "..domain.ports.out..";
    static final String WEB = "..infrastructure.web..";
    static final String FACHADAS = "..application.service..";
    static final String PERSISTENCIA = "..infrastructure.persistence..";
    static final String MODULO = "..catalog..";
    static final String CATEDRA_REST = "..infrastructure.catedrarest..";
    static final String CATEDRA_REDIS = "..infrastructure.catedraredis..";

    static final ArchRule MODULO_SOLO_TIENE_TRES_CAPAS = noClasses()
            .that().resideInAPackage(MODULO)
            .should().resideOutsideOfPackages(DOMINIO, APLICACION, INFRAESTRUCTURA)
            .allowEmptyShould(true);

    static final ArchRule DOMINIO_ES_PURO = noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    APLICACION,
                    INFRAESTRUCTURA,
                    CONFIG,
                    "org.springframework..",
                    "jakarta..",
                    "com.fasterxml..")
            .allowEmptyShould(true);

    static final ArchRule APLICACION_NO_CONOCE_INFRAESTRUCTURA = noClasses()
            .that().resideInAPackage(APLICACION)
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    INFRAESTRUCTURA,
                    CONFIG,
                    "org.springframework.web..",
                    "org.springframework.data..",
                    "org.springframework.kafka..",
                    "jakarta.persistence..",
                    "com.fasterxml..")
            .allowEmptyShould(true);

    static final ArchRule WEB_NO_VE_PERSISTENCIA_NI_PUERTOS_DE_SALIDA = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat()
            .resideInAnyPackage(PUERTOS_DE_SALIDA, PERSISTENCIA)
            .allowEmptyShould(true);

    static final ArchRule PUERTOS_DE_ENTRADA_SOLO_SE_IMPLEMENTAN_EN_APLICACION = classes()
            .that().implement(JavaClass.Predicates.resideInAPackage(PUERTOS_DE_ENTRADA))
            .should().resideInAPackage(APLICACION)
            .allowEmptyShould(true);

    static final ArchRule SERVICE_SOLO_EN_LAS_FACHADAS = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(FACHADAS)
            .allowEmptyShould(true);

    static final ArchRule SIN_INYECCION_POR_CAMPO = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .allowEmptyShould(true);

    /**
     * E §6.2: las busquedas se resuelven con la copia local y nunca consultan a la catedra. Solo los casos de uso de
     * sincronizacion pueden usar los puertos de lectura de la catedra (los adaptadores que los implementan quedan
     * afuera de la regla).
     */
    static final ArchRule SOLO_LA_SINCRONIZACION_USA_LOS_PUERTOS_DE_LA_CATEDRA = noClasses()
            .that().resideOutsideOfPackages(CATEDRA_REST, CATEDRA_REDIS)
            .and().doNotHaveSimpleName("ApplyFullSnapshotUseCaseImpl")
            .and().doNotHaveSimpleName("ApplyIncrementalChangeUseCaseImpl")
            .should().dependOnClassesThat(
                    simpleName("CatedraSnapshotPort").or(simpleName("CatedraCatalogReadPort")))
            .allowEmptyShould(true);

    /** Complementa la anterior: ni la web ni la persistencia usan los clientes REST o Redis de la catedra. */
    static final ArchRule WEB_Y_PERSISTENCIA_NO_USAN_LA_CATEDRA = noClasses()
            .that().resideInAnyPackage(WEB, PERSISTENCIA)
            .should().dependOnClassesThat()
            .resideInAnyPackage(CATEDRA_REST, CATEDRA_REDIS, "org.springframework.data.redis..")
            .allowEmptyShould(true);

    private ArchitectureRules() {
    }
}