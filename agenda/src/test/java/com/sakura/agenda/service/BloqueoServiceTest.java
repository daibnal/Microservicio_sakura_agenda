package com.sakura.agenda.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.Bloqueo;
import com.sakura.agenda.repository.BloqueoRepository;

@ExtendWith(MockitoExtension.class)
class BloqueoServiceTest {

    @Mock
    private BloqueoRepository bloqueoRepository;

    @InjectMocks
    private BloqueoService bloqueoService;

    @Nested
    @DisplayName("Pruebas para el método listar(LocalDate)")
    class ListarTests {

        @Test
        @DisplayName("Debe retornar todos los bloqueos cuando la fecha es null")
        void listar_FechaNull_RetornaTodosLosBloqueos() {
            // Given
            List<Bloqueo> bloqueosEsperados = List.of(new Bloqueo(), new Bloqueo());
            when(bloqueoRepository.findAll()).thenReturn(bloqueosEsperados);

            // When
            List<Bloqueo> resultado = bloqueoService.listar(null);

            // Then
            assertNotNull(resultado);
            assertEquals(2, resultado.size());
            verify(bloqueoRepository, times(1)).findAll();
            verify(bloqueoRepository, never()).findByFechaOrderByHoraInicioAsc(any());
        }

        @Test
        @DisplayName("Debe retornar bloqueos filtrados y ordenados por fecha cuando la fecha NO es null")
        void listar_FechaValida_RetornaBloqueosDeLaFecha() {
            // Given
            LocalDate fecha = LocalDate.of(2026, 10, 15);
            List<Bloqueo> bloqueosEsperados = List.of(new Bloqueo());
            when(bloqueoRepository.findByFechaOrderByHoraInicioAsc(fecha)).thenReturn(bloqueosEsperados);

            // When
            List<Bloqueo> resultado = bloqueoService.listar(fecha);

            // Then
            assertNotNull(resultado);
            assertEquals(1, resultado.size());
            verify(bloqueoRepository, times(1)).findByFechaOrderByHoraInicioAsc(fecha);
            verify(bloqueoRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("Pruebas para el método crear(Bloqueo)")
    class CrearTests {

        @Test
        @DisplayName("Debe crear el bloqueo correctamente cuando horaInicio es anterior a horaFin")
        void crear_HorasValidas_GuardaYRetornaBloqueo() {
            // Given
            Bloqueo bloqueoEntrada = new Bloqueo();
            bloqueoEntrada.setId(99L); // ID previo para verificar que se blanquee
            bloqueoEntrada.setHoraInicio(LocalTime.of(9, 0));
            bloqueoEntrada.setHoraFin(LocalTime.of(10, 0));

            Bloqueo bloqueoGuardado = new Bloqueo();
            bloqueoGuardado.setId(1L);
            bloqueoGuardado.setHoraInicio(LocalTime.of(9, 0));
            bloqueoGuardado.setHoraFin(LocalTime.of(10, 0));

            when(bloqueoRepository.save(any(Bloqueo.class))).thenReturn(bloqueoGuardado);

            // When
            Bloqueo resultado = bloqueoService.crear(bloqueoEntrada);

            // Then
            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertNull(bloqueoEntrada.getId(), "El ID del bloqueo enviado debe resetearse a null antes de guardar");
            verify(bloqueoRepository, times(1)).save(bloqueoEntrada);
        }

        @Test
        @DisplayName("Debe lanzar ReglaNegocioException cuando horaInicio es IGUAL a horaFin")
        void crear_HoraInicioIgualAHoraFin_LanzaExcepcion() {
            // Given
            LocalTime hora = LocalTime.of(10, 0);
            Bloqueo bloqueo = new Bloqueo();
            bloqueo.setHoraInicio(hora);
            bloqueo.setHoraFin(hora);

            // When & Then
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, 
                () -> bloqueoService.crear(bloqueo)
            );

            assertEquals("La hora de inicio debe ser anterior a la de término", ex.getMessage());
            verify(bloqueoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar ReglaNegocioException cuando horaInicio es POSTERIOR a horaFin")
        void crear_HoraInicioPosteriorAHoraFin_LanzaExcepcion() {
            // Given
            Bloqueo bloqueo = new Bloqueo();
            bloqueo.setHoraInicio(LocalTime.of(11, 0));
            bloqueo.setHoraFin(LocalTime.of(10, 0));

            // When & Then
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, 
                () -> bloqueoService.crear(bloqueo)
            );

            assertEquals("La hora de inicio debe ser anterior a la de término", ex.getMessage());
            verify(bloqueoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Pruebas para el método eliminar(Long)")
    class EliminarTests {

        @Test
        @DisplayName("Debe eliminar el bloqueo correctamente cuando el ID existe")
        void eliminar_IdExistente_EliminaRegistro() {
            // Given
            Long id = 1L;
            when(bloqueoRepository.existsById(id)).thenReturn(true);
            doNothing().when(bloqueoRepository).deleteById(id);

            // When & Then
            assertDoesNotThrow(() -> bloqueoService.eliminar(id));
            verify(bloqueoRepository, times(1)).existsById(id);
            verify(bloqueoRepository, times(1)).deleteById(id);
        }

        @Test
        @DisplayName("Debe lanzar RecursoNoEncontradoException cuando el ID no existe")
        void eliminar_IdNoExistente_LanzaExcepcion() {
            // Given
            Long id = 99L;
            when(bloqueoRepository.existsById(id)).thenReturn(false);

            // When & Then
            RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, 
                () -> bloqueoService.eliminar(id)
            );

            assertEquals("No existe el bloqueo", ex.getMessage());
            verify(bloqueoRepository, times(1)).existsById(id);
            verify(bloqueoRepository, never()).deleteById(any());
        }
    }
}