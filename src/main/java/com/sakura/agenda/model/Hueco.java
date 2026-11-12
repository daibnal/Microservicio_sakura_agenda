package com.sakura.agenda.model;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hueco {

	private LocalTime horaInicio;

	private LocalTime horaFin;

}
