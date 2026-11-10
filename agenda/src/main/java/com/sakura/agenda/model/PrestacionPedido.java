package com.sakura.agenda.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrestacionPedido {

	@NotNull(message = "El servicio es obligatorio")
	private Long servicioId;

	@NotNull(message = "El profesional es obligatorio")
	private Long profesionalId;

}
