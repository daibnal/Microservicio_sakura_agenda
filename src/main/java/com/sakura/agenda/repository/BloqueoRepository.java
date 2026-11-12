package com.sakura.agenda.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sakura.agenda.model.Bloqueo;

@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

	List<Bloqueo> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

	@Query("""
			select b from Bloqueo b
			where b.fecha = :fecha
			  and b.horaInicio < :horaFin
			  and b.horaFin > :horaInicio
			""")
	List<Bloqueo> buscarCruces(@Param("fecha") LocalDate fecha, @Param("horaInicio") LocalTime horaInicio,
			@Param("horaFin") LocalTime horaFin);

}
