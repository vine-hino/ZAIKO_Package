package com.vine.connector_api

interface ConnectionSelector {
    fun current(): ConnectionType
    fun change(type: ConnectionType)
}