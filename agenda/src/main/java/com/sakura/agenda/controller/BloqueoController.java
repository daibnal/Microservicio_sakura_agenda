package com.sakura.agenda.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sakura.agenda.client.SesionGuard;
import com.sakura.agenda.model.Bloqueo;
import com.sakura.agenda.service.BloqueoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reservas/bloqueos")
public class BloqueoController {

	@Autowired
	private BloqueoService bloqueoService;

	@Autowired
	private SesionGuard sesionGuard;

	@GetMapping
	public List<Bloqueo> listar(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		sesionGuard.exigirAdmin(authorization);
		return bloqueoService.listar(fecha);
	}

	@PostMapping
	public ResponseEntity<Bloqueo> crear(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@Valid @RequestBody Bloqueo bloqueo) {
		sesionGuard.exigirAdmin(authorization);
		return new ResponseEntity<>(bloqueoService.crear(bloqueo), HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long id) {
		sesionGuard.exigirAdmin(authorization);
		bloqueoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}
