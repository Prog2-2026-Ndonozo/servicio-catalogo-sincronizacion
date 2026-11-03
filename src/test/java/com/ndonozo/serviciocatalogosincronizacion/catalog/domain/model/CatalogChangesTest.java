package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogChangesTest {

    private static CatalogChanges.CatalogChangesBuilder cambiosValidos() {
        return CatalogChanges.builder()
                .version(7L)
                .categoryIds(List.of())
                .professionalIds(List.of(15L))
                .weeklySchedulesIds(List.of());
    }

    @Test
    void se_crea_correctamente_con_el_ejemplo_del_contrato() {
        CatalogChanges cambios = cambiosValidos().build();

        assertThat(cambios.getVersion()).isEqualTo(7L);
        assertThat(cambios.getCategoryIds()).isEmpty();
        assertThat(cambios.getProfessionalIds()).containsExactly(15L);
        assertThat(cambios.getWeeklySchedulesIds()).isEmpty();
    }

    @Test
    void acepta_una_version_con_las_tres_listas_vacias() {
        CatalogChanges cambios = cambiosValidos().professionalIds(List.of()).build();

        assertThat(cambios.getCategoryIds()).isEmpty();
        assertThat(cambios.getProfessionalIds()).isEmpty();
        assertThat(cambios.getWeeklySchedulesIds()).isEmpty();
    }

    @Test
    void conserva_varios_ids_en_el_orden_recibido() {
        CatalogChanges cambios = cambiosValidos().weeklySchedulesIds(List.of(21L, 3L, 9L)).build();

        assertThat(cambios.getWeeklySchedulesIds()).containsExactly(21L, 3L, 9L);
    }

    @Test
    void acepta_la_version_mas_chica_posible() {
        assertThat(cambiosValidos().version(1L).build().getVersion()).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    void rechaza_version_no_positiva(long versionInvalida) {
        assertThatThrownBy(() -> cambiosValidos().version(versionInvalida).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("version");
    }

    @Test
    void rechaza_lista_de_categorias_nula() {
        assertThatThrownBy(() -> cambiosValidos().categoryIds(null).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("categoryIds");
    }

    @Test
    void rechaza_lista_de_profesionales_nula() {
        assertThatThrownBy(() -> cambiosValidos().professionalIds(null).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("professionalIds");
    }

    @Test
    void rechaza_lista_de_horarios_nula() {
        assertThatThrownBy(() -> cambiosValidos().weeklySchedulesIds(null).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("weeklySchedulesIds");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rechaza_ids_no_positivos_en_cada_lista(long idInvalido) {
        assertThatThrownBy(() -> cambiosValidos().categoryIds(List.of(1L, idInvalido)).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("categoryIds");
        assertThatThrownBy(() -> cambiosValidos().professionalIds(List.of(idInvalido)).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("professionalIds");
        assertThatThrownBy(() -> cambiosValidos().weeklySchedulesIds(List.of(idInvalido, 2L)).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("weeklySchedulesIds");
    }

    @Test
    void rechaza_un_id_nulo_dentro_de_la_lista() {
        List<Long> conNulo = Arrays.asList(1L, null);

        assertThatThrownBy(() -> cambiosValidos().professionalIds(conNulo).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("professionalIds");
    }

    @Test
    void las_listas_expuestas_no_se_pueden_modificar() {
        CatalogChanges cambios = cambiosValidos().build();

        assertThatThrownBy(() -> cambios.getProfessionalIds().add(99L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void modificar_la_lista_original_despues_de_construir_no_cambia_el_objeto() {
        List<Long> original = new ArrayList<>(List.of(15L));
        CatalogChanges cambios = cambiosValidos().professionalIds(original).build();

        original.add(99L);

        assertThat(cambios.getProfessionalIds()).containsExactly(15L);
    }
}
