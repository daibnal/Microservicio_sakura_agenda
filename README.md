# Microservicio Sakura Agenda - Sakura Salón

## Descripción del proyecto
El microservicio de Agenda de Sakura permite gestionar las reservas de horas del salón de belleza, facilitando la programación, modificación y cancelación de citas. Además, permite consultar la disponibilidad de horarios y organizar las atenciones de los clientes y profesionales.

## Integrantes
- Dairys Bernal
- Rocio Bustos
- Amaru Burdiles

## Microservicios relacionados
- Gateway
- Personas (Usuarios)
- Personal (Profesionales)
- Catálogo (Servicios)
- Configuración
- Pago
- Iventario
- Reporte

## Rutas principales
- **Crear reserva:** `POST: /api/sakura/v1/agenda/reservas`
- **Listar reservas:** `GET: /api/sakura/v1/agenda/reservas`
- **Obtener reserva por ID: GET:** `/api/sakura/v1/agenda/reservas/{id}`
- **Actualizar estado de reserva: PUT:** `/api/sakura/v1/agenda/reservas/{id}/estado`
- **Crear bloqueo:** `/api/sakura/v1/agenda/bloqueos`
- **Listar bloqueos: GET:** `/api/sakura/v1/agenda/bloqueos`

## Documentación Swagger
- **Swagger UI:** http://localhost:8084/swagger-ui/index.html
- **OpenAPI:** http://localhost:8084/v3/api-docs

## Ejecución mediante API Gateway
Si el API Gateway se encuentra en ejecución, las solicitudes pueden realizarse mediante:
http://localhost:9000/api/sakura/v1/agenda/reservas

## Ejemplos de Json
{
"crear_reserva_request": {
"clienteId": 104,
"nombreCliente": "Valentina Araya",
"telefonoCliente": "+56955667788",
"correoCliente": "valentina.araya@gmail.com",
"servicioId": 3,
"nombreServicio": "Lifting de Pestañas + Tinte",
"especialistaId": 2,
"nombreEspecialista": "Camila Valenzuela",
"fechaReserva": "2026-11-25",
"horaInicio": "15:30:00",
"horaFin": "16:30:00",
"observaciones": "Cliente prefiere curvatura natural."
},
"crear_reserva_response_201": {
"id": 48,
"codigoReserva": "SAK-20261125-048",
"clienteId": 104,
"nombreCliente": "Valentina Araya",
"telefonoCliente": "+56955667788",
"correoCliente": "valentina.araya@gmail.com",
"servicioId": 3,
"nombreServicio": "Lifting de Pestañas + Tinte",
"especialistaId": 2,
"nombreEspecialista": "Camila Valenzuela",
"fechaReserva": "2026-11-25",
"horaInicio": "15:30:00",
"horaFin": "16:30:00",
"estado": "CONFIRMADA",
"observaciones": "Cliente prefiere curvatura natural.",
"creadoEn": "2026-11-19T13:10:00Z"
},
"listar_reservas_response_200": [
{
"id": 48,
"codigoReserva": "SAK-20261125-048",
"nombreCliente": "Valentina Araya",
"nombreServicio": "Lifting de Pestañas + Tinte",
"nombreEspecialista": "Camila Valenzuela",
"fechaReserva": "2026-11-25",
"horaInicio": "15:30:00",
"horaFin": "16:30:00",
"estado": "CONFIRMADA"
},
{
"id": 49,
"codigoReserva": "SAK-20261125-049",
"nombreCliente": "Sofía Castro",
"nombreServicio": "Manicure Permanente",
"nombreEspecialista": "Isadora Reyes",
"fechaReserva": "2026-11-25",
"horaInicio": "17:00:00",
"horaFin": "18:00:00",
"estado": "PENDIENTE"
}
],
"registrar_bloqueo_request": {
"especialistaId": 2,
"fecha": "2026-11-26",
"horaInicio": "13:00:00",
"horaFin": "14:00:00",
"motivo": "Horario de colación"
},
"registrar_bloqueo_response_201": {
"id": 12,
"especialistaId": 2,
"nombreEspecialista": "Camila Valenzuela",
"fecha": "2026-11-26",
"horaInicio": "13:00:00",
"horaFin": "14:00:00",
"motivo": "Horario de colación",
"activo": true
},
"error_conflicto_horario_409": {
"timestamp": "2026-11-19T13:12:45Z",
"status": 409,
"error": "Conflict",
"message": "El bloque horario seleccionado (15:30 - 16:30) ya se encuentra ocupado para la especialista.",
"path": "/api/sakura/v1/agenda/reservas"
},
"error_validacion_campos_400": {
"timestamp": "2026-11-19T13:14:10Z",
"status": 400,
"error": "Bad Request",
"message": "Error de validación en los datos enviados",
"detalles": {
"correoCliente": "El formato del correo electrónico no es válido",
"fechaReserva": "La fecha de reserva debe ser igual o posterior a la fecha actual"
},
"path": "/api/sakura/v1/agenda/reservas"
}
}

