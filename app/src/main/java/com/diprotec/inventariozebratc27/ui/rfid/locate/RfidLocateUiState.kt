package com.diprotec.inventariozebratc27.ui.rfid.locate

data class RfidLocateUiState(
    val searchInput: String = "",
    val locating: Boolean = false,
    val connecting: Boolean = false,
    val searchingProduct: Boolean = false,
    val checkingReader: Boolean = false,

    val relativeDistance: Int = 0,
    val locateReadCount: Int = 0,
    val lastLocatedEpc: String? = null,

    val message: String = "Ingrese código de producto, código secundario o descripción.",
    val error: String? = null,

    val options: List<RfidProductSearchOption> = emptyList(),
    val totalOptionsFound: Int = 0,
    val tooManyOptions: Boolean = false,

    val selectedOption: RfidProductSearchOption? = null,
    val generatedEpcToLocate: String? = null
)