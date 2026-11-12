package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalCategoryMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProfessionalCategoryReadAdapterTest {

    @Mock
    private JpaProfessionalCategoryRepository repository;

    private JpaProfessionalCategoryReadAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaProfessionalCategoryReadAdapter(repository, new ProfessionalCategoryMapper());
    }

    private static ProfessionalCategoryEntity entidad(long id, String nombre, boolean habilitada) {
        ProfessionalCategoryEntity e = new ProfessionalCategoryEntity();
        e.setId(id);
        e.setName(nombre);
        e.setDescription("desc " + nombre);
        e.setEnabled(habilitada);
        e.setCreatedAt(Instant.parse("2026-07-01T12:00:00Z"));
        e.setUpdatedAt(Instant.parse("2026-09-10T13:40:00Z"));
        return e;
    }

    @Test
    void convierte_cada_entidad_al_dominio_y_conserva_el_orden_del_repositorio() {
        when(repository.findAll(org.mockito.ArgumentMatchers.any(Sort.class)))
                .thenReturn(List.of(entidad(101, "Clinica medica", true), entidad(102, "Pediatria", false)));

        List<ProfessionalCategory> resultado = adapter.findAll();

        assertThat(resultado).extracting(ProfessionalCategory::getId).containsExactly(101L, 102L);
        assertThat(resultado.get(0).getName()).isEqualTo("Clinica medica");
        assertThat(resultado.get(0).getDescription()).isEqualTo("desc Clinica medica");
        assertThat(resultado.get(0).isEnabled()).isTrue();
        assertThat(resultado.get(1).isEnabled()).isFalse();
    }

    @Test
    void pide_las_categorias_ordenadas_por_nombre_e_id() {
        when(repository.findAll(org.mockito.ArgumentMatchers.any(Sort.class))).thenReturn(List.of());

        adapter.findAll();

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sort.capture());
        assertThat(sort.getValue()).isEqualTo(Sort.by("name", "id"));
    }

    @Test
    void sin_categorias_devuelve_una_lista_vacia() {
        when(repository.findAll(org.mockito.ArgumentMatchers.any(Sort.class))).thenReturn(List.of());

        assertThat(adapter.findAll()).isEmpty();
    }
}
