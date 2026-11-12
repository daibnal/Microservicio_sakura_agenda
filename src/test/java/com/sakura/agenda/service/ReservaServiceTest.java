package com.sakura.agenda.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sakura.agenda.client.CatalogoCliente;
import com.sakura.agenda.client.CategoriaDTO;
import com.sakura.agenda.client.ConfiguracionCliente;
import com.sakura.agenda.client.ConfiguracionDTO;
import com.sakura.agenda.client.PagoCliente;
import com.sakura.agenda.client.PersonalCliente;
import com.sakura.agenda.client.ProfesionalDTO;
import com.sakura.agenda.client.ServicioDTO;
import com.sakura.agenda.client.SesionDTO;
import com.sakura.agenda.exception.AccesoDenegadoException;
import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.EstadosReserva;
import com.sakura.agenda.model.PrestacionPedido;
import com.sakura.agenda.model.MoverReservaRequest;
import com.sakura.agenda.model.Reserva;
import com.sakura.agenda.model.ReservaRequest;
import com.sakura.agenda.model.ResultadoReserva;
import com.sakura.agenda.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private CatalogoCliente catalogoCliente;

    @Mock
    private PersonalCliente personalCliente;

    @Mock
    private DisponibilidadService disponibilidadService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private PagoCliente pagoCliente;

    @Mock
    private ConfiguracionCliente configuracionCliente;

    @InjectMocks
    private ReservaService reservaService;

    @Captor
    private ArgumentCaptor<Reserva> reservaCaptor;

    private static final int PLAZO_MINUTOS = 30;

    @BeforeEach
    void setUp() {
        // Inyección manual del valor @Value("${sakura.reserva.plazo-minutos}")
        ReflectionTestUtils.setField(reservaService, "plazoMinutos", PLAZO_MINUTOS);
    }

    // ==========================================
    // AUXILIARES / HELPERS
    // ==========================================

    private SesionDTO crearSesion(Long id, String nombre, String apellido, String correo, String rol) {
        SesionDTO s = new SesionDTO();
        s.setId(id);
        s.setNombre(nombre);
        s.setApellido(apellido);
        s.setCorreo(correo);
        s.setRol(rol);
        return s;
    }

    private ServicioDTO crearServicio(Long id, String nombre, int duracion, int precio, String categoriaNombre) {
        ServicioDTO s = new ServicioDTO();
        s.setId(id);
        s.setNombre(nombre);
        s.setDuracionMinutos(duracion);
        s.setPrecio(precio);
        if (categoriaNombre != null) {
            CategoriaDTO categoria = new CategoriaDTO();
            categoria.setNombre(categoriaNombre);
            s.setCategoria(categoria);
        }
        return s;
    }

    private ProfesionalDTO crearProfesional(Long id, String nombre) {
        ProfesionalDTO p = new ProfesionalDTO();
        p.setId(id);
        p.setNombre(nombre);
        return p;
    }

    private Reserva crearReservaBase(Long id, Long idCliente, String estado, LocalDate fecha, LocalTime horaInicio) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setIdCliente(idCliente);
        r.setNombreCliente("Juan Perez");
        r.setCorreoCliente("juan@example.com");
        r.setServicioId(10L);
        r.setNombreServicio("Corte de Cabello");
        r.setDuracionMinutos(30);
        r.setProfesionalId(20L);
        r.setNombreProfesional("Ana");
        r.setFecha(fecha);
        r.setHoraInicio(horaInicio);
        r.setHoraFin(horaInicio.plusMinutes(30));
        r.setEstado(estado);
        r.setMontoTotal(10000);
        r.setMontoAbono(5000);
        r.setMontoPagado(0);
        r.setExpiraEn(LocalDateTime.now().plusMinutes(30));
        return r;
    }

    // ==========================================
    // CALCULAR ABONO
    // ==========================================

    @Nested
    @DisplayName("Pruebas de calcularAbono")
    class CalcularAbonoTests {

        @Test
        @DisplayName("Debe calcular el abono correctamente según el porcentaje configurado")
        void calcularAbono_Exito() {
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setPorcentajeAbono(50);
            when(configuracionCliente.obtener()).thenReturn(config);

            int abono = reservaService.calcularAbono(10000);

            assertEquals(5000, abono);
            verify(configuracionCliente, times(1)).obtener();
        }
    }

    // ==========================================
    // CREAR RESERVA
    // ==========================================

    @Nested
    @DisplayName("Pruebas de crear")
    class CrearReservaTests {

        @Test
        @DisplayName("Debe crear una reserva individual correctamente cuando no es recurrente")
        void crear_ReservaIndividual_Exito() {
            SesionDTO sesion = crearSesion(1L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            ReservaRequest datos = new ReservaRequest();
            datos.setServicioId(10L);
            datos.setProfesionalId(20L);
            datos.setFecha(LocalDate.now().plusDays(2));
            datos.setHoraInicio(LocalTime.of(10, 0));

            ServicioDTO servicio = crearServicio(10L, "Masaje", 60, 20000, "Spá");
            ProfesionalDTO profesional = crearProfesional(20L, "Carlos");

            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setPorcentajeAbono(50);

            when(catalogoCliente.obtener(10L)).thenReturn(servicio);
            when(personalCliente.obtener(20L)).thenReturn(profesional);
            when(configuracionCliente.obtener()).thenReturn(config);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            ResultadoReserva resultado = reservaService.crear(sesion, datos);

            assertNotNull(resultado);
            assertNotNull(resultado.getReserva());
            assertEquals(1, resultado.getGeneradas().size());
            assertEquals("Juan Perez", resultado.getReserva().getNombreCliente());
            assertEquals(EstadosReserva.PENDIENTE_PAGO, resultado.getReserva().getEstado());
            assertEquals(10000, resultado.getReserva().getMontoAbono());

            verify(disponibilidadService).verificarLibre(20L, 10L, datos.getFecha(), datos.getHoraInicio(), null);
            assertEquals("Spá", resultado.getReserva().getNombreCategoria());
            assertEquals(1, resultado.getReserva().getPrestaciones().size());
        }

        @Test
        @DisplayName("Debe encadenar prestaciones de profesionales distintos en una sola cita")
        void crear_DosPrestaciones_SumaDuracionYEncadenaHorarios() {
            SesionDTO sesion = crearSesion(1L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            PrestacionPedido lifting = new PrestacionPedido();
            lifting.setServicioId(10L);
            lifting.setProfesionalId(20L);
            PrestacionPedido unas = new PrestacionPedido();
            unas.setServicioId(11L);
            unas.setProfesionalId(30L);
            ReservaRequest datos = new ReservaRequest();
            datos.setFecha(LocalDate.now().plusDays(2));
            datos.setHoraInicio(LocalTime.of(10, 0));
            datos.setPrestaciones(List.of(lifting, unas));

            when(catalogoCliente.obtener(10L)).thenReturn(crearServicio(10L, "Lifting", 60, 20000, "Pestañas"));
            when(catalogoCliente.obtener(11L)).thenReturn(crearServicio(11L, "Uñas", 45, 10000, "Manos"));
            when(personalCliente.obtener(20L)).thenReturn(crearProfesional(20L, "Ana"));
            when(personalCliente.obtener(30L)).thenReturn(crearProfesional(30L, "Carla"));
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setPorcentajeAbono(50);
            when(configuracionCliente.obtener()).thenReturn(config);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            ResultadoReserva resultado = reservaService.crear(sesion, datos);

            assertEquals(105, resultado.getReserva().getDuracionMinutos());
            assertEquals(LocalTime.of(11, 45), resultado.getReserva().getHoraFin());
            assertEquals(30000, resultado.getReserva().getMontoTotal());
            assertEquals(15000, resultado.getReserva().getMontoAbono());
            assertEquals("Lifting, Uñas", resultado.getReserva().getNombreServicio());
            assertEquals(20L, resultado.getReserva().getProfesionalId());
            assertEquals(2, resultado.getReserva().getPrestaciones().size());
            assertEquals(LocalTime.of(10, 0), resultado.getReserva().getPrestaciones().get(0).getHoraInicio());
            assertEquals(LocalTime.of(11, 0), resultado.getReserva().getPrestaciones().get(1).getHoraInicio());
            assertEquals(30L, resultado.getReserva().getPrestaciones().get(1).getProfesionalId());
            verify(disponibilidadService).verificarLibre(20L, 10L, datos.getFecha(), LocalTime.of(10, 0), null);
            verify(disponibilidadService).verificarLibre(30L, 11L, datos.getFecha(), LocalTime.of(11, 0), null);
        }

        @Test
        @DisplayName("Debe guardar solo el nombre si el apellido está en blanco y la categoría no tiene nombre")
        void crear_ApellidoEnBlancoYCategoriaSinNombre_Exito() {
            SesionDTO sesion = crearSesion(1L, "Juan", "  ", "juan@test.com", "CLIENTE");
            ReservaRequest datos = new ReservaRequest();
            datos.setServicioId(10L);
            datos.setProfesionalId(20L);
            datos.setFecha(LocalDate.now().plusDays(2));
            datos.setHoraInicio(LocalTime.of(10, 0));

            ServicioDTO servicio = crearServicio(10L, "Masaje", 60, 20000, null);
            CategoriaDTO categoria = new CategoriaDTO();
            servicio.setCategoria(categoria);

            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setPorcentajeAbono(50);

            when(catalogoCliente.obtener(10L)).thenReturn(servicio);
            when(personalCliente.obtener(20L)).thenReturn(crearProfesional(20L, "Carlos"));
            when(configuracionCliente.obtener()).thenReturn(config);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            ResultadoReserva resultado = reservaService.crear(sesion, datos);

            assertEquals("Juan", resultado.getReserva().getNombreCliente());
            assertEquals("", resultado.getReserva().getNombreCategoria());
            verify(reservaRepository).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Debe usar una repetición cuando la serie quincenal no indica cantidad")
        void crear_RecurrenciaQuincenal_RepeticionesNulas_UsaUna() {
            SesionDTO sesion = crearSesion(1L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            ReservaRequest datos = new ReservaRequest();
            datos.setServicioId(10L);
            datos.setProfesionalId(20L);
            datos.setFecha(LocalDate.now().plusDays(2));
            datos.setHoraInicio(LocalTime.of(10, 0));
            datos.setRecurrencia("quincenal");

            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setPorcentajeAbono(10);
            when(catalogoCliente.obtener(10L)).thenReturn(crearServicio(10L, "Corte", 30, 10000, null));
            when(personalCliente.obtener(20L)).thenReturn(crearProfesional(20L, "Ana"));
            when(configuracionCliente.obtener()).thenReturn(config);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            ResultadoReserva resultado = reservaService.crear(sesion, datos);

            assertEquals(1, resultado.getGeneradas().size());
            assertTrue(resultado.getFechasOmitidas().isEmpty());
            assertNotNull(resultado.getReserva().getSerieId());
            verify(reservaRepository, atLeastOnce()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("Debe fallar al crear recurrente si las repeticiones son menores a 1")
        void crear_RecurrenciaQuincenal_RepeticionesInvalidas_LanzaExcepcion() {
            SesionDTO sesion = crearSesion(1L, "Juan", null, "juan@test.com", "CLIENTE");
            ReservaRequest datos = new ReservaRequest();
            datos.setRecurrencia("QUINCENAL");
            datos.setRepeticiones(0);
            datos.setServicioId(10L);
            datos.setProfesionalId(20L);
            datos.setFecha(LocalDate.now().plusDays(1));
            datos.setHoraInicio(LocalTime.of(10, 0));

            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setPorcentajeAbono(10);
            when(catalogoCliente.obtener(anyLong())).thenReturn(crearServicio(10L, "S", 30, 100, null));
            when(personalCliente.obtener(anyLong())).thenReturn(crearProfesional(20L, "P"));
            when(configuracionCliente.obtener()).thenReturn(config);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.crear(sesion, datos));

            assertEquals("Las repeticiones deben ser al menos 1", ex.getMessage());
        }

        @Test
        @DisplayName("Debe crear serie de reservas quincenales y omitir las fechas no disponibles")
        void crear_RecurrenciaQuincenal_OmitirFechasConOcupacion() {
            SesionDTO sesion = crearSesion(1L, "Juan", "Gomez", "juan@test.com", "CLIENTE");
            LocalDate fechaInicial = LocalDate.now().plusDays(1);
            ReservaRequest datos = new ReservaRequest();
            datos.setServicioId(10L);
            datos.setProfesionalId(20L);
            datos.setFecha(fechaInicial);
            datos.setHoraInicio(LocalTime.of(10, 0));
            datos.setRecurrencia("QUINCENAL");
            datos.setRepeticiones(3);

            ServicioDTO servicio = crearServicio(10L, "Corte", 30, 10000, null);
            ProfesionalDTO profesional = crearProfesional(20L, "Ana");
            ConfiguracionDTO config = new ConfiguracionDTO();
            config.setPorcentajeAbono(20);

            when(catalogoCliente.obtener(10L)).thenReturn(servicio);
            when(personalCliente.obtener(20L)).thenReturn(profesional);
            when(configuracionCliente.obtener()).thenReturn(config);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            // Simular conflicto de disponibilidad en la segunda repetición (+14 días)
            LocalDate fechaSegundaRepeticion = fechaInicial.plusDays(14);
            doNothing().when(disponibilidadService).verificarLibre(eq(20L), eq(10L), eq(fechaInicial), any(), any());
            doThrow(new ReglaNegocioException("Horario ocupado"))
                    .when(disponibilidadService).verificarLibre(eq(20L), eq(10L), eq(fechaSegundaRepeticion), any(), any());
            doNothing().when(disponibilidadService).verificarLibre(eq(20L), eq(10L), eq(fechaInicial.plusDays(28)), any(), any());

            ResultadoReserva resultado = reservaService.crear(sesion, datos);

            assertEquals(2, resultado.getGeneradas().size());

            assertNotNull(resultado.getReserva().getSerieId());
        }
    }

    // ==========================================
    // BÚSQUEDA Y VALIDACIONES DE ACCESO
    // ==========================================

    @Nested
    @DisplayName("Pruebas de búsqueda y permisos")
    class BusquedaYPermisosTests {

        @Test
        @DisplayName("buscar() lanza RecursoNoEncontradoException cuando la reserva no existe")
        void buscar_NoExiste_LanzaExcepcion() {
            when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

            RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                    () -> reservaService.buscar(99L));

            assertEquals("No existe la reserva", ex.getMessage());
            verify(reservaRepository).findById(99L);
        }

        @Test
        @DisplayName("buscar() retorna la reserva cuando existe")
        void buscar_Existe_RetornaReserva() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.of(10, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            Reserva resultado = reservaService.buscar(1L);

            assertEquals(1L, resultado.getId());
            verify(reservaRepository).findById(1L);
        }

        @Test
        @DisplayName("mias() retorna las reservas del cliente")
        void mias_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.of(10, 0));
            when(reservaRepository.findByIdClienteOrderByFechaDescHoraInicioDesc(100L)).thenReturn(List.of(reserva));

            List<Reserva> resultado = reservaService.mias(100L);

            assertEquals(1, resultado.size());
            verify(reservaRepository).findByIdClienteOrderByFechaDescHoraInicioDesc(100L);
        }

        @Test
        @DisplayName("rango() consulta las reservas entre dos fechas")
        void rango_Exito() {
            LocalDate desde = LocalDate.of(2026, 5, 1);
            LocalDate hasta = LocalDate.of(2026, 5, 31);
            when(reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(desde, hasta))
                    .thenReturn(Collections.emptyList());

            List<Reserva> resultado = reservaService.rango(desde, hasta);

            assertTrue(resultado.isEmpty());
            verify(reservaRepository).findByFechaBetweenOrderByFechaAscHoraInicioAsc(desde, hasta);
        }

        @Test
        @DisplayName("historial() consulta las citas atendidas del profesional")
        void historial_Exito() {
            LocalDate desde = LocalDate.of(2026, 5, 1);
            LocalDate hasta = LocalDate.of(2026, 5, 31);
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, desde, LocalTime.of(10, 0));
            when(reservaRepository.findHistorial(eq(20L), eq(desde), eq(hasta), anyList())).thenReturn(List.of(reserva));

            List<Reserva> resultado = reservaService.historial(20L, desde, hasta);

            assertEquals(1, resultado.size());
            assertEquals(LocalTime.of(10, 0), resultado.get(0).getHoraInicio());
            verify(reservaRepository).findHistorial(eq(20L), eq(desde), eq(hasta),
                    eq(List.of(EstadosReserva.CONFIRMADA, EstadosReserva.PAGADA_COMPLETA, EstadosReserva.EN_PROCESO)));
        }

        @Test
        @DisplayName("tieneReservas() indica si el profesional tiene citas")
        void tieneReservas_Exito() {
            when(reservaRepository.existsByProfesionalId(20L)).thenReturn(true);

            assertTrue(reservaService.tieneReservas(20L));
            verify(reservaRepository).existsByProfesionalId(20L);
        }

        @Test
        @DisplayName("exigirDuenoOAdmin no lanza excepción si el usuario es ADMIN")
        void exigirDuenoOAdmin_Admin_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            SesionDTO admin = crearSesion(200L, "Admin", "User", "admin@test.com", "ADMIN");

            assertDoesNotThrow(() -> reservaService.exigirDuenoOAdmin(reserva, admin));
        }

        @Test
        @DisplayName("exigirDuenoOAdmin no lanza excepción si el usuario es el dueño de la reserva")
        void exigirDuenoOAdmin_Dueno_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            SesionDTO dueno = crearSesion(100L, "Cliente", "User", "cliente@test.com", "CLIENTE");

            assertDoesNotThrow(() -> reservaService.exigirDuenoOAdmin(reserva, dueno));
        }

        @Test
        @DisplayName("exigirDuenoOAdmin lanza AccesoDenegadoException si el usuario no es dueño ni ADMIN")
        void exigirDuenoOAdmin_OtroUsuario_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            SesionDTO intruso = crearSesion(999L, "Intruso", "User", "intruso@test.com", "CLIENTE");

            AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                    () -> reservaService.exigirDuenoOAdmin(reserva, intruso));

            assertEquals("No puede operar una reserva de otro cliente", ex.getMessage());
        }
    }

    // ==========================================
    // AGENDA Y FILTROS
    // ==========================================

    @Nested
    @DisplayName("Pruebas de filtrado de agenda")
    class AgendaTests {

        @Test
        @DisplayName("agenda() calcula rango 'dia' y aplica filtros correctamente")
        void agenda_VistaDia_FiltraLiberadas() {
            LocalDate hoy = LocalDate.of(2026, 5, 10);
            Reserva r1 = crearReservaBase(1L, 1L, EstadosReserva.CONFIRMADA, hoy, LocalTime.of(10, 0));
            Reserva r2 = crearReservaBase(2L, 2L, EstadosReserva.LIBERADA, hoy, LocalTime.of(11, 0));

            when(reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(hoy, hoy))
                    .thenReturn(List.of(r1, r2));

            List<Reserva> resultado = reservaService.agenda("dia", hoy, null, null);

            assertEquals(1, resultado.size());
            assertEquals(1L, resultado.get(0).getId());
        }

        @Test
        @DisplayName("agenda() calcula rango 'semana' correctamente")
        void agenda_VistaSemana_Exito() {
            LocalDate lunes = LocalDate.of(2026, 5, 11); // Lunes
            LocalDate domingo = LocalDate.of(2026, 5, 17); // Domingo

            Reserva reserva = crearReservaBase(1L, 1L, EstadosReserva.CONFIRMADA, lunes, LocalTime.of(10, 0));
            when(reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(lunes, domingo))
                    .thenReturn(List.of(reserva));

            List<Reserva> resultado = reservaService.agenda("semana", lunes, null, "  ");

            assertEquals(1, resultado.size());
            verify(reservaRepository).findByFechaBetweenOrderByFechaAscHoraInicioAsc(lunes, domingo);
        }

        @Test
        @DisplayName("agenda() calcula rango 'mes' correctamente")
        void agenda_VistaMes_Exito() {
            LocalDate fecha = LocalDate.of(2026, 2, 15);
            LocalDate inicioMes = LocalDate.of(2026, 2, 1);
            LocalDate finMes = LocalDate.of(2026, 2, 28);

            when(reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(inicioMes, finMes))
                    .thenReturn(Collections.emptyList());

            List<Reserva> resultado = reservaService.agenda("mes", fecha, null, null);

            assertNotNull(resultado);
            verify(reservaRepository).findByFechaBetweenOrderByFechaAscHoraInicioAsc(inicioMes, finMes);
        }

        @Test
        @DisplayName("agenda() lanza ReglaNegocioException si la vista no es válida")
        void agenda_VistaInvalida_LanzaExcepcion() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.agenda("año", LocalDate.now(), null, null));

            assertEquals("La vista debe ser dia, semana o mes", ex.getMessage());
        }

        @Test
        @DisplayName("agenda() filtra por profesional y por estado")
        void agenda_FiltraProfesionalYEstado() {
            LocalDate hoy = LocalDate.of(2026, 5, 10);
            Reserva coincidente = crearReservaBase(1L, 1L, EstadosReserva.CONFIRMADA, hoy, LocalTime.of(10, 0));
            Reserva otroProfesional = crearReservaBase(2L, 1L, EstadosReserva.CONFIRMADA, hoy, LocalTime.of(11, 0));
            otroProfesional.setProfesionalId(30L);
            Reserva otroEstado = crearReservaBase(3L, 1L, EstadosReserva.PENDIENTE_PAGO, hoy, LocalTime.of(12, 0));

            when(reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(hoy, hoy))
                    .thenReturn(List.of(coincidente, otroProfesional, otroEstado));

            List<Reserva> resultado = reservaService.agenda("dia", hoy, 20L, EstadosReserva.CONFIRMADA);

            assertEquals(1, resultado.size());
            assertEquals(1L, resultado.get(0).getId());
        }

        @Test
        @DisplayName("agenda() muestra solo el tramo del profesional dentro de una cita con varias prestaciones")
        void agenda_FiltraTramoDelProfesional() {
            LocalDate hoy = LocalDate.of(2026, 5, 10);
            Reserva visita = crearReservaBase(1L, 1L, EstadosReserva.CONFIRMADA, hoy, LocalTime.of(10, 0));
            visita.setHoraFin(LocalTime.of(11, 45));
            visita.setDuracionMinutos(105);
            visita.setNombreServicio("Lifting, Uñas");
            com.sakura.agenda.model.Prestacion lifting = new com.sakura.agenda.model.Prestacion();
            lifting.setOrden(1);
            lifting.setServicioId(10L);
            lifting.setNombreServicio("Lifting");
            lifting.setNombreCategoria("Pestañas");
            lifting.setDuracionMinutos(60);
            lifting.setPrecio(20000);
            lifting.setProfesionalId(20L);
            lifting.setNombreProfesional("Ana");
            lifting.setHoraInicio(LocalTime.of(10, 0));
            lifting.setHoraFin(LocalTime.of(11, 0));
            com.sakura.agenda.model.Prestacion unas = new com.sakura.agenda.model.Prestacion();
            unas.setOrden(2);
            unas.setServicioId(11L);
            unas.setNombreServicio("Uñas");
            unas.setNombreCategoria("Manos");
            unas.setDuracionMinutos(45);
            unas.setPrecio(10000);
            unas.setProfesionalId(30L);
            unas.setNombreProfesional("Carla");
            unas.setHoraInicio(LocalTime.of(11, 0));
            unas.setHoraFin(LocalTime.of(11, 45));
            visita.agregarPrestacion(lifting);
            visita.agregarPrestacion(unas);
            when(reservaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(hoy, hoy)).thenReturn(List.of(visita));

            List<Reserva> deCarla = reservaService.agenda("dia", hoy, 30L, null);

            assertEquals(1, deCarla.size());
            assertEquals("Uñas", deCarla.get(0).getNombreServicio());
            assertEquals(LocalTime.of(11, 0), deCarla.get(0).getHoraInicio());
            assertEquals(LocalTime.of(11, 45), deCarla.get(0).getHoraFin());
            assertEquals(30L, deCarla.get(0).getProfesionalId());
        }
    }

    // ==========================================
    // CANCELAR Y MOVER (ADMIN)
    // ==========================================

    @Nested
    @DisplayName("Pruebas de Cancelar y Mover (Admin)")
    class CancelarMoverAdminTests {

        @Test
        @DisplayName("cancelar() cambia el estado a CANCELADA y notifica al cliente")
        void cancelar_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            Reserva resultado = reservaService.cancelar(1L);

            assertEquals(EstadosReserva.CANCELADA, resultado.getEstado());
            verify(reservaRepository).save(reserva);
            verify(notificacionService).avisar(eq(reserva), eq("CANCELACION"), contains("Su cita fue cancelada por la administración."));
        }

        @Test
        @DisplayName("cancelar() lanza ReglaNegocioException si la reserva ya está cancelada o liberada")
        void cancelar_YaCancelada_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CANCELADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.cancelar(1L));

            assertEquals("La reserva ya no se puede cancelar", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("cancelar() lanza ReglaNegocioException si la reserva está liberada")
        void cancelar_Liberada_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.LIBERADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.cancelar(1L));

            assertEquals("La reserva ya no se puede cancelar", ex.getMessage());
            verify(notificacionService, never()).avisar(any(), any(), any());
        }

        @Test
        @DisplayName("mover() actualiza fecha/hora y notifica si hay disponibilidad")
        void mover_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.of(10, 0));
            MoverReservaRequest datos = new MoverReservaRequest();
            LocalDate nuevaFecha = LocalDate.now().plusDays(5);
            LocalTime nuevaHora = LocalTime.of(14, 0);
            datos.setFecha(nuevaFecha);
            datos.setHoraInicio(nuevaHora);

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            Reserva resultado = reservaService.mover(1L, datos);

            assertEquals(nuevaFecha, resultado.getFecha());
            assertEquals(nuevaHora, resultado.getHoraInicio());
            assertEquals(nuevaHora.plusMinutes(reserva.getDuracionMinutos()), resultado.getHoraFin());

            verify(disponibilidadService).verificarLibre(20L, 10L, nuevaFecha, nuevaHora, 1L);
            verify(notificacionService).avisar(eq(reserva), eq("MODIFICACION"), contains("Su cita fue modificada por la administración."));
        }

        @Test
        @DisplayName("mover() desplaza cada prestación conservando su duración")
        void mover_ConPrestaciones_ReencadenaTramos() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.of(10, 0));
            reserva.setDuracionMinutos(105);
            reserva.setHoraFin(LocalTime.of(11, 45));
            com.sakura.agenda.model.Prestacion lifting = new com.sakura.agenda.model.Prestacion();
            lifting.setOrden(1);
            lifting.setServicioId(10L);
            lifting.setDuracionMinutos(60);
            lifting.setProfesionalId(20L);
            lifting.setHoraInicio(LocalTime.of(10, 0));
            lifting.setHoraFin(LocalTime.of(11, 0));
            lifting.setPrecio(20000);
            lifting.setNombreServicio("Lifting");
            lifting.setNombreCategoria("Pestañas");
            lifting.setNombreProfesional("Ana");
            com.sakura.agenda.model.Prestacion unas = new com.sakura.agenda.model.Prestacion();
            unas.setOrden(2);
            unas.setServicioId(11L);
            unas.setDuracionMinutos(45);
            unas.setProfesionalId(30L);
            unas.setHoraInicio(LocalTime.of(11, 0));
            unas.setHoraFin(LocalTime.of(11, 45));
            unas.setPrecio(10000);
            unas.setNombreServicio("Uñas");
            unas.setNombreCategoria("Manos");
            unas.setNombreProfesional("Carla");
            reserva.agregarPrestacion(lifting);
            reserva.agregarPrestacion(unas);
            MoverReservaRequest datos = new MoverReservaRequest();
            datos.setFecha(LocalDate.now().plusDays(3));
            datos.setHoraInicio(LocalTime.of(15, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            Reserva resultado = reservaService.mover(1L, datos);

            assertEquals(LocalTime.of(15, 0), resultado.getHoraInicio());
            assertEquals(LocalTime.of(16, 45), resultado.getHoraFin());
            assertEquals(LocalTime.of(16, 0), resultado.getPrestaciones().get(1).getHoraInicio());
            verify(disponibilidadService).verificarLibre(20L, 10L, datos.getFecha(), LocalTime.of(15, 0), 1L);
            verify(disponibilidadService).verificarLibre(30L, 11L, datos.getFecha(), LocalTime.of(16, 0), 1L);
        }

        @Test
        @DisplayName("mover() lanza ReglaNegocioException si la reserva está liberada")
        void mover_Liberada_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.LIBERADA, LocalDate.now(), LocalTime.of(10, 0));
            MoverReservaRequest datos = new MoverReservaRequest();
            datos.setFecha(LocalDate.now().plusDays(1));
            datos.setHoraInicio(LocalTime.of(11, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.mover(1L, datos));

            assertEquals("La reserva ya no se puede modificar", ex.getMessage());
            verifyNoInteractions(disponibilidadService);
        }

        @Test
        @DisplayName("mover() lanza ReglaNegocioException si la reserva está cancelada")
        void mover_Cancelada_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CANCELADA, LocalDate.now(), LocalTime.of(10, 0));
            MoverReservaRequest datos = new MoverReservaRequest();
            datos.setFecha(LocalDate.now().plusDays(1));
            datos.setHoraInicio(LocalTime.of(11, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> reservaService.mover(1L, datos));

            assertEquals("La reserva ya no se puede modificar", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }
    }

    // ==========================================
    // CANCELAR Y MOVER (CLIENTE)
    // ==========================================

    @Nested
    @DisplayName("Pruebas de Cancelar y Mover (Cliente)")
    class CancelarMoverClienteTests {

        @Test
        @DisplayName("cancelarCliente() permite al cliente cancelar su reserva futura")
        void cancelarCliente_Exito() {
            SesionDTO cliente = crearSesion(100L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            LocalDate fechaFutura = LocalDate.now().plusDays(3);
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, fechaFutura, LocalTime.of(10, 0));

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            Reserva resultado = reservaService.cancelarCliente(1L, cliente, "Inconveniente personal");

            assertEquals(EstadosReserva.CANCELADA, resultado.getEstado());
            assertEquals("Inconveniente personal", resultado.getMotivoCancelacion());
            verify(notificacionService).avisar(eq(reserva), eq("CANCELACION"), contains("Motivo: Inconveniente personal"));
        }

        @Test
        @DisplayName("cancelarCliente() lanza AccesoDenegadoException si pertenece a otro cliente")
        void cancelarCliente_OtroCliente_LanzaExcepcion() {
            SesionDTO cliente = crearSesion(999L, "Otro", "Usuario", "otro@test.com", "CLIENTE");
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now().plusDays(2), LocalTime.of(10, 0));

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            assertThrows(AccesoDenegadoException.class,
                    () -> reservaService.cancelarCliente(1L, cliente, "Motivo"));
        }

        @Test
        @DisplayName("cancelarCliente() lanza ReglaNegocioException si la reserva no es futura")
        void cancelarCliente_ReservaPasada_LanzaExcepcion() {
            SesionDTO cliente = crearSesion(100L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            LocalDate fechaPasada = LocalDate.now().minusDays(1);
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, fechaPasada, LocalTime.of(10, 0));

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.cancelarCliente(1L, cliente, "Motivo"));

            assertEquals("Solo puede cancelar una cita futura", ex.getMessage());
        }

        @Test
        @DisplayName("cancelarCliente() lanza ReglaNegocioException si la reserva ya está cancelada")
        void cancelarCliente_YaCancelada_LanzaExcepcion() {
            SesionDTO cliente = crearSesion(100L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CANCELADA, LocalDate.now().plusDays(2), LocalTime.of(10, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.cancelarCliente(1L, cliente, "Motivo"));

            assertEquals("La reserva ya no se puede cancelar", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("cancelarCliente() lanza ReglaNegocioException si la reserva está liberada")
        void cancelarCliente_Liberada_LanzaExcepcion() {
            SesionDTO cliente = crearSesion(100L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.LIBERADA, LocalDate.now().plusDays(2), LocalTime.of(10, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.cancelarCliente(1L, cliente, "Motivo"));

            assertEquals("La reserva ya no se puede cancelar", ex.getMessage());
        }

        @Test
        @DisplayName("moverCliente() modifica la cita futura exitosamente")
        void moverCliente_Exito() {
            SesionDTO cliente = crearSesion(100L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            LocalDate fechaFutura = LocalDate.now().plusDays(2);
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, fechaFutura, LocalTime.of(10, 0));

            MoverReservaRequest datos = new MoverReservaRequest();
            datos.setFecha(fechaFutura.plusDays(1));
            datos.setHoraInicio(LocalTime.of(15, 0));

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            Reserva resultado = reservaService.moverCliente(1L, cliente, datos);

            assertNotNull(resultado);
            assertEquals(datos.getFecha(), resultado.getFecha());
            assertEquals(datos.getHoraInicio(), resultado.getHoraInicio());
            verify(notificacionService).avisar(eq(reserva), eq("MODIFICACION"), any());
        }

        @Test
        @DisplayName("moverCliente() lanza AccesoDenegadoException si la cita es de otro cliente")
        void moverCliente_OtroCliente_LanzaExcepcion() {
            SesionDTO cliente = crearSesion(999L, "Otro", "Usuario", "otro@test.com", "CLIENTE");
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now().plusDays(2), LocalTime.of(10, 0));
            MoverReservaRequest datos = new MoverReservaRequest();
            datos.setFecha(LocalDate.now().plusDays(3));
            datos.setHoraInicio(LocalTime.of(11, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                    () -> reservaService.moverCliente(1L, cliente, datos));

            assertEquals("No puede modificar una reserva de otro cliente", ex.getMessage());
        }

        @Test
        @DisplayName("moverCliente() lanza ReglaNegocioException si la cita no es futura")
        void moverCliente_ReservaPasada_LanzaExcepcion() {
            SesionDTO cliente = crearSesion(100L, "Juan", "Perez", "juan@test.com", "CLIENTE");
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now().minusDays(1), LocalTime.of(10, 0));
            MoverReservaRequest datos = new MoverReservaRequest();
            datos.setFecha(LocalDate.now().plusDays(3));
            datos.setHoraInicio(LocalTime.of(11, 0));
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.moverCliente(1L, cliente, datos));

            assertEquals("Solo puede modificar una cita futura", ex.getMessage());
        }
    }

    // ==========================================
    // ASISTENCIA E INASISTENCIA
    // ==========================================

    @Nested
    @DisplayName("Pruebas de Asistencia e Inasistencia")
    class AsistenciaTests {

        @Test
        @DisplayName("marcarAsistencia() cambia el estado a EN_PROCESO para citas confirmadas")
        void marcarAsistencia_Confirmada_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            Reserva resultado = reservaService.marcarAsistencia(1L);

            assertEquals(EstadosReserva.EN_PROCESO, resultado.getEstado());
            verify(reservaRepository).save(reserva);
        }

        @Test
        @DisplayName("marcarAsistencia() también acepta una cita pagada por completo")
        void marcarAsistencia_PagadaCompleta_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PAGADA_COMPLETA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            Reserva resultado = reservaService.marcarAsistencia(1L);

            assertEquals(EstadosReserva.EN_PROCESO, resultado.getEstado());
            verify(reservaRepository).save(reserva);
        }

        @Test
        @DisplayName("marcarAsistencia() lanza ReglaNegocioException si la cita no está confirmada ni pagada completa")
        void marcarAsistencia_EstadoInvalido_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.marcarAsistencia(1L));

            assertEquals("Solo se puede marcar asistencia de una cita confirmada", ex.getMessage());
        }

        @Test
        @DisplayName("marcarInasistencia() marca estado INASISTENCIA y guarda el motivo")
        void marcarInasistencia_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            Reserva resultado = reservaService.marcarInasistencia(1L, "No llegó");

            assertEquals(EstadosReserva.INASISTENCIA, resultado.getEstado());
            assertEquals("No llegó", resultado.getMotivoCancelacion());
            verify(reservaRepository).save(reserva);
        }

        @Test
        @DisplayName("marcarInasistencia() lanza ReglaNegocioException si la reserva está cancelada")
        void marcarInasistencia_Cancelada_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CANCELADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.marcarInasistencia(1L, "No llegó"));

            assertEquals("La reserva ya no admite inasistencia", ex.getMessage());
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("marcarInasistencia() lanza ReglaNegocioException si la reserva está liberada")
        void marcarInasistencia_Liberada_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.LIBERADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.marcarInasistencia(1L, "No llegó"));

            assertEquals("La reserva ya no admite inasistencia", ex.getMessage());
        }
    }

    // ==========================================
    // PAGOS Y CONFIRMACIONES
    // ==========================================

    @Nested
    @DisplayName("Pruebas de Procesamiento de Pagos")
    class PagosTests {

        @Test
        @DisplayName("pagarAbono() confirma la reserva cuando el pago es aprobado y vigente")
        void pagarAbono_Aprobado_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            reserva.setExpiraEn(LocalDateTime.now().plusMinutes(10));
            reserva.setMontoAbono(5000);

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            Reserva resultado = reservaService.pagarAbono(1L, true, "AUTH-123");

            assertEquals(EstadosReserva.CONFIRMADA, resultado.getEstado());
            assertEquals(5000, resultado.getMontoPagado());
            verify(pagoCliente).registrar(reserva, "ABONO", 5000, true, "AUTH-123");
            verify(notificacionService).confirmar(reserva);
        }

        @Test
        @DisplayName("pagarAbono() libera la reserva si la fecha de expiración se ha vencido")
        void pagarAbono_Expirado_LanzaExcepcionYLibera() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            reserva.setExpiraEn(LocalDateTime.now().minusMinutes(5));

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.pagarAbono(1L, true, "AUTH-123"));

            assertEquals("El tiempo para pagar el abono venció", ex.getMessage());
            assertEquals(EstadosReserva.LIBERADA, reserva.getEstado());
            verify(reservaRepository).save(reserva);
            verifyNoInteractions(pagoCliente);
        }

        @Test
        @DisplayName("pagarAbono() lanza excepción si el pago no fue aprobado")
        void pagarAbono_Rechazado_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            reserva.setExpiraEn(LocalDateTime.now().plusMinutes(10));

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.pagarAbono(1L, false, "AUTH-123"));

            assertTrue(ex.getMessage().contains("El pago fue rechazado"));
            verify(pagoCliente).registrar(reserva, "ABONO", reserva.getMontoAbono(), false, "AUTH-123");
            verify(notificacionService, never()).confirmar(any());
        }

        @Test
        @DisplayName("pagarAbono() lanza ReglaNegocioException si la reserva no está pendiente de pago")
        void pagarAbono_NoPendiente_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.pagarAbono(1L, true, "AUTH-123"));

            assertEquals("La reserva no está pendiente de pago", ex.getMessage());
            verifyNoInteractions(pagoCliente);
        }

        @Test
        @DisplayName("pagarAbono() libera la reserva si no tiene fecha de expiración")
        void pagarAbono_SinExpiracion_LanzaExcepcionYLibera() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            reserva.setExpiraEn(null);
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.pagarAbono(1L, true, "AUTH-123"));

            assertEquals("El tiempo para pagar el abono venció", ex.getMessage());
            assertEquals(EstadosReserva.LIBERADA, reserva.getEstado());
            verify(reservaRepository).save(reserva);
        }

        @Test
        @DisplayName("registrarSaldo() registra el pago restante y marca PAGADA_COMPLETA")
        void registrarSaldo_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.now());
            reserva.setMontoTotal(10000);
            reserva.setMontoPagado(4000); // Saldo pendiente = 6000

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            Reserva resultado = reservaService.registrarSaldo(1L, "AUTH-SALDO");

            assertEquals(EstadosReserva.PAGADA_COMPLETA, resultado.getEstado());
            assertEquals(10000, resultado.getMontoPagado());
            verify(pagoCliente).registrar(reserva, "SALDO", 6000, true, "AUTH-SALDO");
        }

        @Test
        @DisplayName("registrarSaldo() lanza ReglaNegocioException si la cita no está confirmada")
        void registrarSaldo_NoConfirmada_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.registrarSaldo(1L, "AUTH-SALDO"));

            assertEquals("La cita debe estar confirmada para registrar el saldo", ex.getMessage());
            verifyNoInteractions(pagoCliente);
        }

        @Test
        @DisplayName("registrarManual() aprueba el abono directamente y confirma la reserva")
        void registrarManual_Exito() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.PENDIENTE_PAGO, LocalDate.now(), LocalTime.now());
            reserva.setMontoAbono(3000);

            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

            Reserva resultado = reservaService.registrarManual(1L, "MANUAL-AUTH");

            assertEquals(EstadosReserva.CONFIRMADA, resultado.getEstado());
            assertEquals(3000, resultado.getMontoPagado());
            verify(pagoCliente).registrar(reserva, "ABONO", 3000, true, "MANUAL-AUTH");
            verify(notificacionService).confirmar(reserva);
        }

        @Test
        @DisplayName("registrarManual() lanza ReglaNegocioException si la reserva no está pendiente de pago")
        void registrarManual_NoPendiente_LanzaExcepcion() {
            Reserva reserva = crearReservaBase(1L, 100L, EstadosReserva.CONFIRMADA, LocalDate.now(), LocalTime.now());
            when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> reservaService.registrarManual(1L, "MANUAL-AUTH"));

            assertEquals("La reserva no está pendiente de pago", ex.getMessage());
            verifyNoInteractions(pagoCliente);
        }
    }
}