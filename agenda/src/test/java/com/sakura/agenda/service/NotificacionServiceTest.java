package com.sakura.agenda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sakura.agenda.model.EstadosReserva;
import com.sakura.agenda.model.Notificacion;
import com.sakura.agenda.model.Reserva;
import com.sakura.agenda.repository.NotificacionRepository;
import com.sakura.agenda.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private NotificacionService notificacionService;

    private Reserva reservaPrueba;

    @BeforeEach
    void setUp() {
        reservaPrueba = new Reserva();
        reservaPrueba.setId(1L);
        reservaPrueba.setCorreoCliente("cliente@sakura.com");
        reservaPrueba.setNombreCliente("Ana Gómez");
        reservaPrueba.setNombreServicio("Corte de Cabello");
        reservaPrueba.setNombreProfesional("María Stylist");
        reservaPrueba.setFecha(LocalDate.now().plusDays(1));
        reservaPrueba.setHoraInicio(LocalTime.of(15, 0));
    }

    @Nested
    @DisplayName("Pruebas de Avisos y Confirmación")
    class AvisosYConfirmacionTests {

        @Test
        @DisplayName("Debe crear y guardar una notificación correctamente en avisar()")
        void debeAvisarYGuardarNotificacion() {
            // Act
            notificacionService.avisar(reservaPrueba, "CONFIRMACION", "Mensaje de prueba");

            // Assert
            ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
            verify(notificacionRepository, times(1)).save(captor.capture());

            Notificacion guardada = captor.getValue();
            assertEquals(1L, guardada.getReservaId());
            assertEquals("CONFIRMACION", guardada.getTipo());
            assertEquals("Mensaje de prueba", guardada.getMensaje());
        }

        @Test
        @DisplayName("Debe construir el mensaje correcto al confirmar una reserva")
        void debeConfirmarReserva() {
            // Act
            notificacionService.confirmar(reservaPrueba);

            // Assert
            ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
            verify(notificacionRepository, times(1)).save(captor.capture());

            Notificacion guardada = captor.getValue();
            assertEquals("CONFIRMACION", guardada.getTipo());
            String mensajeEsperado = "Confirmación de su cita en Sakura Salón. Ana Gómez, Corte de Cabello con María Stylist el " 
                    + reservaPrueba.getFecha() + " a las 15:00.";
            assertEquals(mensajeEsperado, guardada.getMensaje());
        }
    }

    @Nested
    @DisplayName("Pruebas de Liberación de Reservas Vencidas")
    class LiberarVencidasTests {

        @Test
        @DisplayName("Debe cambiar estado a LIBERADA si la reserva PENDIENTE_PAGO ha expirado")
        void debeLiberarReservaSiEstaExpirada() {
            // Arrange
            reservaPrueba.setEstado(EstadosReserva.PENDIENTE_PAGO);
            reservaPrueba.setExpiraEn(LocalDateTime.now().minusMinutes(5));

            when(reservaRepository.findByEstado(EstadosReserva.PENDIENTE_PAGO))
                    .thenReturn(List.of(reservaPrueba));

            // Act
            notificacionService.liberarVencidas();

            // Assert
            assertEquals(EstadosReserva.LIBERADA, reservaPrueba.getEstado());
            verify(reservaRepository, times(1)).save(reservaPrueba);
        }

        @Test
        @DisplayName("No debe liberar la reserva si expiraEn no ha vencido aún")
        void noDebeLiberarReservaSiNoHaExpirado() {
            // Arrange
            reservaPrueba.setEstado(EstadosReserva.PENDIENTE_PAGO);
            reservaPrueba.setExpiraEn(LocalDateTime.now().plusMinutes(10));

            when(reservaRepository.findByEstado(EstadosReserva.PENDIENTE_PAGO))
                    .thenReturn(List.of(reservaPrueba));

            // Act
            notificacionService.liberarVencidas();

            // Assert
            assertEquals(EstadosReserva.PENDIENTE_PAGO, reservaPrueba.getEstado());
            verify(reservaRepository, never()).save(any(Reserva.class));
        }

        @Test
        @DisplayName("No debe liberar si expiraEn es nulo")
        void noDebeLiberarReservaSiExpiraEnEsNull() {
            // Arrange
            reservaPrueba.setEstado(EstadosReserva.PENDIENTE_PAGO);
            reservaPrueba.setExpiraEn(null);

            when(reservaRepository.findByEstado(EstadosReserva.PENDIENTE_PAGO))
                    .thenReturn(List.of(reservaPrueba));

            // Act
            notificacionService.liberarVencidas();

            // Assert
            assertEquals(EstadosReserva.PENDIENTE_PAGO, reservaPrueba.getEstado());
            verify(reservaRepository, never()).save(any(Reserva.class));
        }
    }

    @Nested
    @DisplayName("Pruebas de Recordatorios")
    class RecordatoriosTests {

        @Test
        @DisplayName("Debe enviar recordatorio si está en la ventana de 23-25 horas y no se ha notificado antes")
        void debeEnviarRecordatorioDentroDeVentana() {
            // Arrange: Fecha/hora ajustada exactamente a 24 horas a futuro
            LocalDateTime dentroDe24hs = LocalDateTime.now().plusHours(24);
            reservaPrueba.setFecha(dentroDe24hs.toLocalDate());
            reservaPrueba.setHoraInicio(dentroDe24hs.toLocalTime());

            when(reservaRepository.findByEstado(EstadosReserva.CONFIRMADA))
                    .thenReturn(List.of(reservaPrueba));
            when(reservaRepository.findByEstado(EstadosReserva.PAGADA_COMPLETA))
                    .thenReturn(Collections.emptyList());
            when(notificacionRepository.existsByReservaIdAndTipo(1L, "RECORDATORIO"))
                    .thenReturn(false);

            // Act
            notificacionService.enviarRecordatorios();

            // Assert
            verify(notificacionRepository, times(1)).save(any(Notificacion.class));
        }

        @Test
        @DisplayName("No debe enviar recordatorio si la reserva está fuera del rango (ej. faltan 10 horas)")
        void noDebeEnviarRecordatorioFueraDeRango() {
            // Arrange
            LocalDateTime dentroDe10hs = LocalDateTime.now().plusHours(10);
            reservaPrueba.setFecha(dentroDe10hs.toLocalDate());
            reservaPrueba.setHoraInicio(dentroDe10hs.toLocalTime());

            when(reservaRepository.findByEstado(EstadosReserva.CONFIRMADA))
                    .thenReturn(List.of(reservaPrueba));
            when(reservaRepository.findByEstado(EstadosReserva.PAGADA_COMPLETA))
                    .thenReturn(Collections.emptyList());

            // Act
            notificacionService.enviarRecordatorios();

            // Assert
            verify(notificacionRepository, never()).existsByReservaIdAndTipo(any(), any());
            verify(notificacionRepository, never()).save(any(Notificacion.class));
        }

        @Test
        @DisplayName("No debe enviar recordatorio si la cita es posterior a la ventana de 25 horas")
        void noDebeEnviarRecordatorioSiEsDemasiadoLejana() {
            LocalDateTime dentroDe30hs = LocalDateTime.now().plusHours(30);
            reservaPrueba.setFecha(dentroDe30hs.toLocalDate());
            reservaPrueba.setHoraInicio(dentroDe30hs.toLocalTime());

            when(reservaRepository.findByEstado(EstadosReserva.CONFIRMADA)).thenReturn(List.of(reservaPrueba));
            when(reservaRepository.findByEstado(EstadosReserva.PAGADA_COMPLETA)).thenReturn(Collections.emptyList());

            notificacionService.enviarRecordatorios();

            verify(notificacionRepository, never()).save(any(Notificacion.class));
        }

        @Test
        @DisplayName("No debe reenviar recordatorio si ya existe una notificación previa de tipo RECORDATORIO")
        void noDebeEnviarRecordatorioSiYaFueEnviado() {
            // Arrange
            LocalDateTime dentroDe24hs = LocalDateTime.now().plusHours(24);
            reservaPrueba.setFecha(dentroDe24hs.toLocalDate());
            reservaPrueba.setHoraInicio(dentroDe24hs.toLocalTime());

            when(reservaRepository.findByEstado(EstadosReserva.CONFIRMADA))
                    .thenReturn(List.of(reservaPrueba));
            when(reservaRepository.findByEstado(EstadosReserva.PAGADA_COMPLETA))
                    .thenReturn(Collections.emptyList());
            when(notificacionRepository.existsByReservaIdAndTipo(1L, "RECORDATORIO"))
                    .thenReturn(true);

            // Act
            notificacionService.enviarRecordatorios();

            // Assert
            verify(notificacionRepository, never()).save(any(Notificacion.class));
        }
    }

    @Nested
    @DisplayName("Pruebas del Método Programado (tareasHorarias)")
    class TareasHorariasTests {

        @Test
        @DisplayName("Debe ejecutar la liberación y los recordatorios secuencialmente")
        void debeEjecutarAmbasTareasProgramadas() {
            // Arrange
            when(reservaRepository.findByEstado(EstadosReserva.PENDIENTE_PAGO))
                    .thenReturn(Collections.emptyList());
            when(reservaRepository.findByEstado(EstadosReserva.CONFIRMADA))
                    .thenReturn(Collections.emptyList());
            when(reservaRepository.findByEstado(EstadosReserva.PAGADA_COMPLETA))
                    .thenReturn(Collections.emptyList());

            // Act
            notificacionService.tareasHorarias();

            // Assert
            verify(reservaRepository, times(1)).findByEstado(EstadosReserva.PENDIENTE_PAGO);
            verify(reservaRepository, times(1)).findByEstado(EstadosReserva.CONFIRMADA);
            verify(reservaRepository, times(1)).findByEstado(EstadosReserva.PAGADA_COMPLETA);
        }
    }
}