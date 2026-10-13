package com.ndonozo.serviciocatalogosincronizacion;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureRulesFixtureTest {

    private static final String FIXTURES = "com.ndonozo.arquitecturafixtures.violar";

    @Test
    void ModuloSoloTieneTresCapas_detecta_un_adapter_fuera_de_las_tres_capas() {
        assertViolacion(ArchitectureRules.MODULO_SOLO_TIENE_TRES_CAPAS, "ModuloSoloTieneTresCapas");
    }

    @Test
    void DominioEsPuro_detecta_un_import_de_framework() {
        assertViolacion(ArchitectureRules.DOMINIO_ES_PURO, "DominioEsPuro");
    }

    @Test
    void AplicacionNoConoceInfraestructura_detecta_un_import_de_infrastructure() {
        assertViolacion(ArchitectureRules.APLICACION_NO_CONOCE_INFRAESTRUCTURA, "AplicacionNoConoceInfraestructura");
    }

    @Test
    void WebNoVePersistenciaNiPuertosDeSalida_detecta_un_import_de_ports_out() {
        assertViolacion(ArchitectureRules.WEB_NO_VE_PERSISTENCIA_NI_PUERTOS_DE_SALIDA, "WebNoVePersistenciaNiPuertosDeSalida");
    }

    @Test
    void PuertosDeEntradaSoloSeImplementanEnAplicacion_detecta_un_use_case_en_el_dominio() {
        assertViolacion(ArchitectureRules.PUERTOS_DE_ENTRADA_SOLO_SE_IMPLEMENTAN_EN_APLICACION, "PuertosDeEntradaSoloSeImplementanEnAplicacion");
    }

    @Test
    void ServiceSoloEnLasFachadas_detecta_un_service_en_usecases() {
        assertViolacion(ArchitectureRules.SERVICE_SOLO_EN_LAS_FACHADAS, "ServiceSoloEnLasFachadas");
    }

    @Test
    void SinInyeccionPorCampo_detecta_un_autowired_en_un_campo() {
        assertViolacion(ArchitectureRules.SIN_INYECCION_POR_CAMPO, "SinInyeccionPorCampo");
    }

    private static void assertViolacion(ArchRule regla, String paquete) {
        JavaClasses clases = new ClassFileImporter().importPackages(FIXTURES + paquete);
        assertThat(regla.evaluate(clases).hasViolation())
                .as("la regla debia reportar una violacion sobre el fixture %s", paquete)
                .isTrue();
    }
}