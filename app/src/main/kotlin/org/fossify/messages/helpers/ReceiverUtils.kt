package org.fossify.messages.helpers

import android.content.Context
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.messages.extensions.blockReasonsDB
import org.fossify.messages.extensions.config
import org.fossify.messages.models.BlockReason

object ReceiverUtils {

    /**
     * Lightweight value describing a single keyword hit inside the SMS body.
     */
    data class KeywordMatch(
        val keyword: String,
        val matchedText: String,
        val start: Int,
        val end: Int
    )

    fun isMessageFilteredOut(context: Context, body: String, address: String? = null): Boolean {
        // Contacts are always exempt from keyword filtering - bypass if sender is a contact
        if (address != null) {
            val privateCursor = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
            val result = SimpleContactsHelper(context).existsSync(address, privateCursor)
            if (result != ContactLookupResult.NotFound) {
                return false
            }
        }

        // Check whitelist keywords first - if matched, bypass blocked keywords
        for (whitelistKeyword in context.config.whitelistKeywords) {
            if (body.contains(whitelistKeyword, ignoreCase = true)) {
                return false
            }
        }

        for (blockedKeyword in context.config.blockedKeywords) {
            if (body.contains(blockedKeyword, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    /**
     * Returns **all** keyword matches found in [body] against the configured blocked-keyword set.
     *
     * Each match records the actual text fragment that appeared in the body (which may differ
     * from the rule text when the rule is a regex or when case-insensitive matching is used),
     * plus the character offsets for later highlighting.
     *
     * Whitelist and contact-exemption logic is applied first, mirroring [isMessageFilteredOut].
     * If the sender is a contact or a whitelist keyword matches, an empty list is returned.
     */
    fun getKeywordMatches(context: Context, body: String, address: String? = null): List<KeywordMatch> {
        // Contacts are always exempt from keyword filtering
        if (address != null) {
            val privateCursor = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
            val result = SimpleContactsHelper(context).existsSync(address, privateCursor)
            if (result != ContactLookupResult.NotFound) {
                return emptyList()
            }
        }

        // Check whitelist keywords first - if matched, bypass blocked keywords
        for (whitelistKeyword in context.config.whitelistKeywords) {
            if (body.contains(whitelistKeyword, ignoreCase = true)) {
                return emptyList()
            }
        }

        val matches = mutableListOf<KeywordMatch>()
        for (blockedKeyword in context.config.blockedKeywords) {
            findMatches(body, blockedKeyword).forEach { match ->
                matches.add(
                    KeywordMatch(
                        keyword = blockedKeyword,
                        matchedText = match.first,
                        start = match.second,
                        end = match.third
                    )
                )
            }
        }
        return matches
    }

    /**
     * Finds all (possibly overlapping) occurrences of [keyword] in [body] using case-insensitive
     * matching.  Returns a list of (matchedText, start, end) triples.
     */
    private fun findMatches(body: String, keyword: String): List<Triple<String, Int, Int>> {
        if (keyword.isEmpty()) return emptyList()
        val results = mutableListOf<Triple<String, Int, Int>>()
        var searchFrom = 0
        while (true) {
            val idx = body.indexOf(keyword, startIndex = searchFrom, ignoreCase = true)
            if (idx < 0) break
            val end = idx + keyword.length
            results.add(Triple(body.substring(idx, end), idx, end))
            searchFrom = idx + 1 // allow overlapping matches
        }
        return results
    }

    /**
     * Converts a [KeywordMatch] into a [BlockReason] row ready for insertion.
     */
    fun KeywordMatch.toBlockReason(messageId: Long, threadId: Long): BlockReason {
        return BlockReason(
            messageId = messageId,
            threadId = threadId,
            ruleType = BlockReason.TYPE_KEYWORD,
            matchedText = matchedText,
            matchStart = start,
            matchEnd = end
        )
    }

    /**
     * Collects and persists all block reasons for a single blocked message.
     *
     * Keyword hits are extracted from [body]; a single NUMBER reason is added when
     * either [isBlockedNumber] or [isBlockedUnknown] is true (both conditions produce
     * the same reason, so only one row is inserted to avoid duplicates).
     */
    fun recordBlockReasons(
        context: Context,
        messageId: Long,
        threadId: Long,
        address: String,
        body: String,
        isBlockedNumber: Boolean,
        isBlockedUnknown: Boolean
    ) {
        val keywordMatches = getKeywordMatches(context, body, address)
        val reasons = mutableListOf<BlockReason>()
        keywordMatches.forEach { match ->
            reasons.add(match.toBlockReason(messageId, threadId))
        }
        if (isBlockedNumber || isBlockedUnknown) {
            reasons.add(
                BlockReason(
                    messageId = messageId,
                    threadId = threadId,
                    ruleType = BlockReason.TYPE_NUMBER,
                    matchedText = address,
                    matchStart = null,
                    matchEnd = null
                )
            )
        }
        if (reasons.isNotEmpty()) {
            try {
                context.blockReasonsDB.insertAll(reasons)
            } catch (e: Exception) {
                // Best-effort: don't let reason-recording failure prevent the block itself.
            }
        }
    }
}
