# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/)
y el proyecto sigue [Versionado Semántico](https://semver.org/lang/es/).

## [No publicado]

### Añadido — Configuración RFID ajustable (2026-07-14)
- **Nueva pantalla "Configuración RFID"**, accesible desde el menú principal y **sin
  restricción de perfil** (ruta `rfid_settings`, `ui/rfid/settings/`).
- **Potencia de antena configurable por separado** para *inventario* y *localización*
  (sliders 0-100 %). Se guarda el **porcentaje y no el índice** de potencia: el índice
  depende del lector conectado, mientras que el porcentaje funciona sin lector presente,
  es portable entre modelos y se mapea al índice soportado en el momento de aplicarlo
  (`powerIndexForPercent`, con `coerceIn` sobre el rango real del equipo).
- **Los dos beeps son ajustables de forma independiente:**
  - *Beep del lector* — pitido físico del RFD4030, vía `Config.setBeeperVolume`
    (Alto / Medio / Bajo / Silencio, ver `RfidBeeperVolume`).
  - *Tono de proximidad* — tono que emite la app al localizar (0-100 %; 0 % silencia).
    El `ToneGenerator` se recrea al cambiar el volumen, ya que este se fija al construirlo.
- Botón para restaurar los valores por defecto.
- **Compatibilidad:** sin ajustes guardados el comportamiento es idéntico al anterior
  (inventario 100 %, localización 50 %, beeper Alto, tono 80 %). Reemplaza a la constante
  `LOCATIONING_POWER_DIVISOR`.

### Cambiado — Cierre de captura y envío de capturas (2026-07-14)
- **"Procesando capturas…":** al detener la lectura, el indicador se mantiene hasta que
  termina el vaciado del lote pendiente (antes `stoppingReading` se apagaba antes de
  persistir) y se muestra un overlay bloqueante, para que el operador no salga de la
  pantalla creyendo que el proceso ya terminó.
- **Drenado protegido:** `stopFlusherAndFlushPending()` envuelve **todo el bucle** de
  vaciado en `NonCancellable` (antes solo el lote en vuelo), de modo que si el ViewModel
  se destruye a mitad no se pierden capturas encoladas.
- **Envío de capturas troceado:** `SyncService` envía las capturas en bloques de 500
  (`CAPTURAS_POR_ENVIO`) en lugar de un único request con todas, con cabeceras firmadas
  por request. Cada bloque se marca como sincronizado por separado, así un fallo no
  descarta el progreso de los bloques ya confirmados.

### Rendimiento — Robustez ante lecturas RFID excesivas (2026-07-13)
- **Persistencia por lotes transaccional:** para minimizar la pérdida de etiquetas cuando el
  lector reporta un volumen excesivo, la captura RFID ya no escribe una transacción SQLite por
  etiqueta. `InventoryRfidViewModel` encola los EPC nuevos y los vacía en micro-lotes
  (`FLUSH_INTERVAL_MS` = 150 ms, `MAX_BATCH_SIZE` = 500) mediante el nuevo
  `InventoryRepository.registerRfidInventoryItems`, que inserta el lote completo dentro de una
  sola transacción (`AppDatabase.withTransaction`). Esto amortiza el `fsync`, sube el
  throughput del consumidor y reduce los descartes por saturación.
- **Buffer de lecturas ampliado:** `_tagReads` pasa de 1000 a 16384 (`TAG_READS_BUFFER`),
  conservando `DROP_OLDEST`, para absorber ráfagas grandes sin crecer sin límite en memoria.
- **Sin pérdida al cerrar:** el lote pendiente se **vacía por completo** (en `NonCancellable`)
  antes de limpiar la caché de sesión al detener, finalizar, salir o cargar un inventario.
- **Reintento ante fallo:** si un lote falla al persistir, sus EPC se quitan de `seenEpcs` para
  que el reporte continuo del lector los vuelva a capturar.
- El inventario normal no cambia de resultado: mismas capturas, mismos datos y contadores; solo
  se persisten/refrescan en micro-lotes de ~150 ms. El método individual
  `registerRfidInventoryItem` conserva su comportamiento.

### Diseño — Sistema de diseño unificado (2026-07-13)
- **Design system centralizado:** todo el estilo visual (colores, tipografía, tamaños de
  texto y de botón, espaciados, formas) se movió a la capa de diseño. Se crearon
  `ui/theme/Dimens.kt` (escala de espaciado, alturas, iconos, bordes, radios, elevaciones y
  anchos responsivos), `ui/theme/Shape.kt` (formas Material3 10/16/24 dp) y
  `ui/components/AppComponents.kt` (componentes reutilizables: `AppActionButton`,
  `AppTopBar`, `AppCard`, `AppTextField`, `SectionTitle`, `StatusDot`, `StatusChip`).
- **Tipografía única:** `Type.kt` define una sola `FontFamily` y todos los estilos usados por
  la app (se agregaron `headlineMedium`, `titleSmall` y `bodySmall`, que antes caían a los
  valores por defecto de Material).
- **Colores canónicos:** estados unificados (`StatusOnline` #2E7D32, `StatusWarning` #F9A825,
  `StatusError` #C62828); se eliminaron los colores divergentes y los `Color(0x…)` locales de
  las pantallas.
- **Pantallas migradas a tokens:** 0 valores de diseño hardcodeados en pantallas y
  ViewModels; los controles estándar usan los componentes compartidos.
- **UI listado de capturas:** se quitó la flecha "Volver" del encabezado (y el parámetro
  `onBack` asociado en `InventoryListScreen`/`NavGraph`).
- **Captura RFID:** el aviso *"Etiqueta duplicada ignorada"* dejó de mostrarse; se conserva
  únicamente el contador de duplicados.

### Corregido (2026-07-13)
- **Detector de proximidad RFID (localización):** el porcentaje de distancia no aumentaba
  al acercarse a la etiqueta porque la localización corría a potencia RF máxima y saturaba
  la señal. Ahora `applyReadConfiguration(forLocationing)` aplica potencia **moderada**
  (índice medio, ajustable con la constante `LOCATIONING_POWER_DIVISOR`) en modo
  localización y mantiene la potencia máxima en inventario; conserva sesión **S0** y DPO
  desactivado en ambos.

### Rendimiento (2026-07-13)
- **Deduplicación en memoria de lecturas RFID:** `InventoryRfidViewModel` mantiene un caché
  `seenEpcs` por sesión que descarta las relecturas continuas de una misma etiqueta **sin
  consultar la base de datos** y sin inflar el contador de duplicados. Elimina el martilleo
  de Room y el mensaje repetido *"Etiqueta duplicada ignorada"* que apareció al habilitar el
  reporte continuo. El caché se limpia al cargar, detener, finalizar y salir del inventario.
- **Frescura de la distancia en localización:** `_locateResults` usa buffer de 256 con
  `DROP_OLDEST` y `tryEmit`.

### Cambiado (2026-07-13)
- Textos de log corregidos: `UniqueTagReport` ahora se registra como *"desactivado"*; el log
  genérico de sincronización usa redacción neutra (*"Sincronizado &lt;entidad&gt;: N"*).

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
