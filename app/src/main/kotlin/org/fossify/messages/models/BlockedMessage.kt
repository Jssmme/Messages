package org.fossify.messages.models

import android.provider.Telephony
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.helpers.THREAD_RECEIVED_MESSAGE
import org.fossify.messages.helpers.THREAD_SENT_MESSAGE
import org.fossify.messages.helpers.generateStableId

@Entity(
    tableName = "blocked_messages",
    indices = [(Index(value = ["id"], unique = true))]
)
data class BlockedMessage(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "participants") val participants: ArrayList<SimpleContact>,
    @ColumnInfo(name = "date") val date: Int,
    @ColumnInfo(name = "read") val read: Boolean,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "is_mms") val isMMS: Boolean,
    @ColumnInfo(name = "attachment") val attachment: MessageAttachment?,
    @ColumnInfo(name = "sender_phone_number") val senderPhoneNumber: String,
    @ColumnInfo(name = "sender_name") var senderName: String,
    @ColumnInfo(name = "sender_photo_uri") val senderPhotoUri: String,
    @ColumnInfo(name = "subscription_id") var subscriptionId: Int,
    @ColumnInfo(name = "is_scheduled") var isScheduled: Boolean = false,
    @ColumnInfo(name = "blocked_ts") var blockedTS: Long
) {
    fun isReceivedMessage() = type == Telephony.Sms.MESSAGE_TYPE_INBOX

    fun millis() = date * 1000L

    fun getSender(): SimpleContact? =
        participants.firstOrNull { it.doesHavePhoneNumber(senderPhoneNumber) }
            ?: participants.firstOrNull { it.name == senderName }
            ?: participants.firstOrNull()

    fun getStableId(): Long {
        val providerBit = if (isMMS) 1L else 0L
        val key = (id shl 1) or providerBit
        val type = if (isReceivedMessage()) THREAD_RECEIVED_MESSAGE else THREAD_SENT_MESSAGE
        return generateStableId(type, key)
    }

    fun getSelectionKey(): Int {
        return (id xor (id ushr Int.SIZE_BITS)).toInt()
    }

    fun toMessage(): Message {
        return Message(
            id = id,
            body = body,
            type = type,
            status = status,
            participants = participants,
            date = date,
            read = read,
            threadId = threadId,
            isMMS = isMMS,
            attachment = attachment,
            senderPhoneNumber = senderPhoneNumber,
            senderName = senderName,
            senderPhotoUri = senderPhotoUri,
            subscriptionId = subscriptionId,
            isScheduled = isScheduled
        )
    }

    companion object {
        fun fromMessage(message: Message, blockedTS: Long): BlockedMessage {
            return BlockedMessage(
                id = message.id,
                body = message.body,
                type = message.type,
                status = message.status,
                participants = message.participants,
                date = message.date,
                read = message.read,
                threadId = message.threadId,
                isMMS = message.isMMS,
                attachment = message.attachment,
                senderPhoneNumber = message.senderPhoneNumber,
                senderName = message.senderName,
                senderPhotoUri = message.senderPhotoUri,
                subscriptionId = message.subscriptionId,
                isScheduled = message.isScheduled,
                blockedTS = blockedTS
            )
        }

        fun areItemsTheSame(old: BlockedMessage, new: BlockedMessage): Boolean {
            return old.id == new.id
        }

        fun areContentsTheSame(old: BlockedMessage, new: BlockedMessage): Boolean {
            return old.body == new.body &&
                old.threadId == new.threadId &&
                old.date == new.date &&
                old.isMMS == new.isMMS &&
                old.attachment == new.attachment &&
                old.senderPhoneNumber == new.senderPhoneNumber &&
                old.senderName == new.senderName &&
                old.senderPhotoUri == new.senderPhotoUri &&
                old.isScheduled == new.isScheduled
        }
    }
}
