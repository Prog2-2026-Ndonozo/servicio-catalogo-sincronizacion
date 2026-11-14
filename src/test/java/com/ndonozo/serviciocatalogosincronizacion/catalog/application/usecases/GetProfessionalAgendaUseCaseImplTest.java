package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalAgendaReadPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProfessionalAgendaUseCaseImplTest {

    @Mock
    private ProfessionalAgendaReadPort readPort;

    @InjectMocks
    private GetProfessionalAgendaUseCaseImpl useCase;

    private static ProfessionalAgenda agenda() {
        Professional p = Professional.builder().id(201L).categoryId(101L).firstName("Ana").lastName("Torres")
                .enabled(true).createdAt(Instant.parse("2026-09-01T03:00:00Z"))
                .updatedAt(Instant.parse("2026-09-01T03:00:00Z")).build();
        return new ProfessionalAgenda(p, List.of());
    }

    @Test
    void delega_en_el_puerto_y_devuelve_su_resultado() {
        ProfessionalAgenda agenda = agenda();
        when(readPort.findByProfessionalId(201)).thenReturn(Optional.of(agenda));

        assertThat(useCase.getProfessionalAgenda(201)).containsSame(agenda);
        verify(readPort).findByProfessionalId(201);
        verifyNoMoreInteractions(readPort);
    }

    @Test
    void un_profesional_inexistente_devuelve_vacio_sin_lanzar() {
        when(readPort.findByProfessionalId(999)).thenReturn(Optional.empty());

        assertThat(useCase.getProfessionalAgenda(999)).isEmpty();
    }

    @Test
    void si_el_puerto_falla_la_excepcion_se_propaga() {
        IllegalStateException error = new IllegalStateException("base caida");
        when(readPort.findByProfessionalId(201)).thenThrow(error);

        assertThatThrownBy(() -> useCase.getProfessionalAgenda(201)).isSameAs(error);
    }
}
