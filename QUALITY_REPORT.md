# Reporte de Evaluación Integral de Calidad de Software
**Proyecto:** `rtm-payment-billing` (Microservicio de Pagos por Transferencia y Facturación)
**Fecha:** Octubre 2026
**Evaluador:** Jules Software Engineering Agent

---

## 1. Resumen Ejecutivo

Se ha llevado a cabo una auditoría integral de calidad de software del microservicio **`rtm-payment-billing`**. Se evaluaron cinco dimensiones clave:
1. **Arquitectura y Diseño (Arquitectura Hexagonal / Puertos y Adaptadores)**
2. **Código Limpio y Mantenibilidad (Clean Code & SOLID)**
3. **Seguridad y Control de Acceso**
4. **Manejo de Errores, Excepciones y Resiliencia**
5. **Posibles Bugs, Concurrencia y Casos Borde**

**Diagnóstico General:**
El repositorio muestra una excelente adopción del patrón de **Arquitectura Hexagonal**. El dominio está completamente desacoplado del framework (Spring Boot) y de dependencias externas. El código es claro, expresivo y cuenta con una arquitectura de seguridad sólida basada en verificación de firmas JWT RS256 mediante clave pública y filtros para la API interna.

Sin embargo, existen áreas de mejora críticas relacionadas con **transaccionalidad en casos de uso, resiliencia/consistencia eventual en eventos integrados, gestión de hilos y concurrencia en la generación de correlativos de facturas, y cobertura de pruebas unitarias/de integración.**

---

## 2. Arquitectura y Diseño

### Fortalezas
- **Separación de Capas Rigurosa (Hexagonal Architecture):**
  - **Domain (`com.rentamovil.payment.domain`):** Contiene entidades puras (`Payment`, `Invoice`, `BankAccount`, `AuthenticatedUser`), reglas de negocio, enumeraciones y puertos de entrada/salida (`port.in`, `port.out`). No posee ninguna anotación de Spring ni librería de terceros salvo Jackson/Lombok en DTOs o tipos básicos.
  - **Application (`com.rentamovil.payment.application`):** Implementa los casos de uso (`PaymentUseCaseImpl`, `InvoiceUseCaseImpl`, `BankAccountUseCaseImpl`). Depende únicamente de los puertos del dominio.
  - **Infrastructure (`com.rentamovil.payment.infrastructure`):** Aísla la persistencia (Spring Data JPA), controladores REST, adaptadores de seguridad (`JwtVerifierAdapter`), generador de PDF (`OpenPdfInvoiceGenerator`), cliente HTTP (`ReservationClientAdapter`) y publicador de eventos (`BookingEventForwarderAdapter`).
- **Inyección de Dependencias Limpia:** Se realiza la configuración de beans explícitamente en `BeanConfig.java`, evitando acoplar las clases del dominio con `@Service` o `@Component`.

### Áreas de Mejora
1. **Falta de Demarcación Transaccional en Casos de Uso (`@Transactional`):**
   - En `PaymentUseCaseImpl.approve()`, se guarda el pago como aprobado, se audita, se publica un evento HTTP a `booking-reservation` y se genera/guarda la factura.
   - Dado que los casos de uso son POJOs sin anotaciones de Spring, actualmente no hay `@Transactional` ni a nivel de adaptador ni de caso de uso. Si ocurre un fallo guardando la factura o en el PDF, la base de datos no revertirá automáticamente las modificaciones previas a menos que la capa superior o el adaptador abra una transacción.

---

## 3. Código Limpio y Mantenibilidad (Clean Code & SOLID)

### Fortalezas
- **Inmutabilidad y Encapsulación:** Entidades como `Payment` e `Invoice` utilizan métodos de fábrica estáticos y métodos de dominio inmutables (`approve()`, `reject()`, `withPdfUrl()`) que devuelven nuevas instancias o modifican el estado de forma controlada.
- **Naming y Claridad:** Nombres de clases, variables y métodos claros y alineados con las Historias de Usuario (HU-PAYMENT-001 a 005).
- **Mapeadores Aislados:** `PersistenceMapper` aísla las conversiones entre entidades de dominio y entidades JPA (`PaymentEntity`, `InvoiceEntity`, etc.).

### Áreas de Mejora
1. **Generación de Número de Factura en `InvoiceJpaRepository`:**
   - La generación del número correlativo de factura (`nextInvoiceNumber()`) calcula `count() + 1` en consulta SQL:
     ```java
     default String nextInvoiceNumber() {
         long next = count() + 1;
         return String.format("FAC-%06d", next);
     }
     ```
   - **Riesgo:** Si dos pagos se aprueban simultáneamente en hilos o instancias concurrentes, ambas lecturas obtendrán el mismo `count()`, generando duplicados del número de factura. Aunque la restricción de unicidad en la BD capturará esto tirando `DataIntegrityViolationException`, el número de factura no debe basarse en un `count()` expuesto a condiciones de carrera.

---

## 4. Análisis de Seguridad

