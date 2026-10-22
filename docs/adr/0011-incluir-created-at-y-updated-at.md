# ADR-0011: Incluir `createdAt` y `updatedAt` en el modelo de dominio

Fecha: 2026-10-06
Estado: aceptada

## Contexto
La cátedra publica `createdAt` y `updatedAt` en categorías, profesionales y horarios semanales
(I §7, I §14.4). Ningún paso del contrato de sincronización los usa: el orden lo dan las versiones
(E §6.2, I §14.5), la idempotencia usa `eventId` (I §15.1) y los filtros de búsqueda (E §4.1) no los mencionan.

## Opciones consideradas
1. **No incluirlos.**
   + Modelo más chico: solo lo que el negocio usa hoy.
   − Si hacen falta después hay que agregarlos al modelo y a la base.
2. **Incluirlos.**
   + Se conserva todo lo que la cátedra publica; sirven para diagnóstico ("¿cuándo cambió este registro?").
   − Datos que hoy ninguna regla usa; más campos para mapear y mantener.

## Decisión
Opción 2: `createdAt` y `updatedAt` forman parte de `ProfessionalCategory`, `Professional` y `WeeklySchedule`.

## Justificación
Decisión de Nehuen: conservar los datos que publica la cátedra. Se evaluó la opción de no incluirlos (más
simple, y el costo de agregarlos después era bajo porque la base aún no está diseñada); se prefirió guardarlos
por su utilidad para diagnóstico.

## Consecuencias
- Son fechas-hora en UTC (I §4); el tipo concreto se define al escribir el modelo.
- Hay que mapearlos en el adaptador REST, el adaptador Redis y la entidad JPA (issues #4 y #5).
- Quedan fuera de los filtros de búsqueda y de la lógica de versionado salvo que un ADR posterior diga otra cosa.
