package com.guardsms.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

enum class MessageStatus { SAFE, FLAGGED, ANALYZING, BLOCKED, PENDING }
enum class ThreatLevel { NONE, LOW, MEDIUM, HIGH, CRITICAL }

@Serializable
data class SmsMessage(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val sender: String = "",
    val body: String = "",
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("is_contact") val isContact: Boolean = false,
    @SerialName("extracted_links") val extractedLinks: List<String> = emptyList(),
    @SerialName("extracted_domains") val extractedDomains: List<String> = emptyList(),
    val status: String = MessageStatus.PENDING.name,
    @SerialName("threat_level") val threatLevel: String = ThreatLevel.NONE.name,
    @SerialName("threat_reason") val threatReason: String? = null,
    @SerialName("is_redflagged") val isRedflagged: Boolean = false,
    @SerialName("received_at") val receivedAt: String = "",
    @SerialName("analyzed_at") val analyzedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class FlaggedDomain(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("domain_hash") val domainHash: String = "",
    val domain: String = "",
    @SerialName("url_hash") val urlHash: String? = null,
    val url: String? = null,
    @SerialName("report_count") val reportCount: Int = 1,
    @SerialName("threat_type") val threatType: String = "",
    val description: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class FlaggedMessage(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("message_hash") val messageHash: String = "",
    @SerialName("raw_preview") val rawPreview: String = "",
    @SerialName("extracted_links") val extractedLinks: List<String> = emptyList(),
    @SerialName("extracted_domains") val extractedDomains: List<String> = emptyList(),
    @SerialName("report_count") val reportCount: Int = 1,
    @SerialName("threat_type") val threatType: String = "",
    val description: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Contact(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    val phone: String = "",
    @SerialName("phone_normalized") val phoneNormalized: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class UserReport(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("report_type") val reportType: String = "",   // domain | url | message | sms
    val content: String = "",
    @SerialName("content_hash") val contentHash: String = "",
    @SerialName("threat_type") val threatType: String = "",
    val description: String? = null,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = ""
)

data class AnalysisResult(
    val status: MessageStatus,
    val threatLevel: ThreatLevel,
    val threatReason: String?,
    val flaggedLinks: List<String>,
    val flaggedDomains: List<String>
)
