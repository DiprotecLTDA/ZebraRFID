package com.diprotec.inventariozebratc27.core.network

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

class NetworkUsageCallFactory(
    private val client: OkHttpClient
) : Call.Factory {

    override fun newCall(request: Request): Call {
        val tag = NetworkUsageContext.currentTag()

        val taggedRequest = if (tag != null) {
            request.newBuilder()
                .tag(NetworkUsageTag::class.java, tag)
                .build()
        } else {
            request
        }

        return client.newCall(taggedRequest)
    }
}