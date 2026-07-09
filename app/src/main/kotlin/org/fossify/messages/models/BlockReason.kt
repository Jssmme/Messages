package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records why a specific blocked message was blocked at the time of interception.
 *
 * Stored in a separate table (not inside [BlockedMessage]) because the vast majority
 * of normal messages never need this data.  One blocked message can have multiple
 * rows here — e.g. when both the sender number and a body keyword triggered the block.
 *
 * @property id          Auto-generated primary key.
 * @property messageId   The id of the blocked message (matches [BlockedMessage.id]).
 * @property threadId    The thread id of the blocked message (for efficient bulk cleanup).
 * @property ruleType    "NUMBER" or "KEYWORD".
 * @property matchedText The actual text fragment that was matched in the SMS body
 *                       (for KEYWORD) or the sender phone number (for NUMBER).
 * @property matchStart  Start character offset of the match within the SMS body.
 *                       Null for NUMBER-type matches (not used for body highlighting).
 * @property matchEnd    End character offset (exclusive) of the match within the SMS body.
 *                       Null for NUMBER-type matches.
 */
@Entity(
    tableName = "block_reasons",
    indices = [
        Index(value = ["message_id"]),
        Index(value = ["thread_id"])
    ]
)
data class BlockReason(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "message_id") val messageId: Long,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "rule_type") val ruleType: String,
    @ColumnInfo(name = "matched_text") val matchedText: String,
    @ColumnInfo(name = "match_start") val matchStart: Int? = null,
    @ColumnInfo(name = "match_end") val matchEnd: Int? = null
) {
    companion object {
        const val TYPE_NUMBER = "NUMBER"
        const val TYPE_KEYWORD = "KEYWORD"
    }
}
