package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ListProfessionalCategoriesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalCategoryServiceTest {

    @Mock
    private ListProfessionalCategoriesUseCase listProfessionalCategories;

    @InjectMocks
    private ProfessionalCategoryService service;

    @Test
    void delega_en_el_caso_de_uso_y_devuelve_su_resultado() {
        List<ProfessionalCategory> categorias = List.of();
        when(listProfessionalCategories.listProfessionalCategories()).thenReturn(categorias);

        assertThat(service.list()).isSameAs(categorias);
    }

    @Test
    void si_el_caso_de_uso_falla_la_excepcion_se_propaga() {
        IllegalStateException error = new IllegalStateException("base caida");
        when(listProfessionalCategories.listProfessionalCategories()).thenThrow(error);

        assertThatThrownBy(() -> service.list()).isSameAs(error);
    }
}
