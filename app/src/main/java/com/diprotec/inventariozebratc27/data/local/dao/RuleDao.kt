package com.diprotec.inventariozebratc27.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.diprotec.inventariozebratc27.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("DELETE FROM reglas")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RuleEntity>)

    @Transaction
    suspend fun replaceAll(items: List<RuleEntity>) {
        clearAll()
        upsertAll(items)
    }

    @Query("SELECT * FROM reglas ORDER BY nombre")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query(
        """
        SELECT *
        FROM reglas
        WHERE TRIM(rutEmpresa) = TRIM(:rutEmpresa)
          AND (
                TRIM(perfil) = TRIM(:perfil)
                OR TRIM(perfil) = TRIM(:perfilId)
              )
        LIMIT 1
        """
    )
    suspend fun getByEmpresaAndPerfil(
        rutEmpresa: String,
        perfil: String,
        perfilId: String
    ): RuleEntity?

    @Query(
        """
        SELECT *
        FROM reglas
        WHERE TRIM(rutEmpresa) = TRIM(:rutEmpresa)
        LIMIT 1
        """
    )
    suspend fun getByEmpresa(
        rutEmpresa: String
    ): RuleEntity?

    @Query("SELECT COUNT(*) FROM reglas")
    suspend fun count(): Int
}