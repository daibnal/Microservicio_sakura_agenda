package com.sakura.agenda.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sakura.agenda.exception.NoAutenticadoException;
import com.sakura.agenda.exception.ReglaNegocioException;

@Component
public class PersonasCliente {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${sakura.personas.url}")
	private String baseUrl;

	public SesionDTO obtenerSesion(String token) {
		try {
			return restTemplate.getForObject(baseUrl + "/api/v1/usuarios/sesion/" + token, SesionDTO.class);
		} catch (HttpClientErrorException.NotFound ex) {
			return null;
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo verificar la sesión");
		}
	}

	public void verificarClave(String authorization, String clave) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.AUTHORIZATION, authorization);
			restTemplate.exchange(baseUrl + "/api/v1/auth/verificar", HttpMethod.POST,
					new HttpEntity<>(java.util.Map.of("clave", clave), headers), String.class);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
			throw new NoAutenticadoException("La clave de autorización no es válida");
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo verificar la clave");
		}
	}

}
