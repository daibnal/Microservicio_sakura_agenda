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
