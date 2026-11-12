package com.sakura.agenda.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sakura.agenda.exception.ReglaNegocioException;

@Component
public class ConfiguracionCliente {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${sakura.configuracion.url}")
	private String baseUrl;

	public ConfiguracionDTO obtener() {
		try {
			ConfiguracionDTO configuracion = restTemplate.getForObject(baseUrl + "/api/v1/configuracion/vigente",
					ConfiguracionDTO.class);
			if (configuracion == null) {
				throw new ReglaNegocioException("No se pudo consultar la configuración");
			}
			return configuracion;
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo consultar la configuración");
		}
	}

}
