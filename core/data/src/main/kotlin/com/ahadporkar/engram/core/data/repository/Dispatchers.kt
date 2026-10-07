package com.ahadporkar.engram.core.data.repository

import javax.inject.Qualifier

/** CPU-bound work (statistics, planning). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Blocking I/O (files, SQLite import). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Process-wide scope for work that must outlive a screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
