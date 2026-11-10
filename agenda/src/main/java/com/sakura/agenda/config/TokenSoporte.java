package com.sakura.agenda.config;

import com.sakura.agenda.exception.NoAutenticadoException;

public final class TokenSoporte {

	private TokenSoporte() {
	}

	public static String extraer(String authorization) {
		if (authorization == null || authorization.isBlank()) {
			throw new NoAutenticadoException("Debe iniciar sesión");
		}
		String token = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
		if (token.isEmpty()) {
			throw new NoAutenticadoException("Debe iniciar sesión");
		}
		return token;
	}

}
