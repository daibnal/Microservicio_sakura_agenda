package com.sakura.agenda.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sakura.agenda.config.TokenSoporte;
import com.sakura.agenda.exception.AccesoDenegadoException;
import com.sakura.agenda.exception.NoAutenticadoException;

@Component
public class SesionGuard {

	@Autowired
	private PersonasCliente personasCliente;

	public SesionDTO exigirSesion(String authorization) {
		String token = TokenSoporte.extraer(authorization);
		SesionDTO sesion = personasCliente.obtenerSesion(token);
		if (sesion == null || sesion.getId() == null) {
			throw new NoAutenticadoException("Debe iniciar sesión");
		}
		return sesion;
	}

	public SesionDTO exigirAdmin(String authorization) {
		SesionDTO sesion = exigirSesion(authorization);
		if (!"ADMIN".equals(sesion.getRol())) {
			throw new AccesoDenegadoException("No tiene permiso para esta acción");
		}
		return sesion;
	}

}
