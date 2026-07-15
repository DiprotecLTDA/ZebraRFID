package com.diprotec.inventariozebratc27.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.diprotec.inventariozebratc27.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("DELETE FROM productos")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProductEntity>)

    @Transaction
    suspend fun replaceAll(items: List<ProductEntity>) {
        clearAll()
        upsertAll(items)
    }

    @Query("SELECT * FROM productos ORDER BY descripcion")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * 
        FROM productos 
        WHERE codigo = :codigo 
           OR codigoSecundario = :codigo 
        LIMIT 1
        """
    )
    suspend fun findByCodigo(codigo: String): ProductEntity?

    /**
     * Resuelve varios códigos de una sola vez, para la captura RFID masiva: hacer una
     * consulta por etiqueta multiplicaría las llamadas a Room en la ruta caliente.
     *
     * Usa COLLATE NOCASE porque los candidatos derivados de un EPC vienen normalizados en
     * mayúsculas y el catálogo puede tener los códigos en otra capitalización (la búsqueda
     * de localización ya compara así).
     *
     * OJO: la lista se expande dos veces, así que consume el doble de parámetros. Hay que
     * trocearla para no superar el límite de SQLite (999 en Android 10).
     */
    @Query(
        """
        SELECT *
        FROM productos
        WHERE codigo COLLATE NOCASE IN (:codigos)
           OR codigoSecundario COLLATE NOCASE IN (:codigos)
        """
    )
    suspend fun findByCodigos(codigos: List<String>): List<ProductEntity>

    @Query(
        """
        SELECT *
        FROM productos
        WHERE codigo LIKE '%' || :query || '%' COLLATE NOCASE
           OR codigoSecundario LIKE '%' || :query || '%' COLLATE NOCASE
           OR descripcion LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY
            CASE
                WHEN codigo = :query COLLATE NOCASE THEN 0
                WHEN codigoSecundario = :query COLLATE NOCASE THEN 1
                WHEN codigo LIKE :query || '%' COLLATE NOCASE THEN 2
                WHEN codigoSecundario LIKE :query || '%' COLLATE NOCASE THEN 3
                WHEN descripcion LIKE :query || '%' COLLATE NOCASE THEN 4
                ELSE 5
            END,
            descripcion ASC,
            codigo ASC
        LIMIT :limit
        """
    )
    suspend fun searchForRfidLocation(
        query: String,
        limit: Int = 25
    ): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun count(): Int
}