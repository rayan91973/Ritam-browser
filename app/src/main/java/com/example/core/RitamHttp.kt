package com.example.core

import okhttp3.OkHttpClient

/**
 * Process-wide OkHttp baseline.
 *
 * Every feature used to construct its own [OkHttpClient], which allocates a separate connection
 * pool and dispatcher (and therefore a separate thread pool) per helper. Clients derived through
 * [builder] share the pool and dispatcher while still tuning timeouts locally, which keeps socket
 * reuse across features and bounds the number of idle threads.
 *
 * The DoH resolver intentionally keeps its own client: DNS traffic is a privacy boundary and
 * should not share connections with content requests.
 */
object RitamHttp {

    val base: OkHttpClient by lazy { OkHttpClient.Builder().build() }

    /** A client that shares the base connection pool and dispatcher. */
    fun builder(): OkHttpClient.Builder = base.newBuilder()
}
