package com.sakura.agenda.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.exception.ReglaNegocioException;

@Component
public class CatalogoCliente {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${sakura.catalogo.url}")
	private String baseUrl;

	public ServicioDTO obtener(Long id) {
		try {
			ServicioDTO servicio = restTemplate.getForObject(baseUrl + "/api/v1/servicios/" + id, ServicioDTO.class);
			if (servicio == null || servicio.getId() == null) {
				throw new RecursoNoEncontradoException("No existe el servicio");
			}
			return servicio;
		} catch (HttpClientErrorException.NotFound ex) {
			throw new RecursoNoEncontradoException("No existe el servicio");
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo consultar el catálogo");
		}
	}

}
