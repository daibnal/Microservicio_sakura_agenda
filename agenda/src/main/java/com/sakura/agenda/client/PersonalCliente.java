package com.sakura.agenda.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sakura.agenda.exception.RecursoNoEncontradoException;
import com.sakura.agenda.exception.ReglaNegocioException;

@Component
public class PersonalCliente {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${sakura.personal.url}")
	private String baseUrl;

	public ProfesionalDTO obtener(Long id) {
		try {
			ProfesionalDTO profesional = restTemplate.getForObject(baseUrl + "/api/v1/profesionales/" + id,
					ProfesionalDTO.class);
			if (profesional == null || profesional.getId() == null) {
				throw new RecursoNoEncontradoException("No existe el profesional");
			}
			return profesional;
		} catch (HttpClientErrorException.NotFound ex) {
			throw new RecursoNoEncontradoException("No existe el profesional");
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo consultar el personal");
		}
	}

	public List<ServicioDTO> servicios(Long profesionalId) {
		return lista(baseUrl + "/api/v1/profesionales/" + profesionalId + "/servicios",
				new ParameterizedTypeReference<List<ServicioDTO>>() {
				});
	}

	public List<TurnoDTO> turnos(Long profesionalId) {
		return lista(baseUrl + "/api/v1/profesionales/" + profesionalId + "/turnos",
				new ParameterizedTypeReference<List<TurnoDTO>>() {
				});
	}

	public List<AusenciaDTO> ausencias(Long profesionalId) {
		return lista(baseUrl + "/api/v1/profesionales/" + profesionalId + "/ausencias",
				new ParameterizedTypeReference<List<AusenciaDTO>>() {
				});
	}

	private <T> List<T> lista(String url, ParameterizedTypeReference<List<T>> tipo) {
		try {
			List<T> cuerpo = restTemplate.exchange(url, HttpMethod.GET, null, tipo).getBody();
			return cuerpo == null ? List.of() : cuerpo;
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo consultar el personal");
		}
	}

}
