package com.vine.database.model

enum class TerminalType {
    PC,
    HT,
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    ERROR,
}

enum class OperationType {
    INBOUND,
    OUTBOUND,
    MOVE_OUT,
    MOVE_IN,
    STOCKTAKE,
    ADJUST,
    CANCEL,
}

enum class StocktakeStatus {
    DRAFT,
    CONFIRMED,
}

enum class QuantitySignType {
    PLUS,
    MINUS,
    BOTH,
}

enum class BarcodeType {
    JAN,
    ITF,
    CODE128,
    QR,
    OTHER,
}