package com.sakura.agenda.model;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prestaciones")
@Data
@NoArgsConstructor
public class Prestacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "reserva_id", nullable = false)
	@JsonIgnore
	private Reserva reserva;

	@Column(name = "orden_tramo", nullable = false)
	private int orden;

	@Column(nullable = false)
	private Long servicioId;

	@Column(nullable = false, length = 120)
	private String nombreServicio;

	@Column(nullable = false, length = 80)
	private String nombreCategoria;

	@Column(nullable = false)
	private Integer duracionMinutos;

	@Column(nullable = false)
	private Integer precio;

	@Column(nullable = false)
	private Long profesionalId;

	@Column(nullable = false, length = 80)
	private String nombreProfesional;

	@Column(nullable = false)
	private LocalTime horaInicio;

	@Column(nullable = false)
	private LocalTime horaFin;

}
