package com.diprotec.inventariozebratc27

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.diprotec.inventariozebratc27.core.config.SettingsManager
import com.diprotec.inventariozebratc27.data.local.database.AppDatabase
import com.diprotec.inventariozebratc27.data.local.entity.InventoryEntity
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryStatus
import com.diprotec.inventariozebratc27.data.repository.InventoryRepository
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de estrés de la captura RFID masiva.
 *
 * Simula la llegada de [TOTAL_TAGS] etiquetas únicas y las procesa por el MISMO camino
 * que usa la app en producción: deduplicación en memoria (equivalente a `seenEpcs` del
 * `InventoryRfidViewModel`) y persistencia por lotes transaccionales vía
 * [InventoryRepository.registerRfidInventoryItems] (decodificación GS1 + chequeo de
 * duplicado por índice + búsqueda de descripción + inserción, todo en una transacción).
 *
 * Mide throughput, latencia por lote, memoria y verifica la corrección del deduplicado.
 * Al terminar escribe un informe HTML con gráficos y un CSV con los datos crudos.
 *
 * IMPORTANTE: usa una base de datos SEPARADA ([TEST_DB]) y NO toca los inventarios reales
 * del dispositivo. Tampoco sobrescribe el `deviceId` si el equipo ya está configurado.
 *
 * Ejecutar con:
 *   ./gradlew connectedDebugAndroidTest --tests "*RfidMassiveCaptureStressTest*"
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RfidMassiveCaptureStressTest {

    private companion object {
        const val TAG = "RFID_STRESS"

        /** Cantidad de etiquetas únicas a simular. */
        const val TOTAL_TAGS = 40_000

        /** Debe coincidir con MAX_BATCH_SIZE del InventoryRfidViewModel. */
        const val BATCH_SIZE = 500

        /** Etiquetas ya insertadas que se re-envían para validar el deduplicado. */
        const val DUPLICATE_SAMPLE = 1_000

        const val TEST_DB = "rfid_stress_test.db"
        const val FALLBACK_DEVICE_ID = "STRESS-TEST-DEVICE"
    }

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: InventoryRepository
    private lateinit var settings: SettingsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var inventoryId: Long = 0L
    private var deviceIdWasBlank = false

    @Before
    fun setUp(): Unit = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        context.deleteDatabase(TEST_DB)

        db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build()

        settings = SettingsManager(context)
        settings.start(scope)

        // Espera breve a que el DataStore emita el estado actual del dispositivo.
        delay(750)

        // Si el equipo ya está configurado se reutiliza su deviceId y NO se sobrescribe
        // nada. Solo si viene vacío se coloca uno de prueba, que se limpia al terminar.
        if (settings.deviceId.value.isBlank()) {
            deviceIdWasBlank = true
            settings.saveDeviceId(FALLBACK_DEVICE_ID)

            withTimeout(10_000) {
                while (settings.deviceId.value.isBlank()) {
                    delay(50)
                }
            }
        }

        repository = InventoryRepository(
            appDatabase = db,
            inventoryDao = db.inventoryDao(),
            inventoryItemDao = db.inventoryItemDao(),
            productDao = db.productoDao(),
            locationDao = db.ubicacionDao(),
            ruleDao = db.reglaDao(),
            userDao = db.userDao(),
            settings = settings
        )

        inventoryId = db.inventoryDao().insert(
            InventoryEntity(
                remoteInventoryId = "STRESS-1",
                rutUsuario = "11111111-1",
                name = "Inventario de estrés",
                status = InventoryStatus.PENDING.name,
                tipoLectura = 1
            )
        )
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (deviceIdWasBlank) {
            // Se restaura el estado original para no dejar un deviceId falso.
            settings.saveDeviceId("")
        }

        db.close()
        context.deleteDatabase(TEST_DB)
        scope.cancel()
    }

    @Test
    fun capturaMasivaDe40000Etiquetas(): Unit = runBlocking {
        val metrics = mutableListOf<BatchMetric>()

        // Espeja el caché en memoria del ViewModel para medir su costo real.
        val seenEpcs = mutableSetOf<String>()

        Log.i(TAG, "Iniciando prueba con $TOTAL_TAGS etiquetas, lotes de $BATCH_SIZE")

        val heapStart = usedHeapMb()
        val startAll = SystemClock.elapsedRealtime()

        var processed = 0
        var batchIndex = 0

        while (processed < TOTAL_TAGS) {
            val upTo = minOf(processed + BATCH_SIZE, TOTAL_TAGS)
            val batch = (processed until upTo).map { epcFor(it) }

            // Deduplicado en memoria (lo que hace registerEpcAsCapture antes de encolar).
            batch.forEach { seenEpcs.add(it) }

            val t0 = SystemClock.elapsedRealtime()

            val results = repository.registerRfidInventoryItems(
                inventoryId = inventoryId,
                ubicacionId = "UBI-1",
                ubicacionNombre = "Bodega",
                quantity = 1.0,
                unitMeasure = "UN",
                unitMeasureId = "1",
                rutUsuario = "11111111-1",
                epcs = batch
            )

            val elapsed = SystemClock.elapsedRealtime() - t0

            assertEquals(
                "Todas las etiquetas del lote deben ser nuevas",
                batch.size,
                results.count { !it.isDuplicate }
            )

            processed = upTo
            batchIndex++

            metrics += BatchMetric(
                index = batchIndex,
                size = batch.size,
                elapsedMs = elapsed,
                cumulative = processed,
                heapMb = usedHeapMb()
            )

            if (batchIndex % 20 == 0) {
                Log.i(TAG, "Procesadas $processed/$TOTAL_TAGS etiquetas")
            }
        }

        val totalMs = SystemClock.elapsedRealtime() - startAll
        val heapAfterInsert = usedHeapMb()

        // ---- Verificación de deduplicado: re-enviar etiquetas ya insertadas ----
        val dupBatch = (0 until DUPLICATE_SAMPLE).map { epcFor(it) }
        val dupStart = SystemClock.elapsedRealtime()

        val dupResults = repository.registerRfidInventoryItems(
            inventoryId = inventoryId,
            ubicacionId = "UBI-1",
            ubicacionNombre = "Bodega",
            quantity = 1.0,
            unitMeasure = "UN",
            unitMeasureId = "1",
            rutUsuario = "11111111-1",
            epcs = dupBatch
        )

        val dupMs = SystemClock.elapsedRealtime() - dupStart

        assertTrue(
            "Las etiquetas repetidas deben detectarse como duplicadas",
            dupResults.all { it.isDuplicate }
        )

        // ---- Carga de pendientes: es lo que hace el worker de sincronización ----
        val loadStart = SystemClock.elapsedRealtime()
        val pendientes = repository.getCapturasPendientesSincronizar()
        val loadMs = SystemClock.elapsedRealtime() - loadStart

        assertEquals(
            "No deben insertarse filas duplicadas",
            TOTAL_TAGS,
            pendientes.size
        )

        val syncBlocks = pendientes.chunked(500).size

        val summary = Summary(
            totalTags = TOTAL_TAGS,
            batchSize = BATCH_SIZE,
            totalMs = totalMs,
            avgThroughput = throughput(TOTAL_TAGS, totalMs),
            maxBatchMs = metrics.maxOf { it.elapsedMs },
            minBatchMs = metrics.minOf { it.elapsedMs },
            heapStartMb = heapStart,
            heapAfterInsertMb = heapAfterInsert,
            heapPeakMb = metrics.maxOf { it.heapMb },
            seenEpcsSize = seenEpcs.size,
            duplicateSample = DUPLICATE_SAMPLE,
            duplicateMs = dupMs,
            loadPendingMs = loadMs,
            rowsInDb = pendientes.size,
            syncBlocks = syncBlocks
        )

        val reportDir = context.getExternalFilesDir(null)
            ?: context.filesDir

        val htmlFile = File(reportDir, "rfid_stress_report.html")
        val csvFile = File(reportDir, "rfid_stress_metrics.csv")

        csvFile.writeText(buildCsv(metrics))
        htmlFile.writeText(buildHtml(summary, metrics))

        Log.i(TAG, "===== RESULTADO =====")
        Log.i(TAG, "Etiquetas: ${summary.totalTags} en ${summary.totalMs} ms")
        Log.i(TAG, "Throughput medio: ${fmt(summary.avgThroughput)} etiquetas/s")
        Log.i(TAG, "Lote: min ${summary.minBatchMs} ms / max ${summary.maxBatchMs} ms")
        Log.i(TAG, "Heap: inicio ${fmt(summary.heapStartMb)} MB, pico ${fmt(summary.heapPeakMb)} MB")
        Log.i(TAG, "Duplicados: ${summary.duplicateSample} en ${summary.duplicateMs} ms")
        Log.i(TAG, "Carga de pendientes: ${summary.rowsInDb} filas en ${summary.loadPendingMs} ms")
        Log.i(TAG, "Bloques de envío (500): ${summary.syncBlocks}")
        Log.i(TAG, "INFORME HTML: ${htmlFile.absolutePath}")
        Log.i(TAG, "CSV: ${csvFile.absolutePath}")
        Log.i(TAG, "=====================")
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Genera un EPC SGTIN-96 válido y único por índice (header 0x30 + 22 hex).
     * El índice queda en los bits bajos, que corresponden al serial, de modo que
     * cada etiqueta produce una gs1Key distinta.
     */
    private fun epcFor(index: Int): String {
        return "30" + String.format(Locale.US, "%022X", index.toLong())
    }

    private fun usedHeapMb(): Double {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576.0
    }

    private fun throughput(tags: Int, ms: Long): Double {
        if (ms <= 0L) return 0.0
        return tags * 1000.0 / ms
    }

    private fun fmt(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    private fun buildCsv(metrics: List<BatchMetric>): String {
        val header = "lote,tamano,ms,acumulado,throughput_tags_s,heap_mb\n"

        val rows = metrics.joinToString(separator = "") { m ->
            val tp = throughput(m.size, m.elapsedMs)
            "${m.index},${m.size},${m.elapsedMs},${m.cumulative}," +
                    "${fmt(tp)},${fmt(m.heapMb)}\n"
        }

        return header + rows
    }

    private fun buildHtml(summary: Summary, metrics: List<BatchMetric>): String {
        val throughputSeries = metrics.map { throughput(it.size, it.elapsedMs) }
        val heapSeries = metrics.map { it.heapMb }
        val latencySeries = metrics.map { it.elapsedMs.toDouble() }

        return """
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Prueba de estrés RFID — ${summary.totalTags} etiquetas</title>
<style>
  body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
         margin: 24px; color: #1F2937; background: #F7F7F7; }
  h1 { font-size: 22px; margin-bottom: 4px; }
  h2 { font-size: 16px; margin-top: 28px; }
  .sub { color: #9AA0A6; font-size: 13px; margin-top: 0; }
  .cards { display: flex; flex-wrap: wrap; gap: 12px; margin: 16px 0; }
  .card { background: #fff; border: 1px solid #E2E5E7; border-radius: 10px;
          padding: 12px 16px; min-width: 150px; }
  .card .k { color: #9AA0A6; font-size: 12px; }
  .card .v { font-size: 20px; font-weight: 700; margin-top: 2px; }
  .chart { background: #fff; border: 1px solid #E2E5E7; border-radius: 10px; padding: 12px; }
  table { border-collapse: collapse; background: #fff; width: 100%; font-size: 13px; }
  th, td { border: 1px solid #E2E5E7; padding: 6px 10px; text-align: left; }
  th { background: #F7F7F7; }
</style>
</head>
<body>
<h1>Prueba de estrés — captura RFID masiva</h1>
<p class="sub">${summary.totalTags} etiquetas únicas · lotes de ${summary.batchSize} · una transacción por lote</p>

<div class="cards">
  <div class="card"><div class="k">Tiempo total</div><div class="v">${fmt(summary.totalMs / 1000.0)} s</div></div>
  <div class="card"><div class="k">Throughput medio</div><div class="v">${fmt(summary.avgThroughput)} tags/s</div></div>
  <div class="card"><div class="k">Lote más lento</div><div class="v">${summary.maxBatchMs} ms</div></div>
  <div class="card"><div class="k">Lote más rápido</div><div class="v">${summary.minBatchMs} ms</div></div>
  <div class="card"><div class="k">Heap pico</div><div class="v">${fmt(summary.heapPeakMb)} MB</div></div>
  <div class="card"><div class="k">Filas en BD</div><div class="v">${summary.rowsInDb}</div></div>
</div>

<h2>Throughput por lote (etiquetas/s)</h2>
<div class="chart">${lineChart(throughputSeries, "#2E7D32")}</div>

<h2>Latencia por lote (ms)</h2>
<div class="chart">${lineChart(latencySeries, "#C0392B")}</div>

<h2>Memoria usada (MB)</h2>
<div class="chart">${lineChart(heapSeries, "#F9A825")}</div>

<h2>Detalle</h2>
<table>
  <tr><th>Métrica</th><th>Valor</th></tr>
  <tr><td>Etiquetas simuladas</td><td>${summary.totalTags}</td></tr>
  <tr><td>Tamaño de lote</td><td>${summary.batchSize}</td></tr>
  <tr><td>Tiempo total de persistencia</td><td>${summary.totalMs} ms</td></tr>
  <tr><td>Throughput medio</td><td>${fmt(summary.avgThroughput)} etiquetas/s</td></tr>
  <tr><td>Heap al inicio</td><td>${fmt(summary.heapStartMb)} MB</td></tr>
  <tr><td>Heap tras insertar todo</td><td>${fmt(summary.heapAfterInsertMb)} MB</td></tr>
  <tr><td>Heap pico</td><td>${fmt(summary.heapPeakMb)} MB</td></tr>
  <tr><td>EPCs en caché de sesión (seenEpcs)</td><td>${summary.seenEpcsSize}</td></tr>
  <tr><td>Re-envío de duplicados</td><td>${summary.duplicateSample} en ${summary.duplicateMs} ms</td></tr>
  <tr><td>Filas finales en BD (sin duplicar)</td><td>${summary.rowsInDb}</td></tr>
  <tr><td>Carga de capturas pendientes (worker de sync)</td><td>${summary.loadPendingMs} ms</td></tr>
  <tr><td>Bloques de envío de 500</td><td>${summary.syncBlocks}</td></tr>
</table>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Gráfico de líneas en SVG, sin dependencias externas ni internet.
     */
    private fun lineChart(values: List<Double>, color: String): String {
        if (values.isEmpty()) return "<p>Sin datos</p>"

        val width = 900.0
        val height = 220.0
        val padLeft = 56.0
        val padBottom = 28.0
        val padTop = 12.0
        val padRight = 12.0

        val maxValue = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val plotW = width - padLeft - padRight
        val plotH = height - padTop - padBottom

        val stepX = if (values.size > 1) plotW / (values.size - 1) else 0.0

        val points = values.mapIndexed { i, v ->
            val x = padLeft + i * stepX
            val y = padTop + plotH - (v / maxValue) * plotH
            "${fmt(x)},${fmt(y)}"
        }.joinToString(" ")

        val gridLines = (0..4).joinToString("") { i ->
            val y = padTop + plotH - (i / 4.0) * plotH
            val label = fmt(maxValue * i / 4.0)
            """<line x1="$padLeft" y1="${fmt(y)}" x2="${fmt(width - padRight)}" y2="${fmt(y)}"
                     stroke="#E2E5E7" stroke-width="1"/>
               <text x="4" y="${fmt(y + 4)}" font-size="11" fill="#9AA0A6">$label</text>"""
        }

        return """
<svg viewBox="0 0 ${fmt(width)} ${fmt(height)}" width="100%" preserveAspectRatio="xMidYMid meet">
  $gridLines
  <polyline fill="none" stroke="$color" stroke-width="2" points="$points"/>
  <text x="$padLeft" y="${fmt(height - 8)}" font-size="11" fill="#9AA0A6">lote 1</text>
  <text x="${fmt(width - padRight - 60)}" y="${fmt(height - 8)}" font-size="11" fill="#9AA0A6">lote ${values.size}</text>
</svg>
        """.trimIndent()
    }

    private data class BatchMetric(
        val index: Int,
        val size: Int,
        val elapsedMs: Long,
        val cumulative: Int,
        val heapMb: Double
    )

    private data class Summary(
        val totalTags: Int,
        val batchSize: Int,
        val totalMs: Long,
        val avgThroughput: Double,
        val maxBatchMs: Long,
        val minBatchMs: Long,
        val heapStartMb: Double,
        val heapAfterInsertMb: Double,
        val heapPeakMb: Double,
        val seenEpcsSize: Int,
        val duplicateSample: Int,
        val duplicateMs: Long,
        val loadPendingMs: Long,
        val rowsInDb: Int,
        val syncBlocks: Int
    )
}
