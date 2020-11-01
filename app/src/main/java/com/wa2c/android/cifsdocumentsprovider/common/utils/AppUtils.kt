package com.wa2c.android.cifsdocumentsprovider.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Do action if null or empty
 */
fun <C, R> C?.ifNullOrEmpty(defaultValue: () -> R?): R?
        where R : CharSequence?, C : R?
        = if (isNullOrEmpty()) defaultValue() else this

/**
 * Renew collection elements.
 */
fun <E, T : MutableCollection<E>> T.renew(v: Collection<E>): T {
    this.clear()
    this.addAll(v)
    return this
}

/**
 * Renew map elements.
 */
fun <K, V, T : MutableMap<K, V>> T.renew(m: Map<K,V>): T {
    this.clear()
    this.putAll(m)
    return this
}


class MainCoroutineScope: CoroutineScope {
    val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
}
