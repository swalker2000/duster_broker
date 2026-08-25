package com.duster.duster_protocol.transport.ssl

import com.duster.transport.mqtt.config.TrustAllTrustManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.InputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

object DusterProtocolSsl {

    const val DEFAULT_KEY_ALIAS = "duster"
    const val DEFAULT_KEYSTORE_PASSWORD = "changeit"

    fun selfSignedServerContext(commonName: String = "localhost"): SSLContext {
        val password = DEFAULT_KEYSTORE_PASSWORD.toCharArray()
        return serverContext(selfSignedKeyStore(commonName, password), password)
    }

    fun selfSignedKeyStore(
        commonName: String = "localhost",
        password: CharArray = DEFAULT_KEYSTORE_PASSWORD.toCharArray()
    ): KeyStore {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val now = Instant.now()
        val holder = X500Name("CN=$commonName")
        val certHolder = JcaX509v3CertificateBuilder(
            holder,
            BigInteger.valueOf(now.toEpochMilli()),
            Date.from(now.minus(1, ChronoUnit.DAYS)),
            Date.from(now.plus(3650, ChronoUnit.DAYS)),
            holder,
            keyPair.public
        ).build(JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private))
        val certificate: X509Certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, password)
        keyStore.setKeyEntry(DEFAULT_KEY_ALIAS, keyPair.private, password, arrayOf(certificate))
        return keyStore
    }

    fun serverContext(keyStoreStream: InputStream, password: String): SSLContext {
        val pwd = password.ifBlank { DEFAULT_KEYSTORE_PASSWORD }.toCharArray()
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStoreStream.use { keyStore.load(it, pwd) }
        return serverContext(keyStore, pwd)
    }

    fun serverContext(keyStore: KeyStore, password: CharArray): SSLContext {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, password)
        val context = SSLContext.getInstance("TLS")
        context.init(kmf.keyManagers, null, SecureRandom())
        return context
    }

    fun trustingServer(serverKeyStore: KeyStore): SSLContext {
        val certificate = serverKeyStore.getCertificate(DEFAULT_KEY_ALIAS)
        val trustStore = KeyStore.getInstance("PKCS12")
        trustStore.load(null, DEFAULT_KEYSTORE_PASSWORD.toCharArray())
        trustStore.setCertificateEntry(DEFAULT_KEY_ALIAS, certificate)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)
        val context = SSLContext.getInstance("TLS")
        context.init(null, tmf.trustManagers, SecureRandom())
        return context
    }

    fun insecureClientSocketFactory(): SSLSocketFactory {
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf(TrustAllTrustManager), SecureRandom())
        return context.socketFactory
    }
}
