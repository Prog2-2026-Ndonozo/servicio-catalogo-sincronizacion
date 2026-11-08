package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogChanges;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.VersionWindow;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.mapper.CatedraRedisMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Usa el JsonMapper y el mapper reales; solo Redis es un mock. Así se prueba el recorrido
 * Redis -> JSON -> DTO -> dominio y la traducción de errores.
 */
@ExtendWith(MockitoExtension.class)
class CatedraRedisAdapterTest {

    private static final String METADATA = "catedra:sync:metadata";
    private static final String CATEGORIAS = "catedra:sync:professional-categories";
    private static final String PROFESIONALES = "catedra:sync:professionals";
    private static final String HORARIOS = "catedra:sync:weekly-schedules";

    private static final String CAMBIOS_7 = """
            {"version": 7, "changes": {"professionalCategories": [], "professionals": [15], "weeklySchedules": []}}
            """;

    private static final String ANA = """
            {"id": 15, "categoryId": 1, "firstName": "Ana", "lastName": "Gomez", "enabled": true,
             "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
            """;

    private static final String LUIS = """
            {"id": 16, "categoryId": 1, "firstName": "Luis", "lastName": "Perez", "enabled": false,
             "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
            """;

    private static final String CATEGORIA = """
            {"id": 1, "name": "Clinica medica", "description": "Atencion clinica general", "enabled": true,
             "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
            """;

    private static final String HORARIO = """
            {"id": 21, "professionalId": 15, "dayOfWeek": "MONDAY", "startTime": "09:00:00", "endTime": "12:00:00",
             "slotDurationMinutes": 30, "enabled": true,
             "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
            """;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @Mock
    private ValueOperations<String, String> valueOps;

