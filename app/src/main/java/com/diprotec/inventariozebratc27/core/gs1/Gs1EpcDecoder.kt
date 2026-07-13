package com.diprotec.inventariozebratc27.core.gs1

import java.math.BigInteger

data class Gs1EpcDecoded(
    val epcRaw: String,
    val epcNormalized: String,
    val gs1Type: String,
    val gs1Key: String,
    val gtin: String? = null,
    val serial: String? = null,
    val isRecognized: Boolean = false
)

object Gs1EpcDecoder {

    private const val TYPE_UNKNOWN = "UNKNOWN"
    private const val TYPE_EPC_URI = "EPC_URI"
    private const val TYPE_SGTIN_96 = "SGTIN-96"
    private const val TYPE_SSCC_96 = "SSCC-96"
    private const val TYPE_SGLN_96 = "SGLN-96"
    private const val TYPE_GRAI_96 = "GRAI-96"
    private const val TYPE_GID_96 = "GID-96"

    private data class Partition(
        val companyPrefixBits: Int,
        val companyPrefixDigits: Int,
        val referenceBits: Int,
        val referenceDigits: Int
    )

    private val sgtinPartitionTable = mapOf(
        0 to Partition(40, 12, 4, 1),
        1 to Partition(37, 11, 7, 2),
        2 to Partition(34, 10, 10, 3),
        3 to Partition(30, 9, 14, 4),
        4 to Partition(27, 8, 17, 5),
        5 to Partition(24, 7, 20, 6),
        6 to Partition(20, 6, 24, 7)
    )

    private val ssccPartitionTable = mapOf(
        0 to Partition(40, 12, 18, 5),
        1 to Partition(37, 11, 21, 6),
        2 to Partition(34, 10, 24, 7),
        3 to Partition(30, 9, 28, 8),
        4 to Partition(27, 8, 31, 9),
        5 to Partition(24, 7, 34, 10),
        6 to Partition(20, 6, 38, 11)
    )

    private val sglnPartitionTable = mapOf(
        0 to Partition(40, 12, 1, 0),
        1 to Partition(37, 11, 4, 1),
        2 to Partition(34, 10, 7, 2),
        3 to Partition(30, 9, 11, 3),
        4 to Partition(27, 8, 14, 4),
        5 to Partition(24, 7, 17, 5),
        6 to Partition(20, 6, 21, 6)
    )

    private val graiPartitionTable = mapOf(
        0 to Partition(40, 12, 4, 1),
        1 to Partition(37, 11, 7, 2),
        2 to Partition(34, 10, 10, 3),
        3 to Partition(30, 9, 14, 4),
        4 to Partition(27, 8, 17, 5),
        5 to Partition(24, 7, 20, 6),
        6 to Partition(20, 6, 24, 7)
    )

    fun decode(value: String): Gs1EpcDecoded {
        val raw = value.trim()
        val normalized = normalize(raw)

        if (normalized.isBlank()) {
            return unknown(
                raw = raw,
                normalized = normalized
            )
        }

        if (normalized.startsWith("URN:EPC:")) {
            return decodeEpcUri(
                raw = raw,
                normalized = normalized
            )
        }

        if (!isHex(normalized)) {
            return unknown(
                raw = raw,
                normalized = normalized
            )
        }

        val binary = runCatching {
            hexToBinary(normalized)
        }.getOrNull() ?: return unknown(
            raw = raw,
            normalized = normalized
        )

        if (binary.length < 8) {
            return unknown(
                raw = raw,
                normalized = normalized
            )
        }

        return when (val header = readInt(binary, 0, 8)) {
            0x30 -> decodeSgtin96(raw, normalized, binary)
            0x31 -> decodeSscc96(raw, normalized, binary)
            0x32 -> decodeSgln96(raw, normalized, binary)
            0x33 -> decodeGrai96(raw, normalized, binary)
            0x35 -> decodeGid96(raw, normalized, binary)
            else -> unknown(
                raw = raw,
                normalized = normalized,
                type = "UNKNOWN_HEADER_$header"
            )
        }
    }

    private fun decodeSgtin96(
        raw: String,
        normalized: String,
        binary: String
    ): Gs1EpcDecoded {
        if (binary.length < 96) {
            return unknown(raw, normalized, TYPE_SGTIN_96)
        }

        val partitionValue = readInt(binary, 11, 3)
        val partition = sgtinPartitionTable[partitionValue]
            ?: return unknown(raw, normalized, TYPE_SGTIN_96)

        val companyPrefixStart = 14
        val itemReferenceStart = companyPrefixStart + partition.companyPrefixBits
        val serialStart = itemReferenceStart + partition.referenceBits

        val companyPrefix = readBigInteger(
            binary,
            companyPrefixStart,
            partition.companyPrefixBits
        ).toDecimalString(partition.companyPrefixDigits)

        val itemReference = readBigInteger(
            binary,
            itemReferenceStart,
            partition.referenceBits
        ).toDecimalString(partition.referenceDigits)

        val serial = readBigInteger(
            binary,
            serialStart,
            38
        ).toString()

        val gtin = buildGtin14(
            companyPrefix = companyPrefix,
            itemReference = itemReference
        )

        val key = if (!gtin.isNullOrBlank()) {
            "$TYPE_SGTIN_96|$gtin|$serial"
        } else {
            "$TYPE_SGTIN_96|$companyPrefix|$itemReference|$serial"
        }

        return Gs1EpcDecoded(
            epcRaw = raw,
            epcNormalized = normalized,
            gs1Type = TYPE_SGTIN_96,
            gs1Key = key,
            gtin = gtin,
            serial = serial,
            isRecognized = true
        )
    }

