package com.wisdomtower.academy.ui.study

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * OFFLINE STATUS (O5a, 2026-10-23, docs/OFFLINE_STATUS_PLAN.md): a counter that goes up by one each time the phone goes from
 * "no working internet" to "working internet" (Android reports the network as VALIDATED). The status screens use the counter
 * as an extra key of their loading effect, so a screen that showed the saved copy or the empty skeleton reads the real
 * numbers again by itself, without the student tapping Retry. No change while the phone stays online (the counter does not
 * move). Uses `ACCESS_NETWORK_STATE`, which `AndroidManifest.xml` already declares (checked 2026-10-23), so the manifest is
 * not touched. Registered once for the whole app process on first use; never throws.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object NetworkReturnSignal {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    private var started = false

    @Volatile
    private var online = false

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            online = cm.activeNetwork
                ?.let { cm.getNetworkCapabilities(it) }
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (validated && !online) {
                        online = true
                        _count.value = _count.value + 1
                    } else if (!validated) {
                        online = false
                    }
                }

                override fun onLost(network: Network) {
                    online = false
                }
            })
        } catch (e: Exception) {
            // no automatic reload on this phone; the Retry buttons still work
        }
    }
}

/** The current value of [NetworkReturnSignal.count]; starts the watcher on first use. Use it as an extra effect key. */
@Composable
fun rememberNetworkReturnCount(): Int {
    val context = LocalContext.current
    remember { NetworkReturnSignal.start(context) }
    val count by NetworkReturnSignal.count.collectAsState()
    return count
}
