# Inventario Zebra TC27 + RFID

Aplicación Android de inventario para el dispositivo **Zebra TC27** conectado a un lector **RFID RFD4030+** (Bluetooth). Permite capturar inventarios por código de barras (láser) y por lectura RFID, gestionarlos localmente y sincronizarlos con el backend.

- **Package:** `com.diprotec.inventariozebratc27`
- **Versión:** 1.0.0
- **minSdk:** 29 · **targetSdk:** 35 · **compileSdk:** 36

## Stack

| Área | Tecnología |
|------|------------|
| UI | Jetpack Compose + Material 3 + Navigation Compose |
| Inyección de dependencias | Hilt |
| Persistencia local | Room (BD versión 30) + DataStore Preferences |
| Red | Retrofit + Moshi + OkHttp |
| Trabajo en segundo plano | WorkManager (Hilt Worker) |
| Hardware Zebra | EMDK (TC27) + RFID API3 (RFD40, AAR local) |

## Arquitectura

Arquitectura por capas dentro de `app/src/main/java/com/diprotec/inventariozebratc27/`:

- **`core/`** — utilidades transversales: red (`ApiCallExecutor`, interceptores, cabeceras firmadas), cripto/firma de dispositivo, sesión, formato, validadores, decodificador GS1/EPC.
- **`data/`** — `local/` (Room: entidades, DAOs, migraciones, DataStore), `remote/` (DTOs, `ApiService`), `mappers/` y `repository/` (patrón interfaz + `Impl`).
- **`di/`** — módulos Hilt (`AppModule`, `DatabaseModule`, `NetworkModule`, `RepositoryModule`, `SecurityModule`, `ServiceModule`).
- **`rfid/`** — `ZebraRfidManager` (conexión, inventario y localización de etiquetas vía SDK Zebra) y modelos de lectura/estado.
- **`scanner/` · `serial/`** — escáner de código de barras y serial del dispositivo Zebra.
- **`service/`** — servicios de negocio: `SyncService` (sincronización de catálogos e inventarios), activación, autenticación, descarga de APK, versión.
- **`worker/`** — workers de sincronización (`CatalogSyncWorker`, `PendingInventorySyncWorker`, `StartupSyncWorker`).
- **`ui/`** — pantallas Compose y ViewModels por feature (login, menú, inventario: crear/capturar/rfid/lista/pendientes, localización RFID, ajustes, uso de datos, actualización, etc.).

## Módulo RFID

`ZebraRfidManager` configura el lector al conectarse aplicando un perfil de lectura de alto rendimiento (potencia RF máxima soportada, sesión de singulación **S0**, DPO desactivado y reporte continuo de etiquetas). La deduplicación de lecturas se realiza a nivel de aplicación por clave GS1 (`InventoryRepository.registerRfidInventoryItem`).

## Compilación

Requisitos: JDK 11+ (probado con Java 21), Android SDK. El SDK de RFID se incluye como AAR local en `app/libs/`.

```bash
./gradlew assembleDebug     # APK de depuración
./gradlew assembleRelease   # APK de release (requiere configuración de firma)
```

El APK se genera como `ZEBRA_TC27_INVENTARIO_<versionName>.apk`.

> **Nota de seguridad:** las llaves de firma (`*.jks`, `*.pem`) y `local.properties` están excluidas del control de versiones mediante `.gitignore`. No deben subirse al repositorio.

## Permisos relevantes

Bluetooth (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`), ubicación fina (descubrimiento BT), EMDK de Symbol y lectura de OEMInfo de Zebra, además de red e instalación de paquetes (actualización OTA de la propia app).
