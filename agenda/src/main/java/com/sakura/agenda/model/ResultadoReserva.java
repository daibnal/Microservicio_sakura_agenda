package com.sakura.agenda.model;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResultadoReserva {

	private Reserva reserva;

	private List<Reserva> generadas;

	private List<LocalDate> fechasOmitidas;

}
