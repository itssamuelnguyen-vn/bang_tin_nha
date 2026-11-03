package com.samuelcruz.bangtinnha

/** Reads a saved real response from src/test/resources. */
fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()
