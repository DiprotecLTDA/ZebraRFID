# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/)
y el proyecto sigue [Versionado Semántico](https://semver.org/lang/es/).

## [No publicado]

### Corregido
- **Lecturas RFID incompletas:** el lector marcaba etiquetas (sonaba) pero la app no
  registraba todas. Causa: nunca se configuraban los parámetros de RF del lector. Ahora
  `ZebraRfidManager` aplica al conectar la potencia RF máxima soportada, sesión de
  singulación **S0**, DPO desactivado y reporte continuo de etiquetas
  (`setUniqueTagReport(false)`), equiparándose al comportamiento de 123RFID. La
  deduplicación se mantiene a nivel de app por clave GS1.
- **Pérdida de lecturas en ráfaga:** `eventReadNotify` ahora drena el buffer por lotes
  hasta vaciarlo; el `SharedFlow` de lecturas usa buffer de 1000 con `DROP_OLDEST` y
  `tryEmit`, evitando bloqueos y descartes bajo alta carga.
- **Fuga de credenciales en logs:** `RawBodyLoggingInterceptor` ahora enmascara las
  cabeceras sensibles (`Authorization`, `X-API-KEY`, firmas de dispositivo) que antes se
  registraban en texto plano.
- **Reemplazo de usuarios no transaccional:** `UserRepositoryImpl.replaceAllUsers` usa la
  operación transaccional `userDao.replaceAll()`, evitando pérdida de datos si falla entre
  el borrado y la inserción.

### Cambiado
- **Logging solo en depuración:** los interceptores `RawBodyLoggingInterceptor` y
  `HttpLoggingInterceptor` se registran únicamente cuando `BuildConfig.DEBUG` es `true`.
- **`ZebraRfidManager`:** unificación de `configureReader`/`configureReaderForLocationing`,
  extracción de un helper común de detención/purga con los mismos tiempos originales, y
  constantes con nombre para los `delay`.
- **`SyncService`:** helper genérico de sincronización de catálogos y helper único para
  finalizar inventarios, conservando retornos, logs y flujo de errores.
- **`InventoryRepository`:** builder privado común de `InventoryItemEntity` y helper único
  de expiración de inventarios pendientes.
- **URL base:** normalización de la barra final unificada en `core/network/BaseUrlUtils`.

### Seguridad
- Se añadió al `.gitignore` la exclusión de llaves de firma (`*.jks`, `*.pem`),
  archivos de build y volcados de la JVM, para evitar su versionado.

## [1.0.0]

### Añadido
- Versión inicial de la aplicación de inventario para Zebra TC27 + lector RFID RFD4030+.
- Captura de inventario por código de barras (láser) y por RFID.
- Gestión local de inventarios con Room y sincronización con el backend vía WorkManager.
- Localización de etiquetas RFID, actualización OTA de la app y gestión de sesión.
