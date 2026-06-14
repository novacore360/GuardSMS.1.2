package com.guardsms.utils

import java.net.URL
import java.security.MessageDigest

object HashUtils {

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashDomain(domain: String): String = sha256(domain.lowercase().trim())

    fun hashUrl(url: String): String = sha256(normalizeUrl(url))

    fun hashMessage(message: String): String = sha256(message.trim())

    fun normalizeUrl(url: String): String {
        return try {
            val u = if (url.startsWith("http")) url else "https://$url"
            URL(u).let { "${it.protocol}://${it.host}${it.path}".lowercase() }
        } catch (e: Exception) {
            url.lowercase().trim()
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val u = if (url.startsWith("http")) url else "https://$url"
            URL(u).host.removePrefix("www.").lowercase()
        } catch (e: Exception) {
            url.removePrefix("www.").removePrefix("http://").removePrefix("https://")
                .split("/").first().split("?").first().lowercase()
        }
    }
}

object LinkExtractor {

    private val urlRegex = Regex(
        """(?:https?://|www\.)[a-zA-Z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+""",
        RegexOption.IGNORE_CASE
    )

    private val domainRegex = Regex(
        """(?:^|\s)((?:[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,})""",
        RegexOption.IGNORE_CASE
    )

    private val shortenerDomains = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly",
        "buff.ly", "dlvr.it", "short.link", "rebrand.ly", "cutt.ly"
    )

    fun extractLinks(text: String): List<String> {
        return urlRegex.findAll(text).map { it.value }.distinct().toList()
    }

    fun extractDomains(text: String): List<String> {
        val fromUrls = extractLinks(text).map { HashUtils.extractDomain(it) }
        val fromText = domainRegex.findAll(text).map { it.groupValues[1].lowercase() }
        return (fromUrls + fromText).distinct().filter { it.isNotBlank() }
    }

    fun isShortUrl(url: String): Boolean {
        val domain = HashUtils.extractDomain(url)
        return shortenerDomains.contains(domain)
    }
}