    private fun decodeSscc96(
        raw: String,
        normalized: String,
        binary: String
    ): Gs1EpcDecoded {
        if (binary.length < 96) {
            return unknown(raw, normalized, TYPE_SSCC_96)
        }

        val partitionValue = readInt(binary, 11, 3)
        val partition = ssccPartitionTable[partitionValue]
            ?: return unknown(raw, normalized, TYPE_SSCC_96)

        val companyPrefixStart = 14
        val serialReferenceStart = companyPrefixStart + partition.companyPrefixBits

        val companyPrefix = readBigInteger(
            binary,
            companyPrefixStart,
            partition.companyPrefixBits
        ).toDecimalString(partition.companyPrefixDigits)

        val serialReference = readBigInteger(
            binary,
            serialReferenceStart,
            partition.referenceBits
        ).toDecimalString(partition.referenceDigits)

        val sscc = buildSscc18(
            companyPrefix = companyPrefix,
            serialReference = serialReference
        )

        val key = if (!sscc.isNullOrBlank()) {
            "$TYPE_SSCC_96|$sscc"
        } else {
            "$TYPE_SSCC_96|$companyPrefix|$serialReference"
        }

        return Gs1EpcDecoded(
            epcRaw = raw,
            epcNormalized = normalized,
            gs1Type = TYPE_SSCC_96,
            gs1Key = key,
            gtin = null,
            serial = sscc ?: serialReference,
            isRecognized = true
        )
    }

    private fun decodeSgln96(
        raw: String,
        normalized: String,
        binary: String
    ): Gs1EpcDecoded {
        if (binary.length < 96) {
            return unknown(raw, normalized, TYPE_SGLN_96)
        }

        val partitionValue = readInt(binary, 11, 3)
        val partition = sglnPartitionTable[partitionValue]
            ?: return unknown(raw, normalized, TYPE_SGLN_96)

        val companyPrefixStart = 14
        val locationReferenceStart = companyPrefixStart + partition.companyPrefixBits
        val extensionStart = locationReferenceStart + partition.referenceBits

        val companyPrefix = readBigInteger(
            binary,
            companyPrefixStart,
            partition.companyPrefixBits
        ).toDecimalString(partition.companyPrefixDigits)

        val locationReference = if (partition.referenceDigits > 0) {
            readBigInteger(
                binary,
                locationReferenceStart,
                partition.referenceBits
            ).toDecimalString(partition.referenceDigits)
        } else {
            ""
        }

        val extension = readBigInteger(
            binary,
            extensionStart,
            41
        ).toString()

        return Gs1EpcDecoded(
            epcRaw = raw,
            epcNormalized = normalized,
            gs1Type = TYPE_SGLN_96,
            gs1Key = "$TYPE_SGLN_96|$companyPrefix|$locationReference|$extension",
            gtin = null,
            serial = extension,
            isRecognized = true
        )
    }

    private fun decodeGrai96(
        raw: String,
        normalized: String,
        binary: String
    ): Gs1EpcDecoded {
        if (binary.length < 96) {
            return unknown(raw, normalized, TYPE_GRAI_96)
        }

        val partitionValue = readInt(binary, 11, 3)
        val partition = graiPartitionTable[partitionValue]
            ?: return unknown(raw, normalized, TYPE_GRAI_96)

        val companyPrefixStart = 14
        val assetTypeStart = companyPrefixStart + partition.companyPrefixBits
        val serialStart = assetTypeStart + partition.referenceBits

        val companyPrefix = readBigInteger(
            binary,
            companyPrefixStart,
            partition.companyPrefixBits
        ).toDecimalString(partition.companyPrefixDigits)

        val assetType = readBigInteger(
            binary,
            assetTypeStart,
            partition.referenceBits
        ).toDecimalString(partition.referenceDigits)

        val serial = readBigInteger(
            binary,
            serialStart,
            38
        ).toString()

        return Gs1EpcDecoded(
            epcRaw = raw,
            epcNormalized = normalized,
            gs1Type = TYPE_GRAI_96,
            gs1Key = "$TYPE_GRAI_96|$companyPrefix|$assetType|$serial",
            gtin = null,
            serial = serial,
            isRecognized = true
        )
    }

