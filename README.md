# Microservicio Sakura Agenda - Sakura Salón

## Descripción del proyecto
El microservicio de Agenda de Sakura permite gestionar las reservas de horas del salón de belleza, facilitando la programación, modificación y cancelación de citas. Además, permite consultar la disponibilidad de horarios y organizar las atenciones de los clientes y profesionales.

## Integrantes
- Dairys Bernal
- Rocio Bustos
- Amaru Burdiles

## Microservicios relacionados
- Gateway
- Personas / Usuarios
- Personal (Profesionales)
- Catálogo (Servicios)
- Configuración
- Pago

## Rutas principales
- **Crear reserva:** `POST: /api/sakura/v1/agenda/reserva`
- **Listar reservas:** `GET: /api/sakura/v1/agenda/reserva`
- **Gestionar bloqueos:** `POST / PUT / GET: /api/sakura/v1/agenda/bloqueo`

## Documentación Swagger
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI:** http://localhost:8080/v3/api-docs
