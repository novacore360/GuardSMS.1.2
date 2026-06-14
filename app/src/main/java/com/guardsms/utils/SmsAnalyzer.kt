package com.guardsms.utils

import com.guardsms.domain.model.AnalysisResult
import com.guardsms.domain.model.MessageStatus
import com.guardsms.domain.model.ThreatLevel

object SmsAnalyzer {

    // Known phishing/malware TLDs and suspicious patterns
    private val suspiciousTlds = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".pw", ".top", ".xyz", ".click",
        ".download", ".loan", ".review", ".win", ".bid", ".stream"
    )

    private val phishingKeywords = listOf(
        "verify your account", "confirm your identity", "account suspended",
        "click here to claim", "you have won", "congratulations winner",
        "otp expired", "update your kyc", "unusual activity detected",
        "your account will be closed", "action required", "limited time offer",
        "free gift", "claim your prize", "bank details", "credit card details",
        "password reset", "login attempt", "unauthorized access", "gcash account",
        "paymaya account", "bdo online", "bpi online", "metrobank alert",
        "pnb online", "landbank alert", "peso reward", "load reward",
        "promo expires", "last chance", "final notice", "urgent action"
    )

    private val scamPatterns = listOf(
        Regex("""(?i)(win|won|winner|prize).{0,30}(claim|collect|redeem)"""),
        Regex("""(?i)(send|transfer).{0,30}(money|peso|php|load)"""),
        Regex("""(?i)(otp|one.time.password).{0,30}(share|send|give)"""),
        Regex("""(?i)(account|password).{0,30}(verify|confirm|update)"""),
        Regex("""(?i)(\d{6,8}).{0,10}(otp|code|pin)"""),
        Regex("""(?i)(gcash|paymaya|palawan|cebuana).{0,50}(click|tap|go to|visit)"""),
        Regex("""(?i)(?:https?://)?(?:www\.)?(?:[a-z0-9-]+\.){2,}[a-z]{2,}/[a-z0-9?=&%-]{20,}"""),
    )

    fun analyze(
        message: String,
        flaggedDomainHashes: Set<String>,
        flaggedMessageHashes: Set<String>
    ): AnalysisResult {
        val links = LinkExtractor.extractLinks(message)
        val domains = LinkExtractor.extractDomains(message)

        val flaggedLinks = mutableListOf<String>()
        val flaggedDomainsList = mutableListOf<String>()
        val reasons = mutableListOf<String>()

        // Check against flagged domains in DB
        for (domain in domains) {
            val hash = HashUtils.hashDomain(domain)
            if (flaggedDomainHashes.contains(hash)) {
                flaggedDomainsList.add(domain)
                reasons.add("Domain '$domain' is in the flagged list")
            }
        }

        // Check message hash
        val msgHash = HashUtils.hashMessage(message)
        if (flaggedMessageHashes.contains(msgHash)) {
            reasons.add("This exact message has been reported as fraudulent")
        }

        // Check suspicious TLDs
        for (domain in domains) {
            suspiciousTlds.forEach { tld ->
                if (domain.endsWith(tld)) {
                    flaggedDomainsList.add(domain)
                    reasons.add("Suspicious TLD detected: $domain")
                }
            }
        }

        // Check URL shorteners
        for (link in links) {
            if (LinkExtractor.isShortUrl(link)) {
                flaggedLinks.add(link)
                reasons.add("URL shortener detected (may hide malicious links): $link")
            }
        }

        // Keyword analysis
        val lowerMsg = message.lowercase()
        for (keyword in phishingKeywords) {
            if (lowerMsg.contains(keyword)) {
                reasons.add("Suspicious phrase detected: \"$keyword\"")
                break
            }
        }

        // Pattern matching
        for (pattern in scamPatterns) {
            if (pattern.containsMatchIn(message)) {
                reasons.add("Scam pattern detected in message content")
                break
            }
        }

        // Determine threat level
        val threatLevel = when {
            flaggedDomainsList.isNotEmpty() && reasons.size >= 2 -> ThreatLevel.CRITICAL
            flaggedDomainsList.isNotEmpty() || flaggedMessageHashes.contains(msgHash) -> ThreatLevel.HIGH
            reasons.size >= 3 -> ThreatLevel.HIGH
            reasons.size == 2 -> ThreatLevel.MEDIUM
            reasons.size == 1 -> ThreatLevel.LOW
            else -> ThreatLevel.NONE
        }

        val status = when (threatLevel) {
            ThreatLevel.CRITICAL, ThreatLevel.HIGH -> MessageStatus.FLAGGED
            ThreatLevel.MEDIUM -> MessageStatus.FLAGGED
            ThreatLevel.LOW -> MessageStatus.FLAGGED
            ThreatLevel.NONE -> MessageStatus.SAFE
        }

        return AnalysisResult(
            status = status,
            threatLevel = threatLevel,
            threatReason = if (reasons.isEmpty()) null else reasons.joinToString("; "),
            flaggedLinks = flaggedLinks,
            flaggedDomains = flaggedDomainsList
        )
    }
}
