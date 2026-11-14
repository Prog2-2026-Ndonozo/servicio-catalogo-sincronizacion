package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.exception.ProfessionalNotFoundException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.GetProfessionalAgendaUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalAgendaServiceTest {

    @Mock
    private GetProfessionalAgendaUseCase getProfessionalAgenda;

    @InjectMocks
    private ProfessionalAgendaService service;

    @Test
    void devuelve_la_agenda_que_entrega_el_caso_de_uso() {
        Professional p = Professional.builder().id(201L).categoryId(101L).firstName("Ana").lastName("Torres")
                .enabled(true).createdAt(Instant.parse("2026-09-01T03:00:00Z"))
                .updatedAt(Instant.parse("2026-09-01T03:00:00Z")).build();
        ProfessionalAgenda agenda = new ProfessionalAgenda(p, List.of());
        when(getProfessionalAgenda.getProfessionalAgenda(201)).thenReturn(Optional.of(agenda));

        assertThat(service.getAgenda(201)).isSameAs(agenda);
    }

    @Test
    void si_el_profesional_no_existe_lanza_la_excepcion_de_negocio_con_su_id() {
        when(getProfessionalAgenda.getProfessionalAgenda(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAgenda(999))
                .isInstanceOf(ProfessionalNotFoundException.class)
                .hasMessageContaining("999")
                .extracting(e -> ((ProfessionalNotFoundException) e).getProfessionalId()).isEqualTo(999L);
    }

    @Test
    void si_el_caso_de_uso_falla_la_excepcion_se_propaga() {
        IllegalStateException error = new IllegalStateException("base caida");
        when(getProfessionalAgenda.getProfessionalAgenda(201)).thenThrow(error);

        assertThatThrownBy(() -> service.getAgenda(201)).isSameAs(error);
    }
}
