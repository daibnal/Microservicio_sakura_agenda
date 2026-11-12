package com.sakura.agenda.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sakura.agenda.exception.ReglaNegocioException;
import com.sakura.agenda.model.Reserva;

@Component
public class PagoCliente {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${sakura.pago.url}")
	private String baseUrl;

	public void registrar(Reserva reserva, String tipo, int monto, boolean aprobado, String authorization) {
		try {
			HttpHeaders headers = new HttpHeaders();
			if (authorization != null) {
				headers.set(HttpHeaders.AUTHORIZATION, authorization);
			}
			Map<String, Object> cuerpo = Map.of(
					"reservaId", reserva.getId(),
					"idCliente", reserva.getIdCliente(),
					"nombreCliente", reserva.getNombreCliente(),
					"nombreServicio", reserva.getNombreServicio(),
					"fechaCita", reserva.getFecha().toString(),
					"tipo", tipo,
					"monto", monto,
					"aprobado", aprobado);
			restTemplate.exchange(baseUrl + "/api/v1/pagos/registro", HttpMethod.POST, new HttpEntity<>(cuerpo, headers),
					String.class);
		} catch (RestClientException ex) {
			throw new ReglaNegocioException("No se pudo registrar el pago");
		}
	}

}