### Fortalezas
- **Verificación JWT RS256 sin Clave Privada:** `JwtVerifierAdapter` verifica la firma del token emitido por el servicio de IAM usando únicamente la clave pública en formato PEM/RS256.
- **Protección de API Interna (`InternalApiKeyFilter`):** Endpoints bajo `/internal/**` están protegidos por una clave secreta compartida (`X-Internal-Key`).
- **Inyección de Usuario Autenticado Segura:** `CurrentUserArgumentResolver` extrae y valida el JWT de la cabecera `Authorization: Bearer <token>`, inyectando `@CurrentUser AuthenticatedUser` en los controladores sin confiar en datos enviados en el body.
- **Validación de URLs de Archivos (`allowedFileUrlPrefix`):** Se verifica que las URLs de comprobantes y QRs pertenezcan únicamente al prefijo/cuenta configurado de Cloudinary, evitando SSRF o enlaces a orígenes no autorizados.

### Áreas de Mejora
1. **Validación del Algoritmo JWT:**
   - `JwtVerifierAdapter` parsea el token y usa `RSASSAVerifier`. Es recomendable verificar explícitamente que el header del JWT utilice el algoritmo esperado `RS256` para evitar ataques de degradación de algoritmo (ej. algoritmo "none" o HMAC con clave pública).
2. **Sensibilidad a Mayúsculas/Minúsculas en Prefix Checklist:**
   - `checkFileUrl` usa `startsWith(allowedPrefix)`. Se debe asegurar que las URLs sean normalizadas antes de la comparación para evitar evasiones mediante variantes en el esquema o dominio.

---

## 5. Manejo de Errores, Excepciones y Resiliencia

### Fortalezas
- **`GlobalExceptionHandler` Unificado:** Mapea excepciones de dominio a DTOs de error estándar (`ErrorResponse`) con códigos legibles (`VALIDATION_ERROR`, `AMOUNT_MISMATCH`, `INVALID_TOKEN`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`).
- **Mapeo de Errores DB a Mensajes de Negocio:** Captura `DataIntegrityViolationException` e identifica violaciones a constraints como `ck_payment_status`, `ck_payment_amount` y `uq_invoice_reservation_id`.
- **Protección contra Fuga de Datos (Data Leakage):** Los detalles de excepciones no controladas (`Exception.class`) se registran en los logs del servidor y devuelven una respuesta genérica `INTERNAL_ERROR` 500 al cliente.

### Áreas de Mejora
1. **Falta de Reintentos / Outbox Pattern en `BookingEventForwarderAdapter`:**
   - Si el microservicio `booking-reservation` no está disponible o falla por timeout al publicar un evento (`PaymentCompleted`, `PaymentRejected`, etc.), la excepción se captura y se registra un log de error, pero el evento **se pierde**.
   - **Recomendación:** Considerar el patrón Outbox o una cola de mensajería (RabbitMQ/Kafka) o reintentos asíncronos para garantizar entrega eventual (*at-least-once delivery*).

---

## 6. Posibles Bugs y Casos Borde

1. **Condición de Carrera en `nextInvoiceNumber()`:**
   - Como se identificó, la consulta `count() + 1` no es atómica bajo concurrencia elevada.
2. **Coexistencia de Formatos de Fecha y Zona Horaria:**
   - Spring Boot está configurado con `jdbc.time_zone=UTC`. Se debe garantizar que la generación de fechas en entidades use `LocalDateTime.now(ZoneOffset.UTC)` o `Instant` para evitar desfases cuando el servidor corra en una zona horaria distinta.
3. **Manejo de Reentrancia en Aprobación de Pagos:**
   - Si se invoca `approve()` sobre un pago que ya está en estado `APPROVED`, `Payment.approve()` lanza `BusinessRuleException("PAYMENT_ALREADY_REVIEWED")`. Esto es correcto y previene doble aprobación.

---

## 7. Recomendaciones y Plan de Acción

| Prioridad | Componente | Recomendación |
| :--- | :--- | :--- |
| **Alta** | **Casos de Uso / Persistencia** | Agregar gestión explícita de transacciones (`@Transactional` en adaptadores de repositorio o servicios aplicativos) para evitar escrituras parciales. |
| **Alta** | **Facturación** | Reemplazar `count() + 1` en `nextInvoiceNumber()` por una secuencia de base de datos (`SEQUENCE`) o un contador atómico para evitar duplicados en concurrencia. |
| **Media** | **Resiliencia Eventos** | Implementar mecanismo de reintentos o almacenamiento temporal (Outbox pattern) para eventos dirigidos a `booking-reservation`. |
| **Media** | **Seguridad JWT** | Forzar la validación explícita de `JWSAlgorithm.RS256` en `JwtVerifierAdapter`. |
| **Media** | **Pruebas** | Expandir la cobertura de pruebas unitarias para `BankAccountUseCaseImpl`, `InvoiceUseCaseImpl`, `InternalApiKeyFilter` y `GlobalExceptionHandler`. |

---
*Reporte generado exitosamente.*
