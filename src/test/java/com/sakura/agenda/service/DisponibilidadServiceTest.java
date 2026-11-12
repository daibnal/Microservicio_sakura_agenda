package com.sakura.agenda.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sakura.agenda.client.AusenciaDTO;
import com.sakura.agenda.client.CatalogoCliente;
import com.sakura.agenda.client.ConfiguracionCliente;
import com.sakura.agenda.client.ConfiguracionDTO;
import com.sakura.agenda.client.PersonalCliente;
import com.sakura.agenda.client.ProfesionalDTO;
import com.sakura.agenda.client.ServicioDTO;
import com.sakura.agenda.client.TurnoDTO;
import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.Bloqueo;
import com.sakura.agenda.model.Hueco;
import com.sakura.agenda.model.Reserva;
import com.sakura.agenda.repository.BloqueoRepository;
import com.sakura.agenda.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock
    private PersonalCliente personalCliente;

    @Mock
    private CatalogoCliente catalogoCliente;

    @Mock
    private BloqueoRepository bloqueoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ConfiguracionCliente configuracionCliente;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    private Long profesionalId;
    private Long servicioId;
    private LocalDate fechaFutura;
    private LocalTime horaValida;

    @BeforeEach
    void setUp() {
        profesionalId = 1L;
        servicioId = 10L;
        fechaFutura = LocalDate.now().plusDays(5);
        horaValida = LocalTime.of(10, 0);

        // Configuración por defecto para anticipación
        ConfiguracionDTO config = new ConfiguracionDTO();
        config.setDiasAnticipacionMaxima(30);
        config.setHorasAnticipacionMinima(2);
        lenient().when(configuracionCliente.obtener()).thenReturn(config);
    }

    private ProfesionalDTO crearProfesional(boolean activo) {
        ProfesionalDTO dto = new ProfesionalDTO();
        dto.setId(profesionalId);
        dto.setActivo(activo);
        return dto;
    }

    private ServicioDTO crearServicio(boolean activo, int duracionMinutos) {
        ServicioDTO dto = new ServicioDTO();
        dto.setId(servicioId);
        dto.setActivo(activo);
        dto.setDuracionMinutos(duracionMinutos);
        return dto;
    }

    private TurnoDTO crearTurno(int diaSemana, LocalTime inicio, LocalTime fin, LocalTime almuerzoInicio, LocalTime almuerzoFin) {
        TurnoDTO turno = new TurnoDTO();
        turno.setDiaSemana(diaSemana);
        turno.setHoraInicio(inicio);
        turno.setHoraFin(fin);
        turno.setHoraAlmuerzoInicio(almuerzoInicio);
        turno.setHoraAlmuerzoFin(almuerzoFin);
        return turno;
    }

    @Nested
    @DisplayName("Pruebas para verificarLibre(...)")
    class VerificarLibreTests {

        @Test
        @DisplayName("Lanza excepción si el profesional no está activo")
        void verificarLibre_ProfesionalInactivo_LanzaExcepcion() {
            when(catalogoCliente.obtener(servicioId)).thenReturn(crearServicio(true, 30));
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(false));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, null));

            assertEquals("El profesional no está disponible", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si el servicio no está activo")
        void verificarLibre_ServicioInactivo_LanzaExcepcion() {
            when(catalogoCliente.obtener(servicioId)).thenReturn(crearServicio(false, 30));
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, null));

            assertEquals("El servicio no está disponible", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si el profesional no realiza el servicio")
        void verificarLibre_ProfesionalNoRealizaServicio_LanzaExcepcion() {
            when(catalogoCliente.obtener(servicioId)).thenReturn(crearServicio(true, 30));
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(Collections.emptyList());

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, null));

            assertEquals("El profesional no realiza este servicio", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si el profesional está ausente en la fecha")
        void verificarLibre_ProfesionalAusente_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));

            AusenciaDTO ausencia = new AusenciaDTO();
            ausencia.setFechaInicio(fechaFutura.minusDays(1));
            ausencia.setFechaFin(fechaFutura.plusDays(1));
            when(personalCliente.ausencias(profesionalId)).thenReturn(List.of(ausencia));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, null));

            assertEquals("El profesional no atiende en esa fecha", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si la fecha es en el pasado")
        void verificarLibre_FechaPasada_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            LocalDate fechaPasada = LocalDate.now().minusDays(1);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaPasada, horaValida, null));

            assertEquals("No se puede agendar en una fecha pasada", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si la fecha supera la anticipación máxima")
        void verificarLibre_SuperaAnticipacionMaxima_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            LocalDate fechaMuyLejana = LocalDate.now().plusDays(35);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaMuyLejana, horaValida, null));

            assertEquals("No se puede agendar con tanta anticipación", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si la hora no cumple con el margen mínimo de anticipación")
        void verificarLibre_NoCumpleAnticipacionMinima_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            // Hoy, pero con menos de 2 horas de diferencia (horasMinimas = 2)
            LocalDate hoy = LocalDate.now();
            LocalTime horaProxima = LocalTime.now().plusMinutes(30);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, hoy, horaProxima, null));

            assertEquals("Se requiere mayor margen de anticipación", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si la duración del servicio excede el día (cambio de fecha)")
        void verificarLibre_DuracionPasaDeDia_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 120); // 2 horas
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            LocalTime horaNoche = LocalTime.of(23, 30);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaNoche, null));

            assertEquals("El horario está fuera del turno del profesional", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si es el día de hoy y la hora elegida ya pasó")
        void verificarLibre_HoyHoraYaPaso_LanzaExcepcion() {
            // Un margen negativo deja pasar la hora reciente para llegar a la validación de "ya pasó".
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setDiasAnticipacionMaxima(30);
            config.setHorasAnticipacionMinima(-1);
            when(configuracionCliente.obtener()).thenReturn(config);

            ServicioDTO servicio = crearServicio(true, 1);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            LocalDate hoy = LocalDate.now();
            LocalTime candidata = LocalTime.now().minusMinutes(10);
            LocalTime horaPasada = candidata.isAfter(LocalTime.now()) ? LocalTime.MIDNIGHT : candidata;

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, hoy, horaPasada, null));

            assertEquals("Ese horario ya pasó", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si el horario está fuera del turno del profesional")
        void verificarLibre_FueraDeTurno_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 60);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            // Turno de 08:00 a 12:00
            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(8, 0), LocalTime.of(12, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            LocalTime horaFuera = LocalTime.of(13, 0);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaFuera, null));

            assertEquals("El horario está fuera del turno del profesional", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si el horario cae en descanso/almuerzo")
        void verificarLibre_CaeEnAlmuerzo_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 60);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(8, 0), LocalTime.of(18, 0), LocalTime.of(13, 0), LocalTime.of(14, 0));
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            LocalTime horaAlmuerzo = LocalTime.of(13, 0);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaAlmuerzo, null));

            assertEquals("El horario corresponde al descanso del profesional", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si existe un bloqueo administrativo en el tramo")
        void verificarLibre_BloqueadoPorAdministracion_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(8, 0), LocalTime.of(18, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            when(bloqueoRepository.buscarCruces(eq(fechaFutura), eq(horaValida), eq(horaValida.plusMinutes(30))))
                .thenReturn(List.of(new Bloqueo()));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, null));

            assertEquals("El horario está bloqueado por la administración", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si el horario está ocupado por otra reserva")
        void verificarLibre_OcupadoPorReserva_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(8, 0), LocalTime.of(18, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());

            Reserva reservaExistente = new Reserva();
            reservaExistente.setId(100L);
            when(reservaRepository.buscarCruces(eq(profesionalId), eq(fechaFutura), eq(horaValida), eq(horaValida.plusMinutes(30)), any()))
                .thenReturn(List.of(reservaExistente));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, null));

            assertEquals("El horario ya no está disponible", ex.getMessage());
        }

        @Test
        @DisplayName("Debe permitir agendar cuando la reserva existente coincide con ignorarReservaId (Edición)")
        void verificarLibre_IgnoraMismaReserva_Exito() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(8, 0), LocalTime.of(18, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());

            Long reservaId = 100L;
            Reserva reservaExistente = new Reserva();
            reservaExistente.setId(reservaId);

            when(reservaRepository.buscarCruces(eq(profesionalId), eq(fechaFutura), eq(horaValida), eq(horaValida.plusMinutes(30)), any()))
                .thenReturn(List.of(reservaExistente));

            // No debería lanzar ninguna excepción
            assertDoesNotThrow(() -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, reservaId));
        }

        @Test
        @DisplayName("Verificación exitosa en el camino feliz")
        void verificarLibre_CaminoFeliz_Exito() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(8, 0), LocalTime.of(18, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, null));
        }

        @Test
        @DisplayName("Permite una hora de hoy que todavía no pasa")
        void verificarLibre_HoyHoraFutura_NoLanzaPorHorarioPasado() {
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setDiasAnticipacionMaxima(30);
            config.setHorasAnticipacionMinima(-1);
            when(configuracionCliente.obtener()).thenReturn(config);

            LocalTime ahora = LocalTime.now();
            LocalTime horaFutura = ahora.plusMinutes(30);
            org.junit.jupiter.api.Assumptions.assumeTrue(horaFutura.isAfter(ahora));

            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());
            when(personalCliente.turnos(profesionalId)).thenReturn(Collections.emptyList());

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> disponibilidadService.verificarLibre(profesionalId, servicioId, LocalDate.now(), horaFutura, null));

            assertEquals("El horario está fuera del turno del profesional", ex.getMessage());
        }

        @Test
        @DisplayName("Sigue ocupado si la reserva cruzada no es la que se está editando")
        void verificarLibre_IgnoraOtraReserva_SigueOcupado() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());
            TurnoDTO turno = crearTurno(fechaFutura.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(18, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());

            Reserva otra = new Reserva();
            otra.setId(100L);
            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(List.of(otra));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, horaValida, 999L));

            assertEquals("El horario ya no está disponible", ex.getMessage());
        }

        @Test
        @DisplayName("Lanza excepción si la cita empieza antes del turno")
        void verificarLibre_EmpiezaAntesDelTurno_LanzaExcepcion() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());
            TurnoDTO turno = crearTurno(fechaFutura.getDayOfWeek().getValue(), LocalTime.of(10, 0), LocalTime.of(18, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> disponibilidadService.verificarLibre(profesionalId, servicioId, fechaFutura, LocalTime.of(9, 0), null));

            assertEquals("El horario está fuera del turno del profesional", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Pruebas para listarHuecos(...)")
    class ListarHuecosTests {

        @Test
        @DisplayName("Retorna lista vacía si el profesional está inactivo")
        void listarHuecos_ProfesionalInactivo_RetornaListaVacia() {
            when(catalogoCliente.obtener(servicioId)).thenReturn(crearServicio(true, 30));
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(false));

            List<Hueco> resultado = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Retorna lista vacía si el servicio está inactivo")
        void listarHuecos_ServicioInactivo_RetornaListaVacia() {
            when(catalogoCliente.obtener(servicioId)).thenReturn(crearServicio(false, 30));
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));

            List<Hueco> resultado = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Retorna lista vacía si el profesional no realiza el servicio")
        void listarHuecos_NoRealizaServicio_RetornaListaVacia() {
            when(catalogoCliente.obtener(servicioId)).thenReturn(crearServicio(true, 30));
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(Collections.emptyList());

            List<Hueco> resultado = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Retorna lista vacía si el profesional está ausente")
        void listarHuecos_ProfesionalAusente_RetornaListaVacia() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));

            AusenciaDTO ausencia = new AusenciaDTO();
            ausencia.setFechaInicio(fechaFutura.minusDays(1));
            ausencia.setFechaFin(fechaFutura.plusDays(1));
            when(personalCliente.ausencias(profesionalId)).thenReturn(List.of(ausencia));

            List<Hueco> resultado = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Retorna lista vacía si la fecha es pasada o excede días máximos")
        void listarHuecos_FechaInvalida_RetornaListaVacia() {
            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            List<Hueco> resultadoPasado = disponibilidadService.listarHuecos(profesionalId, servicioId, LocalDate.now().minusDays(1));
            List<Hueco> resultadoLejano = disponibilidadService.listarHuecos(profesionalId, servicioId, LocalDate.now().plusDays(40));

            assertTrue(resultadoPasado.isEmpty());
            assertTrue(resultadoLejano.isEmpty());
        }

        @Test
        @DisplayName("Genera huecos correctamente ignorando almuerzos, bloqueos y reservas")
        void listarHuecos_GeneraHuecosValidos_Exito() {
            ServicioDTO servicio = crearServicio(true, 60); // 1 hora de duración
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            // Turno de 09:00 a 12:00 (Huecos posibles: 09-10, 10-11, 11-12)
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(9, 0), LocalTime.of(12, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            // Bloquear tramo de 10:00 a 11:00
            when(bloqueoRepository.buscarCruces(fechaFutura, LocalTime.of(9, 0), LocalTime.of(10, 0))).thenReturn(Collections.emptyList());
            when(bloqueoRepository.buscarCruces(fechaFutura, LocalTime.of(10, 0), LocalTime.of(11, 0))).thenReturn(List.of(new Bloqueo()));
            when(bloqueoRepository.buscarCruces(fechaFutura, LocalTime.of(11, 0), LocalTime.of(12, 0))).thenReturn(Collections.emptyList());

            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            // Debe devolver 2 huecos (09:00 a 10:00 y 11:00 a 12:00)
            assertEquals(2, huecos.size());
            assertEquals(LocalTime.of(9, 0), huecos.get(0).getHoraInicio());
            assertEquals(LocalTime.of(10, 0), huecos.get(0).getHoraFin());
            assertEquals(LocalTime.of(11, 0), huecos.get(1).getHoraInicio());
            assertEquals(LocalTime.of(12, 0), huecos.get(1).getHoraFin());
        }

        @Test
        @DisplayName("Omite huecos de almuerzo, ocupados y turnos de otro día")
        void listarHuecos_OmiteAlmuerzoOcupadosYTurnoDeOtroDia() {
            ServicioDTO servicio = crearServicio(true, 60);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));

            AusenciaDTO ausenciaPasada = new AusenciaDTO();
            ausenciaPasada.setFechaInicio(fechaFutura.minusDays(10));
            ausenciaPasada.setFechaFin(fechaFutura.minusDays(5));
            AusenciaDTO ausenciaFutura = new AusenciaDTO();
            ausenciaFutura.setFechaInicio(fechaFutura.plusDays(3));
            ausenciaFutura.setFechaFin(fechaFutura.plusDays(4));
            when(personalCliente.ausencias(profesionalId)).thenReturn(List.of(ausenciaPasada, ausenciaFutura));

            int diaSemana = fechaFutura.getDayOfWeek().getValue();
            TurnoDTO sinDia = crearTurno(diaSemana, LocalTime.of(9, 0), LocalTime.of(12, 0), null, null);
            sinDia.setDiaSemana(null);
            TurnoDTO otroDia = crearTurno(diaSemana == 7 ? 1 : diaSemana + 1, LocalTime.of(9, 0), LocalTime.of(12, 0), null, null);
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.of(9, 0), LocalTime.of(12, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(sinDia, otroDia, turno));

            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
            Reserva ocupada = new Reserva();
            ocupada.setId(50L);
            when(reservaRepository.buscarCruces(eq(profesionalId), eq(fechaFutura), eq(LocalTime.of(11, 0)), eq(LocalTime.of(12, 0)), any()))
                    .thenReturn(List.of(ocupada));

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertEquals(1, huecos.size());
            assertEquals(LocalTime.of(9, 0), huecos.get(0).getHoraInicio());
            assertEquals(LocalTime.of(10, 0), huecos.get(0).getHoraFin());
        }

        @Test
        @DisplayName("Omite los huecos de hoy que ya pasaron")
        void listarHuecos_HoyYaPaso_OmiteEsosHuecos() {
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setDiasAnticipacionMaxima(30);
            config.setHorasAnticipacionMinima(0);
            when(configuracionCliente.obtener()).thenReturn(config);

            ServicioDTO servicio = crearServicio(true, 30);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            int diaSemana = LocalDate.now().getDayOfWeek().getValue();
            TurnoDTO turno = crearTurno(diaSemana, LocalTime.MIDNIGHT, LocalTime.of(1, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, LocalDate.now());

            assertTrue(huecos.stream().noneMatch(hueco -> !hueco.getHoraInicio().isAfter(LocalTime.now())));
        }

        @Test
        @DisplayName("Omite huecos que quedan dentro del margen mínimo de anticipación")
        void listarHuecos_MuyPronto_RetornaListaVacia() {
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setDiasAnticipacionMaxima(30);
            config.setHorasAnticipacionMinima(48);
            when(configuracionCliente.obtener()).thenReturn(config);

            ServicioDTO servicio = crearServicio(true, 60);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            LocalDate manana = LocalDate.now().plusDays(1);
            TurnoDTO turno = crearTurno(manana.getDayOfWeek().getValue(), LocalTime.of(9, 0), LocalTime.of(12, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, manana);

            assertTrue(huecos.isEmpty());
        }

        @Test
        @DisplayName("Corta el turno cuando la duración se pasa del día")
        void listarHuecos_DuracionPasaDeMedianoche_NoGeneraHueco() {
            ServicioDTO servicio = crearServicio(true, 120);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            TurnoDTO turno = crearTurno(fechaFutura.getDayOfWeek().getValue(), LocalTime.of(23, 0), LocalTime.of(23, 59), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertTrue(huecos.isEmpty());
        }

        @Test
        @DisplayName("Corta el turno cuando la duración es cero")
        void listarHuecos_DuracionCero_NoGeneraHueco() {
            ServicioDTO servicio = crearServicio(true, 0);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            TurnoDTO turno = crearTurno(fechaFutura.getDayOfWeek().getValue(), LocalTime.of(9, 0), LocalTime.of(12, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertTrue(huecos.isEmpty());
        }

        @Test
        @DisplayName("Corta el turno cuando la duración supera las 24 horas")
        void listarHuecos_DuracionMayorAUnDia_NoGeneraHueco() {
            ServicioDTO servicio = crearServicio(true, 25 * 60);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            TurnoDTO turno = crearTurno(fechaFutura.getDayOfWeek().getValue(), LocalTime.MIDNIGHT, LocalTime.of(23, 0), null, null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertTrue(huecos.isEmpty());
        }

        @Test
        @DisplayName("Incluye un hueco de hoy que todavía no pasa")
        void listarHuecos_HoyHoraFutura_IncluyeHueco() {
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setDiasAnticipacionMaxima(30);
            config.setHorasAnticipacionMinima(-1);
            when(configuracionCliente.obtener()).thenReturn(config);

            LocalTime ahora = LocalTime.now().withNano(0);
            LocalTime inicio = ahora.plusMinutes(1);
            org.junit.jupiter.api.Assumptions.assumeTrue(inicio.isAfter(ahora) && inicio.plusMinutes(20).isAfter(inicio));

            ServicioDTO servicio = crearServicio(true, 20);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            TurnoDTO turno = crearTurno(LocalDate.now().getDayOfWeek().getValue(), inicio, inicio.plusMinutes(20), LocalTime.of(10, 0), null);
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, LocalDate.now());

            assertEquals(1, huecos.size());
            assertEquals(inicio, huecos.get(0).getHoraInicio());
        }

        @Test
        @DisplayName("Ignora el almuerzo si solo tiene hora de término")
        void listarHuecos_AlmuerzoSoloConFin_GeneraHueco() {
            ServicioDTO servicio = crearServicio(true, 60);
            when(catalogoCliente.obtener(servicioId)).thenReturn(servicio);
            when(personalCliente.obtener(profesionalId)).thenReturn(crearProfesional(true));
            when(personalCliente.servicios(profesionalId)).thenReturn(List.of(servicio));
            when(personalCliente.ausencias(profesionalId)).thenReturn(Collections.emptyList());

            TurnoDTO turno = crearTurno(fechaFutura.getDayOfWeek().getValue(), LocalTime.of(9, 0), LocalTime.of(10, 0), null, LocalTime.of(13, 0));
            when(personalCliente.turnos(profesionalId)).thenReturn(List.of(turno));
            when(bloqueoRepository.buscarCruces(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reservaRepository.buscarCruces(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            List<Hueco> huecos = disponibilidadService.listarHuecos(profesionalId, servicioId, fechaFutura);

            assertEquals(1, huecos.size());
        }
    }
}