package com.sakura.agenda.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sakura.agenda.model.Prestacion;
import com.sakura.agenda.model.Reserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	boolean existsByProfesionalId(Long profesionalId);

	List<Reserva> findByIdClienteOrderByFechaDescHoraInicioDesc(Long idCliente);

	List<Reserva> findByEstado(String estado);

	List<Reserva> findByFechaBetweenOrderByFechaAscHoraInicioAsc(LocalDate desde, LocalDate hasta);

	List<Reserva> findByProfesionalIdAndFechaBetweenAndEstadoIn(Long profesionalId, LocalDate desde, LocalDate hasta,
			java.util.Collection<String> estados);

	@Query("""
			select distinct r from Reserva r
			left join r.prestaciones prestacion
			where r.fecha between :desde and :hasta
			  and r.estado in :estados
			  and (r.profesionalId = :profesionalId or prestacion.profesionalId = :profesionalId)
			""")
	List<Reserva> findHistorial(@Param("profesionalId") Long profesionalId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta, @Param("estados") java.util.Collection<String> estados);

	@Query("""
			select distinct r from Reserva r
			where r.fecha = :fecha
			  and (
			        r.estado = 'CONFIRMADA'
			        or r.estado = 'PAGADA_COMPLETA'
			        or r.estado = 'EN_PROCESO'
			        or (r.estado = 'PENDIENTE_PAGO' and r.expiraEn > :ahora)
			  )
			  and (
			        exists (
			            select prestacion.id from Prestacion prestacion
			            where prestacion.reserva = r
			              and prestacion.profesionalId = :profesionalId
			              and prestacion.horaInicio < :horaFin
			              and prestacion.horaFin > :horaInicio
			        )
			        or (
			            not exists (select prestacion.id from Prestacion prestacion where prestacion.reserva = r)
			            and r.profesionalId = :profesionalId
			            and r.horaInicio < :horaFin
			            and r.horaFin > :horaInicio
			        )
			  )
			""")
	List<Reserva> buscarCruces(@Param("profesionalId") Long profesionalId, @Param("fecha") LocalDate fecha,
			@Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin,
			@Param("ahora") LocalDateTime ahora);

}
