package com.imptt.apm29.webrtc

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * SSL全部信任
 * Created by chengshaobo on 2018/10/25.
 */
class TrustAllCerts : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}