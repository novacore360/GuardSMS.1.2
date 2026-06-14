package com.guardsms.data.repository

import android.util.Log
import com.guardsms.data.remote.SupabaseClientProvider
import com.guardsms.domain.model.*
import com.guardsms.utils.HashUtils
import com.guardsms.utils.LinkExtractor
import com.guardsms.utils.SmsAnalyzer
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuardRepository @Inject constructor() {

    private val client get() = SupabaseClientProvider.client
    private val db get() = client.postgrest

    // ─────────────── AUTH ───────────────

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching { client.auth.signOut() }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        client.auth.resetPasswordForEmail(email)
    }

    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    // ─────────────── MESSAGES ───────────────

    suspend fun saveMessage(
        sender: String,
        body: String,
        senderName: String?,
        isContact: Boolean
    ): Result<SmsMessage> = runCatching {
        val userId = requireUserId()
        val links = LinkExtractor.extractLinks(body)
        val domains = LinkExtractor.extractDomains(body)
        val now = Instant.now()
        val expiresAt = now.plus(24, ChronoUnit.HOURS)

        // Get flagged domain hashes for analysis
        val flaggedHashes = getFlaggedDomainHashes()
        val flaggedMsgHashes = getFlaggedMessageHashes()
        val analysis = SmsAnalyzer.analyze(body, flaggedHashes, flaggedMsgHashes)

        val msgData = buildJsonObject {
            put("user_id", userId)
            put("sender", sender)
            put("body", body)
            put("sender_name", senderName)
            put("is_contact", isContact)
            put("extracted_links", buildJsonArray { links.forEach { add(it) } })
            put("extracted_domains", buildJsonArray { domains.forEach { add(it) } })
            put("status", analysis.status.name)
            put("threat_level", analysis.threatLevel.name)
            put("threat_reason", analysis.threatReason)
            put("is_redflagged", analysis.status == MessageStatus.FLAGGED)
            put("received_at", now.toString())
            put("analyzed_at", now.toString())
            put("expires_at", expiresAt.toString())
        }

        val result = db["sms_messages"].insert(msgData) {
            select()
        }.decodeSingle<SmsMessage>()
        result
    }

    suspend fun getMessages(limit: Int = 50): Result<List<SmsMessage>> = runCatching {
        val userId = requireUserId()
        db["sms_messages"].select {
            filter { eq("user_id", userId) }
            order("received_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<SmsMessage>()
    }

    suspend fun getFlaggedMessages(): Result<List<SmsMessage>> = runCatching {
        val userId = requireUserId()
        db["sms_messages"].select {
            filter {
                eq("user_id", userId)
                eq("is_redflagged", true)
            }
            order("received_at", Order.DESCENDING)
        }.decodeList<SmsMessage>()
    }

    suspend fun redFlagMessage(messageId: String): Result<Unit> = runCatching {
        db["sms_messages"].update(buildJsonObject {
            put("is_redflagged", true)
            put("status", MessageStatus.FLAGGED.name)
        }) {
            filter { eq("id", messageId) }
        }
    }

    // ─────────────── FLAGGED DOMAINS ───────────────

    suspend fun reportDomain(
        domain: String,
        threatType: String,
        description: String?
    ): Result<Unit> = runCatching {
        val userId = requireUserId()
        val domainClean = domain.lowercase().removePrefix("www.").trim()
        val hash = HashUtils.hashDomain(domainClean)

        // Upsert - increment count if exists
        val existing = runCatching {
            db["flagged_domains"].select {
                filter { eq("domain_hash", hash) }
                limit(1)
            }.decodeSingleOrNull<FlaggedDomain>()
        }.getOrNull()

        if (existing != null) {
            db["flagged_domains"].update(buildJsonObject {
                put("report_count", existing.reportCount + 1)
                put("updated_at", Instant.now().toString())
            }) {
                filter { eq("domain_hash", hash) }
            }
        } else {
            db["flagged_domains"].insert(buildJsonObject {
                put("user_id", userId)
                put("domain_hash", hash)
                put("domain", domainClean)
                put("threat_type", threatType)
                put("description", description)
                put("report_count", 1)
            })
        }
    }

    suspend fun reportUrl(
        url: String,
        threatType: String,
        description: String?
    ): Result<Unit> = runCatching {
        val userId = requireUserId()
        val domain = HashUtils.extractDomain(url)
        val urlHash = HashUtils.hashUrl(url)
        val domainHash = HashUtils.hashDomain(domain)

        db["flagged_domains"].insert(buildJsonObject {
            put("user_id", userId)
            put("domain_hash", domainHash)
            put("domain", domain)
            put("url_hash", urlHash)
            put("url", url)
            put("threat_type", threatType)
            put("description", description)
        })
    }

    suspend fun getFlaggedDomains(): Result<List<FlaggedDomain>> = runCatching {
        db["flagged_domains"].select {
            order("report_count", Order.DESCENDING)
            limit(200)
        }.decodeList<FlaggedDomain>()
    }

    private suspend fun getFlaggedDomainHashes(): Set<String> = runCatching {
        db["flagged_domains"].select(Columns.list("domain_hash"))
            .decodeList<JsonObject>()
            .mapNotNull { it["domain_hash"]?.jsonPrimitive?.content }
            .toSet()
    }.getOrDefault(emptySet())

    private suspend fun getFlaggedMessageHashes(): Set<String> = runCatching {
        db["flagged_messages"].select(Columns.list("message_hash"))
            .decodeList<JsonObject>()
            .mapNotNull { it["message_hash"]?.jsonPrimitive?.content }
            .toSet()
    }.getOrDefault(emptySet())

    // ─────────────── FLAGGED MESSAGES (community) ───────────────

    suspend fun reportRawMessage(
        rawMessage: String,
        threatType: String,
        description: String?
    ): Result<Unit> = runCatching {
        val userId = requireUserId()
        val hash = HashUtils.hashMessage(rawMessage)
        val links = LinkExtractor.extractLinks(rawMessage)
        val domains = LinkExtractor.extractDomains(rawMessage)
        val preview = rawMessage.take(120)

        val existing = runCatching {
            db["flagged_messages"].select {
                filter { eq("message_hash", hash) }
                limit(1)
            }.decodeSingleOrNull<FlaggedMessage>()
        }.getOrNull()

        if (existing != null) {
            db["flagged_messages"].update(buildJsonObject {
                put("report_count", existing.reportCount + 1)
            }) {
                filter { eq("message_hash", hash) }
            }
        } else {
            db["flagged_messages"].insert(buildJsonObject {
                put("user_id", userId)
                put("message_hash", hash)
                put("raw_preview", preview)
                put("extracted_links", buildJsonArray { links.forEach { add(it) } })
                put("extracted_domains", buildJsonArray { domains.forEach { add(it) } })
                put("threat_type", threatType)
                put("description", description)
            })
        }
    }

    // ─────────────── CONTACTS ───────────────

    suspend fun syncContacts(contacts: List<Contact>): Result<Unit> = runCatching {
        val userId = requireUserId()
        if (contacts.isEmpty()) return@runCatching

        // Delete existing contacts for this user first
        db["contacts"].delete {
            filter { eq("user_id", userId) }
        }

        // Batch insert
        val chunks = contacts.chunked(100)
        for (chunk in chunks) {
            val arr = buildJsonArray {
                chunk.forEach { contact ->
                    add(buildJsonObject {
                        put("user_id", userId)
                        put("name", contact.name)
                        put("phone", contact.phone)
                        put("phone_normalized", contact.phoneNormalized)
                    })
                }
            }
            db["contacts"].insert(arr)
        }
    }

    suspend fun getContacts(): Result<List<Contact>> = runCatching {
        val userId = requireUserId()
        db["contacts"].select {
            filter { eq("user_id", userId) }
            order("name", Order.ASCENDING)
        }.decodeList<Contact>()
    }

    suspend fun isContact(phone: String): Boolean = runCatching {
        val userId = requireUserId()
        val normalized = normalizePhone(phone)
        db["contacts"].select {
            filter {
                eq("user_id", userId)
                eq("phone_normalized", normalized)
            }
            limit(1)
        }.decodeList<Contact>().isNotEmpty()
    }.getOrDefault(false)

    // ─────────────── USER REPORTS ───────────────

    suspend fun getUserReports(): Result<List<UserReport>> = runCatching {
        val userId = requireUserId()
        db["user_reports"].select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<UserReport>()
    }

    suspend fun submitReport(
        reportType: String,
        content: String,
        threatType: String,
        description: String?
    ): Result<Unit> = runCatching {
        val userId = requireUserId()
        val hash = HashUtils.sha256(content)
        db["user_reports"].insert(buildJsonObject {
            put("user_id", userId)
            put("report_type", reportType)
            put("content", content)
            put("content_hash", hash)
            put("threat_type", threatType)
            put("description", description)
        })
    }

    // ─────────────── CLEANUP ───────────────

    suspend fun deleteExpiredMessages(): Result<Unit> = runCatching {
        val userId = requireUserId()
        db["sms_messages"].delete {
            filter {
                eq("user_id", userId)
                lt("expires_at", Instant.now().toString())
            }
        }
    }

    // ─────────────── HELPERS ───────────────

    private fun requireUserId(): String =
        client.auth.currentUserOrNull()?.id ?: throw IllegalStateException("User not authenticated")

    fun normalizePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("63") -> "+$digits"
            digits.startsWith("0") -> "+63${digits.drop(1)}"
            digits.length == 10 -> "+63$digits"
            else -> "+$digits"
        }
    }
}
