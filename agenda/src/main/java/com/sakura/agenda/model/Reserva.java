package com.sakura.agenda.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long idCliente;

	@Column(nullable = false, length = 250)
	private String correoCliente;

	@Column(nullable = false, length = 120)
	private String nombreCliente;

	@Column(nullable = false)
	private Long servicioId;

	@Column(nullable = false, length = 120)
	private String nombreServicio;

	@Column(nullable = false)
	private Integer duracionMinutos;

	@Column(nullable = false, length = 80)
	private String nombreCategoria;

	@Column(nullable = false)
	private Long profesionalId;

	@Column(nullable = false, length = 80)
	private String nombreProfesional;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(nullable = false)
	private LocalTime horaInicio;

	@Column(nullable = false)
	private LocalTime horaFin;

	@Column(nullable = false, length = 30)
	private String estado;

	@Column(nullable = false)
	private Integer montoTotal;

	@Column(nullable = false)
	private Integer montoAbono;

	@Column(nullable = false)
	private Integer montoPagado;

	private LocalDateTime expiraEn;

	@Column(nullable = false)
	private LocalDateTime creadaEn;

	@Column(length = 500)
	private String notas;

	@Column(length = 300)
	private String motivoCancelacion;

	@Column(length = 40)
	private String serieId;

	@OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	private List<Prestacion> prestaciones = new ArrayList<>();

	@JsonProperty("prestaciones")
	public List<Prestacion> getPrestaciones() {
		if (prestaciones == null || prestaciones.isEmpty()) {
			return List.of(prestacionDesdeResumen());
		}
		return prestaciones.stream().sorted(Comparator.comparingInt(Prestacion::getOrden)).toList();
	}

	public void agregarPrestacion(Prestacion prestacion) {
		prestacion.setReserva(this);
		if (prestaciones == null) {
			prestaciones = new ArrayList<>();
		}
		prestaciones.add(prestacion);
	}

	public List<Prestacion> prestacionesGuardadas() {
		if (prestaciones == null || prestaciones.isEmpty()) {
			return List.of();
		}
		return prestaciones.stream().sorted(Comparator.comparingInt(Prestacion::getOrden)).toList();
	}

	private Prestacion prestacionDesdeResumen() {
		Prestacion prestacion = new Prestacion();
		prestacion.setOrden(1);
		prestacion.setServicioId(servicioId);
		prestacion.setNombreServicio(nombreServicio);
		prestacion.setNombreCategoria(nombreCategoria == null ? "" : nombreCategoria);
		prestacion.setDuracionMinutos(duracionMinutos);
		prestacion.setPrecio(montoTotal);
		prestacion.setProfesionalId(profesionalId);
		prestacion.setNombreProfesional(nombreProfesional);
		prestacion.setHoraInicio(horaInicio);
		prestacion.setHoraFin(horaFin);
		prestacion.setReserva(this);
		return prestacion;
	}

}
