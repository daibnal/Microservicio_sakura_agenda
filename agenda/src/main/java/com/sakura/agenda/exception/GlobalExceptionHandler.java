package com.sakura.agenda.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> manejoErroresValidacion(MethodArgumentNotValidException ex) {
		Map<String, String> errores = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error -> {
			errores.put(error.getField(), error.getDefaultMessage());
		});
		return errores;
	}

	@ExceptionHandler(ReglaNegocioException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> conflicto(ReglaNegocioException ex) {
		return Map.of("mensaje", ex.getMessage());
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> noEncontrado(RecursoNoEncontradoException ex) {
		return Map.of("mensaje", ex.getMessage());
	}

	@ExceptionHandler(NoAutenticadoException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Map<String, String> noAutenticado(NoAutenticadoException ex) {
		return Map.of("mensaje", ex.getMessage());
	}

	@ExceptionHandler(AccesoDenegadoException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Map<String, String> prohibido(AccesoDenegadoException ex) {
		return Map.of("mensaje", ex.getMessage());
	}

}
