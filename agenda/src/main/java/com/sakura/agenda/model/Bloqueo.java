package com.sakura.agenda.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bloqueos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bloqueo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "La fecha es obligatoria")
	@Column(nullable = false)
	private LocalDate fecha;

	@NotNull(message = "La hora de inicio es obligatoria")
	@Column(nullable = false)
	private LocalTime horaInicio;

	@NotNull(message = "La hora de término es obligatoria")
	@Column(nullable = false)
	private LocalTime horaFin;

	@Size(max = 200, message = "El motivo no puede superar 200 caracteres")
	@Column(length = 200)
	private String motivo;

}
