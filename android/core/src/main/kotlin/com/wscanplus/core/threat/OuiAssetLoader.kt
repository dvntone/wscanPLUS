package com.wscanplus.core.threat

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object OuiAssetLoader {
    fun load(context: Context): OuiLookup {
        val map: MutableMap<String, String> = mutableMapOf()
        context.assets.open("oui.csv").use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader
                    .lineSequence()
                    .drop(1)
                    .forEach { line ->
                        val parts = line.split(",", limit = 4)
                        if (parts.size < 3) return@forEach
                        val oui = parts[1].trim().uppercase()
                        val vendor = parts[2].trim().trim('"')
                        if (oui.length == 6 && vendor.isNotEmpty()) {
                            map[oui] = vendor
                        }
                    }
            }
        }
        return OuiLookup(map)
    }
}
