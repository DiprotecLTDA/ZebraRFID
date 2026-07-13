package com.diprotec.inventariozebratc27.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE inventories
                ADD COLUMN tipoLectura INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE inventory_items
                ADD COLUMN rfidEpcRaw TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE inventory_items
                ADD COLUMN rfidEpcNormalized TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE inventory_items
                ADD COLUMN rfidGs1Type TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE inventory_items
                ADD COLUMN rfidGs1Key TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE inventory_items
                ADD COLUMN rfidGtin TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE inventory_items
                ADD COLUMN rfidSerial TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE inventory_items
                ADD COLUMN rfidDuplicado INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_inventory_items_inventoryId_rfidGs1Key
                ON inventory_items(inventoryId, rfidGs1Key)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_inventory_items_inventoryId_rfidEpcNormalized
                ON inventory_items(inventoryId, rfidEpcNormalized)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_inventory_items_sincronizado_rfidDuplicado_remoteInventoryId_createdAt
                ON inventory_items(sincronizado, rfidDuplicado, remoteInventoryId, createdAt)
                """.trimIndent()
            )
        }
    }
}