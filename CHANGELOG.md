# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/)
y el proyecto sigue [Versionado Semántico](https://semver.org/lang/es/).

## [No publicado]

### Corregido — Resolución de productos en la captura RFID (2026-07-15)
- **Las etiquetas RFID no resolvían su producto contra el catálogo.** La captura consultaba
  Room usando el **GTIN** (si el EPC era SGTIN-96) o el **EPC en hexadecimal** en cualquier
  otro caso. Como en terreno conviven ambas codificaciones —GS1 SGTIN-96 y el código del
  producto grabado en crudo—, las del segundo grupo **nunca coincidían** y caían siempre en
  "Producto no registrado". Y ese valor no es solo la etiqueta en pantalla: es lo que se
  envía al backend como `ProductoCodigo`, así que se reportaban códigos desconocidos.
- **Ahora se derivan candidatos del EPC** (nuevo `core/gs1/RfidProductCodeCandidates`), que
  es la operación inversa de la que ya hacía la pantalla de localización: GTIN embebido, EPC
  tal cual, EPC sin ceros a la izquierda, y el hex decodificado como ASCII (con y sin
  relleno). Se consulta el catálogo con todos ellos.
- **La detección replica la del inventario láser:** se busca por `codigo`/`codigoSecundario`
  y, si no hay coincidencia —o el producto no tiene descripción—, la captura se registra
  igual con "Producto no registrado". Nunca se bloquea la lectura.
- **Al resolver el producto se guarda el `codigo` del catálogo** (que es lo que recibe el
  backend); si no resuelve, se conserva el GTIN/EPC como traza.
- La comparación es **insensible a mayúsculas** (`COLLATE NOCASE`), porque los candidatos
  derivados de un EPC vienen normalizados en mayúsculas y el catálogo puede tener otra
  capitalización. La localización ya comparaba así; ahora la captura es coherente.
- El deduplicado **sigue basándose en la clave del EPC**, nunca en el código del producto:
  dos etiquetas físicas del mismo artículo deben contar por separado.

### Rendimiento — Resolución por lote: 5,7× más rápido (2026-07-15)
- La ruta de captura RFID pasó de **3 llamadas a Room por etiqueta** (contar duplicado,
  buscar producto, insertar) a **3 por lote de 500**: una consulta de duplicados, una de
  productos (troceada) y un único `insertAll`. Las mediciones mostraban que el coste lo
  dominaba el **número de llamadas**, no el trabajo de SQLite (1 llamada ≈ 0,6 ms;
  3 llamadas ≈ 3 ms por etiqueta).
- **Resultado medido con 40.000 etiquetas en TC27:**

  | Métrica | Antes | Después | Mejora |
  |---|---|---|---|
  | Throughput | 325 etiquetas/s | **1.838 etiquetas/s** | **5,7×** |
  | Tiempo total | 123,2 s | **21,8 s** | 5,7× |
  | Lote más lento | 2.932 ms | **369 ms** | 7,9× |
  | Lote más rápido | 1.110 ms | **155 ms** | 7,2× |
  | 1.000 duplicados | 571 ms | **214 ms** | 2,7× |
  | Crecimiento de heap | +43,2 MB | **+11,8 MB** | 3,7× |

- Como efecto secundario, **el vaciado al detener deja de ser un problema**: a 1.838
  etiquetas/s el lector ya no produce más rápido de lo que la base de datos escribe.
- Se añadieron `InventoryItemDao.insertAll` y `findExistingRfidGs1Keys`, y
  `ProductDao.findByCodigos`. Los parámetros se trocean para respetar el límite de 999 de
  SQLite en Android 10 (la consulta de productos expande la lista dos veces).

### Añadido — Prueba de estrés de captura RFID masiva (2026-07-15)
- Nuevo test instrumentado `RfidMassiveCaptureStressTest` que simula **40.000 etiquetas
  únicas** por el mismo camino que usa la app (deduplicación en memoria + persistencia por
  lotes transaccionales de 500), **sin necesitar etiquetas físicas ni el lector**: genera
  EPCs SGTIN-96 válidos y únicos por índice. Mide throughput, latencia por lote y memoria;
  verifica que el deduplicado detecte los re-envíos y que no se inserten filas duplicadas;
  y mide la carga de capturas pendientes que hace el worker de sincronización.
- Al terminar escribe un informe HTML autocontenido con gráficos SVG y un CSV con los datos
  crudos en `Android/data/<package>/files/`. El resumen sale por logcat con el tag
  `RFID_STRESS`.
- Usa una base de datos separada (`rfid_stress_test.db`, borrada al terminar) y no
  sobrescribe el `deviceId` si el equipo ya está configurado: **no toca datos reales**.

### Medición — Resultados con 40.000 etiquetas (2026-07-15)
Medido en Zebra TC27:
- **Throughput: ~325-333 etiquetas/s** (~3 ms por etiqueta nueva). Es el techo real de
  escritura del dispositivo.
- **Memoria: pico de 47,7 MB** partiendo de 4,5 MB. Sin riesgo de OOM a este volumen.
- **Deduplicado: 0,57 ms por etiqueta**, ~5 veces más barato que insertar. Confirma que el
  caché en memoria `seenEpcs` es lo que hace viable el reporte continuo del lector.
- **Carga de 40.000 pendientes: ~1,1 s** → 80 bloques de envío de 500.
- **Consecuencia operativa:** el lector produce (~700-900 tags/s) más rápido de lo que la BD
  escribe (~330/s). No se pierden lecturas (el backlog vive en memoria, acotado y barato),
  pero **el vaciado al detener puede tardar más de un minuto con decenas de miles de
  capturas**. Esto valida la necesidad del indicador "Procesando capturas…".

### Investigado y descartado — Poda de índices de `inventory_items` (2026-07-15)
- **Hipótesis:** de los 9 índices de `inventory_items` solo 3 sirven a una consulta real
  (verificado contra todo `InventoryItemDao`, que es el único lugar que consulta la tabla).
  Se supuso que mantener 6 árboles B muertos en cada inserción era el cuello de botella.
- **Se probó:** poda a 3 índices + migración 30→31 con `DROP INDEX`.
- **Resultado: la hipótesis era incorrecta.** La ganancia medida fue de solo **+2,6 %**
  (325 → 333 etiquetas/s), con signos mezclados entre métricas (el deduplicado salió 10,3 %
  más lento y la carga de pendientes 2,7 % más lenta), lo que indica **ruido de una sola
  corrida** y no una mejora real.
- **Decisión: revertido.** Una migración de esquema en producción no se justifica sin una
  mejora clara. El coste dominante de la inserción **no es** el mantenimiento de índices,
  sino el overhead por fila de Room/SQLite y la escritura WAL.
- Se deja documentado para no repetir la investigación. Los 6 índices sin uso siguen ahí:
  son peso muerto en disco, pero no afectan al rendimiento de forma medible.

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
