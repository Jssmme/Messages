package org.fossify.messages.receivers

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import com.klinker.android.send_message.MmsReceivedReceiver
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.getConversations
import org.fossify.messages.extensions.getLatestMMS
import org.fossify.messages.extensions.getNameFromAddress
import org.fossify.messages.extensions.insertOrUpdateConversation
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.extensions.moveMessageToBlocked
import org.fossify.messages.extensions.shouldUnarchive
import org.fossify.messages.extensions.showReceivedMessageNotification
import org.fossify.messages.extensions.updateConversationArchivedStatus
import org.fossify.messages.helpers.ReceiverUtils.isMessageFilteredOut
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Message

class MmsReceiver : MmsReceivedReceiver() {

    private var lastSenderAddress: String = ""

    override fun isAddressBlocked(context: Context, address: String): Boolean {
        lastSenderAddress = address
        return false
    }

    override fun isContentBlocked(context: Context, content: String): Boolean {
        return false
    }

    override fun onMessageReceived(context: Context, messageUri: Uri) {
        val mms = context.getLatestMMS() ?: return
        val address = mms.getSender()?.phoneNumbers?.firstOrNull()?.normalizedNumber ?: ""
        val size = context.resources.getDimension(R.dimen.notification_large_icon_size).toInt()
        ensureBackgroundThread {
            val isFilteredByKeyword = isMessageFilteredOut(context, mms.body, address)
            val isBlockedNumber = context.isNumberBlocked(address)
            val isBlockedUnknown = context.baseConfig.blockUnknownNumbers && run {
                val privateCursor = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
                val result = SimpleContactsHelper(context).existsSync(address, privateCursor)
                result == ContactLookupResult.NotFound
            }

            if (isFilteredByKeyword || isBlockedNumber || isBlockedUnknown) {
                handleBlockedMmsMessage(context, mms, address)
            } else {
                handleMmsMessage(context, mms, size, address)
            }
        }
    }

    override fun onError(context: Context, error: String) {
        context.showErrorToast(context.getString(R.string.couldnt_download_mms))
    }

    private fun handleMmsMessage(
        context: Context,
        mms: Message,
        size: Int,
        address: String
    ) {
        val glideBitmap = try {
            Glide.with(context)
                .asBitmap()
                .load(mms.attachment!!.attachments.first().getUri())
                .centerCrop()
                .into(size, size)
                .get()
        } catch (e: Exception) {
            null
        }


        val senderName = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            context.getNameFromAddress(address, it)
        }

        context.showReceivedMessageNotification(
            messageId = mms.id,
            address = address,
            senderName = senderName,
            body = mms.body,
            threadId = mms.threadId,
            bitmap = glideBitmap
        )

        val conversation = context.getConversations(mms.threadId).firstOrNull() ?: return
        runCatching { context.insertOrUpdateConversation(conversation) }
        if (context.shouldUnarchive()) {
            context.updateConversationArchivedStatus(mms.threadId, false)
        }
        refreshMessages()
        refreshConversations()
    }

    private fun handleBlockedMmsMessage(
        context: Context,
        mms: Message,
        address: String
    ) {
        val senderName = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            context.getNameFromAddress(address, it)
        }

        val photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(address)

        // Mark the MMS as read in the system DB so the system thread doesn't show
        // a false unread indicator for a blocked message.
        runCatching {
            val values = android.content.ContentValues().apply {
                put(android.provider.Telephony.Mms.READ, 1)
            }
            context.contentResolver.update(
                android.net.Uri.parse("content://mms/${mms.id}"),
                values,
                null,
                null
            )
        }

        val readMms = mms.copy(read = true)
        context.messagesDB.insertOrUpdate(readMms)
        context.moveMessageToBlocked(mms.id)

        // Only create the conversation in our DB if it doesn't already exist.
        // Don't update an existing conversation — the blocked message should not
        // change the snippet, date, or read status shown in the normal conversation list.
        val existingConv = context.conversationsDB.getConversationWithThreadId(mms.threadId)
        if (existingConv == null) {
            val conversation = context.getConversations(mms.threadId).firstOrNull()
            if (conversation != null) {
                val readConv = conversation.copy(read = true, unreadCount = 0)
                runCatching { context.conversationsDB.insertOrUpdate(readConv) }
            } else {
                val newConversation = Conversation(
                    threadId = mms.threadId,
                    snippet = mms.body,
                    date = mms.date,
                    read = true,
                    title = senderName,
                    photoUri = photoUri,
                    isGroupConversation = false,
                    phoneNumber = address,
                    isArchived = false,
                    unreadCount = 0
                )
                runCatching { context.conversationsDB.insertOrUpdate(newConversation) }
            }
        }

        refreshConversations()
    }
}
