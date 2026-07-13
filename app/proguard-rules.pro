# =========================
# DIPROTEC INVENTARIO ZEBRA TC27
# ProGuard / R8 rules
# =========================


# -------------------------
# Kotlin / Coroutines
# -------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**


# -------------------------
# AndroidX / Compose
# Normalmente Compose funciona bien con R8,
# pero estas reglas evitan problemas en previews/tooling y clases sintéticas.
# -------------------------
-dontwarn androidx.compose.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.navigation.**


# -------------------------
# Hilt / Dagger
# -------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends androidx.hilt.work.HiltWorker { *; }

-dontwarn dagger.hilt.**
-dontwarn javax.inject.**
-dontwarn dagger.**


# -------------------------
# Room
# -------------------------
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

-dontwarn androidx.room.**


# -------------------------
# WorkManager
# Mantiene workers que pueden ser instanciados por nombre.
# -------------------------
-keep class com.diprotec.inventariozebratc27.worker.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

-dontwarn androidx.work.**


# -------------------------
# Retrofit / OkHttp
# -------------------------
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep interface com.diprotec.inventariozebratc27.data.remote.api.** { *; }

-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**


# -------------------------
# Moshi
# DTOs usados por Retrofit/Moshi.
# Mantener nombres/campos para serialización JSON.
# -------------------------
-keep class com.diprotec.inventariozebratc27.data.remote.dto.** { *; }
-keep class com.squareup.moshi.** { *; }

-dontwarn com.squareup.moshi.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**


# -------------------------
# ErrorProne / Java annotation APIs
# Algunas dependencias traen anotaciones de compilación que no existen en Android runtime.
# R8 puede detectarlas en release, pero no son necesarias para ejecutar la app.
# Corrige errores como:
# Missing class javax.lang.model.element.Modifier
# -------------------------
-dontwarn javax.lang.model.**
-dontwarn com.google.errorprone.**
-dontwarn org.checkerframework.**


# -------------------------
# DataStore
# -------------------------
-dontwarn androidx.datastore.**


# -------------------------
# Zebra EMDK / DataWedge / RFID SDK
# El EMDK se resuelve en runtime en los dispositivos Zebra.
# Se mantienen las clases Zebra para evitar que R8 elimine/renombre APIs usadas por el SDK.
# -------------------------
-keep class com.symbol.** { *; }
-keep class com.zebra.** { *; }
-keep class com.zebra.rfid.api3.** { *; }

-dontwarn com.symbol.**
-dontwarn com.zebra.**
-dontwarn com.zebra.rfid.api3.**


# -------------------------
# Serial / Zebra providers internos de la app
# -------------------------
-keep class com.diprotec.inventariozebratc27.serial.** { *; }


# -------------------------
# Criptografía / Firma dispositivo
# Mantener clases de llaves, firma y headers protegidos.
# -------------------------
-keep class com.diprotec.inventariozebratc27.core.crypto.** { *; }
-keep class com.diprotec.inventariozebratc27.core.key.** { *; }
-keep class com.diprotec.inventariozebratc27.core.network.ProtectedHeadersBuilder { *; }


# -------------------------
# Configuración / sesión
# -------------------------
-keep class com.diprotec.inventariozebratc27.core.config.** { *; }
-keep class com.diprotec.inventariozebratc27.core.session.** { *; }


# -------------------------
# Modelos locales Room
# -------------------------
-keep class com.diprotec.inventariozebratc27.data.local.entity.** { *; }
-keep class com.diprotec.inventariozebratc27.data.local.dao.** { *; }


# -------------------------
# Repositorios y servicios principales
# No es estrictamente obligatorio, pero reduce riesgo mientras estabilizamos release.
# -------------------------
-keep class com.diprotec.inventariozebratc27.service.** { *; }
-keep class com.diprotec.inventariozebratc27.data.repository.** { *; }


# -------------------------
# RFID interno app
# Mantiene las clases nuevas creadas para RFD4031 / RFD40.
# -------------------------
-keep class com.diprotec.inventariozebratc27.rfid.** { *; }
-keep class com.diprotec.inventariozebratc27.ui.rfid.** { *; }


# -------------------------
# MainActivity / Application
# -------------------------
-keep class com.diprotec.inventariozebratc27.App { *; }
-keep class com.diprotec.inventariozebratc27.ui.main.MainActivity { *; }


# -------------------------
# Coil
# -------------------------
-dontwarn coil.**


# -------------------------
# Warnings seguros habituales
# -------------------------
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**