    private CatedraRedisAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CatedraRedisAdapter(redis, JsonMapper.builder().build(), new CatedraRedisMapper());
    }

    private void conHash() {
        when(redis.opsForHash()).thenReturn(hashOps);
    }

    private void conValue() {
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    // ----- fetchVersionWindow -----

    @Test
    void lee_la_ventana_de_versiones_del_hash_de_metadata() {
        conHash();
        when(hashOps.entries(METADATA)).thenReturn(Map.of(
                "currentVersion", "7", "oldestAvailableVersion", "4", "publishedAt", "2026-09-10T13:44:59Z"));

        VersionWindow ventana = adapter.fetchVersionWindow();

        assertThat(ventana.getCurrentVersion()).isEqualTo(7L);
        assertThat(ventana.getOldestAvailableVersion()).isEqualTo(4L);
    }

    @Test
    void una_metadata_inexistente_es_un_dato_invalido() {
        conHash();
        when(hashOps.entries(METADATA)).thenReturn(Map.of());

        assertThatThrownBy(() -> adapter.fetchVersionWindow())
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void una_version_no_numerica_en_la_metadata_es_un_dato_invalido() {
        conHash();
        when(hashOps.entries(METADATA)).thenReturn(Map.of("currentVersion", "siete", "oldestAvailableVersion", "4"));

        assertThatThrownBy(() -> adapter.fetchVersionWindow())
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void si_redis_no_responde_al_leer_la_metadata_la_catedra_figura_no_disponible() {
        conHash();
        RedisConnectionFailureException causa = new RedisConnectionFailureException("sin conexion");
        when(hashOps.entries(METADATA)).thenThrow(causa);

        assertThatThrownBy(() -> adapter.fetchVersionWindow())
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCause(causa);
    }

    // ----- fetchChanges -----

    @Test
    void lee_los_cambios_de_una_version() {
        conValue();
        when(valueOps.get("catedra:sync:changes:7")).thenReturn(CAMBIOS_7);

        CatalogChanges cambios = adapter.fetchChanges(7);

        assertThat(cambios.getVersion()).isEqualTo(7L);
        assertThat(cambios.getCategoryIds()).isEmpty();
        assertThat(cambios.getProfessionalIds()).containsExactly(15L);
        assertThat(cambios.getWeeklySchedulesIds()).isEmpty();
    }

    @Test
    void una_version_sin_clave_en_redis_es_un_dato_invalido() {
        conValue();
        when(valueOps.get("catedra:sync:changes:7")).thenReturn(null);

        assertThatThrownBy(() -> adapter.fetchChanges(7))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("7");
    }

    @Test
    void un_json_ilegible_en_los_cambios_es_un_dato_invalido() {
        conValue();
        when(valueOps.get("catedra:sync:changes:7")).thenReturn("no es json");

        assertThatThrownBy(() -> adapter.fetchChanges(7))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("catedra:sync:changes:7");
    }

    @Test
    void un_json_null_en_los_cambios_es_un_dato_invalido() {
        conValue();
        when(valueOps.get("catedra:sync:changes:7")).thenReturn("null");

        assertThatThrownBy(() -> adapter.fetchChanges(7))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void unos_cambios_que_declaran_otra_version_se_rechazan() {
        conValue();
        when(valueOps.get("catedra:sync:changes:8")).thenReturn(CAMBIOS_7);

        assertThatThrownBy(() -> adapter.fetchChanges(8))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("8")
                .hasMessageContaining("7");
    }

    @Test
    void si_redis_no_responde_al_leer_los_cambios_la_catedra_figura_no_disponible() {
        conValue();
        RedisConnectionFailureException causa = new RedisConnectionFailureException("sin conexion");
        when(valueOps.get("catedra:sync:changes:7")).thenThrow(causa);

        assertThatThrownBy(() -> adapter.fetchChanges(7))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCause(causa);
    }

    // ----- fetchProfessional -----

    @Test
    void lee_varios_profesionales_con_una_sola_consulta_y_en_el_orden_pedido() {
        conHash();
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("15", "16"))).thenReturn(List.of(ANA, LUIS));

        List<Professional> profesionales = adapter.fetchProfessional(List.of(15L, 16L));

        assertThat(profesionales).extracting(Professional::getId).containsExactly(15L, 16L);
        assertThat(profesionales.get(1).isEnabled()).isFalse();
    }

    @Test
    void un_profesional_deshabilitado_se_devuelve_como_cualquier_otro() {
        conHash();
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("16"))).thenReturn(List.of(LUIS));

        assertThat(adapter.fetchProfessional(List.of(16L)))
                .singleElement()
                .satisfies(p -> assertThat(p.isEnabled()).isFalse());
    }

    @Test
    void con_ids_repetidos_pide_cada_uno_una_sola_vez() {
        conHash();
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("15"))).thenReturn(List.of(ANA));

        List<Professional> profesionales = adapter.fetchProfessional(List.of(15L, 15L));

        assertThat(profesionales).hasSize(1);
    }

    @Test
    void sin_ids_devuelve_una_lista_vacia_sin_consultar_a_redis() {
        assertThat(adapter.fetchProfessional(List.of())).isEmpty();
        assertThat(adapter.fetchCategories(List.of())).isEmpty();
        assertThat(adapter.fetchWeeklySchedule(List.of())).isEmpty();

        verifyNoInteractions(redis);
    }

    @Test
    void un_id_que_no_esta_en_el_hash_es_un_dato_invalido_y_se_informan_todos_los_faltantes() {
        conHash();
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("15", "16", "17")))
                .thenReturn(Arrays.asList(ANA, null, null));

        assertThatThrownBy(() -> adapter.fetchProfessional(List.of(15L, 16L, 17L)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("[16, 17]")
                .hasMessageContaining(PROFESIONALES);
    }

    @Test
    void un_json_ilegible_de_una_entidad_es_un_dato_invalido_con_su_origen() {
        conHash();
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("15"))).thenReturn(List.of("no es json"));

        assertThatThrownBy(() -> adapter.fetchProfessional(List.of(15L)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining(PROFESIONALES + "/15");
    }

    @Test
    void una_entidad_con_datos_invalidos_para_el_dominio_se_rechaza() {
        conHash();
        String sinNombre = ANA.replace("\"firstName\": \"Ana\"", "\"firstName\": \" \"");
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("15"))).thenReturn(List.of(sinNombre));

        assertThatThrownBy(() -> adapter.fetchProfessional(List.of(15L)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void si_redis_no_responde_al_leer_entidades_la_catedra_figura_no_disponible() {
        conHash();
        RedisConnectionFailureException causa = new RedisConnectionFailureException("sin conexion");
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("15"))).thenThrow(causa);

        assertThatThrownBy(() -> adapter.fetchProfessional(List.of(15L)))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCause(causa);
    }

    @Test
    void el_mensaje_de_indisponibilidad_no_expone_detalles_de_la_conexion() {
        conHash();
        when(hashOps.multiGet(PROFESIONALES, List.<Object>of("15")))
                .thenThrow(new RedisConnectionFailureException("redis://usuario:secreto@host:6379"));

        assertThatThrownBy(() -> adapter.fetchProfessional(List.of(15L)))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasMessageNotContaining("secreto")
                .hasMessageNotContaining("usuario");
    }

    // ----- fetchCategories / fetchWeeklySchedule -----

    @Test
    void lee_categorias_del_hash_de_categorias() {
        conHash();
        when(hashOps.multiGet(CATEGORIAS, List.<Object>of("1"))).thenReturn(List.of(CATEGORIA));

        List<ProfessionalCategory> categorias = adapter.fetchCategories(List.of(1L));

        assertThat(categorias).singleElement().satisfies(c -> {
            assertThat(c.getId()).isEqualTo(1L);
            assertThat(c.getName()).isEqualTo("Clinica medica");
        });
        verify(hashOps).multiGet(CATEGORIAS, List.<Object>of("1"));
    }

    @Test
    void lee_horarios_del_hash_de_horarios() {
        conHash();
        when(hashOps.multiGet(HORARIOS, List.<Object>of("21"))).thenReturn(List.of(HORARIO));

        List<WeeklySchedule> horarios = adapter.fetchWeeklySchedule(List.of(21L));

        assertThat(horarios).singleElement().satisfies(h -> {
            assertThat(h.getId()).isEqualTo(21L);
            assertThat(h.getProfessionalId()).isEqualTo(15L);
            assertThat(h.getSlotDurationMinutes()).isEqualTo(30);
        });
    }

    @Test
    void un_id_faltante_en_categorias_o_en_horarios_tambien_es_un_dato_invalido() {
        conHash();
        when(hashOps.multiGet(CATEGORIAS, List.<Object>of("1"))).thenReturn(Arrays.asList((Object) null));
        when(hashOps.multiGet(HORARIOS, List.<Object>of("21"))).thenReturn(Arrays.asList((Object) null));

        assertThatThrownBy(() -> adapter.fetchCategories(List.of(1L)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining(CATEGORIAS);
        assertThatThrownBy(() -> adapter.fetchWeeklySchedule(List.of(21L)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining(HORARIOS);
    }
}