    private fun decodeGid96(
        raw: String,
        normalized: String,
        binary: String
    ): Gs1EpcDecoded {
        if (binary.length < 96) {
            return unknown(raw, normalized, TYPE_GID_96)
        }

        val manager = readBigInteger(binary, 8, 28).toString()
        val objectClass = readBigInteger(binary, 36, 24).toString()
        val serial = readBigInteger(binary, 60, 36).toString()

        return Gs1EpcDecoded(
            epcRaw = raw,
            epcNormalized = normalized,
            gs1Type = TYPE_GID_96,
            gs1Key = "$TYPE_GID_96|$manager|$objectClass|$serial",
            gtin = null,
            serial = serial,
            isRecognized = true
        )
    }

    private fun decodeEpcUri(
        raw: String,
        normalized: String
    ): Gs1EpcDecoded {
        val type = when {
            normalized.contains(":SGTIN-96:") -> TYPE_SGTIN_96
            normalized.contains(":SSCC-96:") -> TYPE_SSCC_96
            normalized.contains(":SGLN-96:") -> TYPE_SGLN_96
            normalized.contains(":GRAI-96:") -> TYPE_GRAI_96
            normalized.contains(":GID-96:") -> TYPE_GID_96
            else -> TYPE_EPC_URI
        }

        return Gs1EpcDecoded(
            epcRaw = raw,
            epcNormalized = normalized,
            gs1Type = type,
            gs1Key = normalized,
            gtin = null,
            serial = null,
            isRecognized = true
        )
    }

    private fun unknown(
        raw: String,
        normalized: String,
        type: String = TYPE_UNKNOWN
    ): Gs1EpcDecoded {
        val key = normalized.ifBlank {
            raw.trim()
        }

        return Gs1EpcDecoded(
            epcRaw = raw,
            epcNormalized = normalized,
            gs1Type = type,
            gs1Key = key,
            gtin = null,
            serial = null,
            isRecognized = false
        )
    }

    fun normalize(value: String): String {
        return value
            .trim()
            .uppercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
    }

    private fun isHex(value: String): Boolean {
        if (value.isBlank()) return false

        return value.all { char ->
            char in '0'..'9' || char in 'A'..'F'
        }
    }

    private fun hexToBinary(hex: String): String {
        val builder = StringBuilder()

        hex.forEach { char ->
            val value = Character.digit(char, 16)

            if (value < 0) {
                throw IllegalArgumentException("Caracter hexadecimal inválido: $char")
            }

            builder.append(
                value
                    .toString(2)
                    .padStart(4, '0')
            )
        }

        return builder.toString()
    }

    private fun readInt(
        binary: String,
        start: Int,
        length: Int
    ): Int {
        if (start < 0 || length <= 0 || start + length > binary.length) {
            return 0
        }

        return binary.substring(start, start + length).toInt(2)
    }

    private fun readBigInteger(
        binary: String,
        start: Int,
        length: Int
    ): BigInteger {
        if (start < 0 || length <= 0 || start + length > binary.length) {
            return BigInteger.ZERO
        }

        return BigInteger(
            binary.substring(start, start + length),
            2
        )
    }

    private fun BigInteger.toDecimalString(
        digits: Int
    ): String {
        return this.toString().padStart(digits, '0')
    }

    private fun buildGtin14(
        companyPrefix: String,
        itemReference: String
    ): String? {
        if (companyPrefix.isBlank() || itemReference.isBlank()) return null

        val indicator = itemReference.firstOrNull()?.toString() ?: return null
        val itemWithoutIndicator = itemReference.drop(1)

        val body = indicator + companyPrefix + itemWithoutIndicator

        if (body.length != 13) return null
        if (!body.all { it in '0'..'9' }) return null

        val checkDigit = calculateGs1CheckDigit(body)

        return body + checkDigit
    }

    private fun buildSscc18(
        companyPrefix: String,
        serialReference: String
    ): String? {
        if (companyPrefix.isBlank() || serialReference.isBlank()) return null

        val extensionDigit = serialReference.firstOrNull()?.toString() ?: return null
        val serialWithoutExtension = serialReference.drop(1)

        val body = extensionDigit + companyPrefix + serialWithoutExtension

        if (body.length != 17) return null
        if (!body.all { it in '0'..'9' }) return null

        val checkDigit = calculateGs1CheckDigit(body)

        return body + checkDigit
    }

    private fun calculateGs1CheckDigit(
        body: String
    ): Int {
        var sum = 0
        var weight = 3

        for (index in body.length - 1 downTo 0) {
            val digit = Character.digit(body[index], 10)

            if (digit < 0) {
                throw IllegalArgumentException(
                    "Caracter numérico inválido: ${body[index]}"
                )
            }

            sum += digit * weight

            weight = if (weight == 3) {
                1
            } else {
                3
            }
        }

        val mod = sum % 10

        return if (mod == 0) {
            0
        } else {
            10 - mod
        }
    }
}