package org.fossify.messages.models

import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.SimpleContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockedMessageTest {

    private fun createSimpleContact(name: String, number: String): SimpleContact {
        return SimpleContact(
            rawId = 0,
            contactId = 0,
            name = name,
            photoUri = "",
            phoneNumbers = arrayListOf(PhoneNumber(number, 0, "", number)),
            birthdays = ArrayList(),
            anniversaries = ArrayList()
        )
    }

    private fun createMessage(
        id: Long = 1L,
        body: String = "test body",
        type: Int = 1, // Telephony.Sms.MESSAGE_TYPE_INBOX
        status: Int = -1,
        date: Int = 1700000000,
        read: Boolean = true,
        threadId: Long = 100L,
        isMMS: Boolean = false,
        senderPhoneNumber: String = "1234567890",
        senderName: String = "Sender",
        senderPhotoUri: String = "",
        subscriptionId: Int = -1,
        isScheduled: Boolean = false
    ): Message {
        return Message(
            id = id,
            body = body,
            type = type,
            status = status,
            participants = arrayListOf(createSimpleContact(senderName, senderPhoneNumber)),
            date = date,
            read = read,
            threadId = threadId,
            isMMS = isMMS,
            attachment = null,
            senderPhoneNumber = senderPhoneNumber,
            senderName = senderName,
            senderPhotoUri = senderPhotoUri,
            subscriptionId = subscriptionId,
            isScheduled = isScheduled
        )
    }

    @Test
    fun `fromMessage copies all fields from Message`() {
        val message = createMessage(
            id = 42L,
            body = "hello world",
            type = 2,
            status = 0,
            date = 1699000000,
            read = false,
            threadId = 200L,
            isMMS = true,
            senderPhoneNumber = "0987654321",
            senderName = "Alice",
            senderPhotoUri = "content://photo/1",
            subscriptionId = 1,
            isScheduled = true
        )
        val blockedTS = 1700000000123L

        val blocked = BlockedMessage.fromMessage(message, blockedTS)

        assertEquals(message.id, blocked.id)
        assertEquals(message.body, blocked.body)
        assertEquals(message.type, blocked.type)
        assertEquals(message.status, blocked.status)
        assertEquals(message.participants, blocked.participants)
        assertEquals(message.date, blocked.date)
        assertEquals(message.read, blocked.read)
        assertEquals(message.threadId, blocked.threadId)
        assertEquals(message.isMMS, blocked.isMMS)
        assertEquals(message.attachment, blocked.attachment)
        assertEquals(message.senderPhoneNumber, blocked.senderPhoneNumber)
        assertEquals(message.senderName, blocked.senderName)
        assertEquals(message.senderPhotoUri, blocked.senderPhotoUri)
        assertEquals(message.subscriptionId, blocked.subscriptionId)
        assertEquals(message.isScheduled, blocked.isScheduled)
        assertEquals(blockedTS, blocked.blockedTS)
    }

    @Test
    fun `toMessage converts BlockedMessage back to Message with all fields`() {
        val blocked = BlockedMessage(
            id = 99L,
            body = "blocked content",
            type = 1,
            status = 5,
            participants = arrayListOf(createSimpleContact("Bob", "5551234")),
            date = 1698000000,
            read = true,
            threadId = 300L,
            isMMS = true,
            attachment = null,
            senderPhoneNumber = "5551234",
            senderName = "Bob",
            senderPhotoUri = "content://photo/bob",
            subscriptionId = 2,
            isScheduled = false,
            blockedTS = 1700000000000L
        )

        val message = blocked.toMessage()

        assertEquals(blocked.id, message.id)
        assertEquals(blocked.body, message.body)
        assertEquals(blocked.type, message.type)
        assertEquals(blocked.status, message.status)
        assertEquals(blocked.participants, message.participants)
        assertEquals(blocked.date, message.date)
        assertEquals(blocked.read, message.read)
        assertEquals(blocked.threadId, message.threadId)
        assertEquals(blocked.isMMS, message.isMMS)
        assertEquals(blocked.attachment, message.attachment)
        assertEquals(blocked.senderPhoneNumber, message.senderPhoneNumber)
        assertEquals(blocked.senderName, message.senderName)
        assertEquals(blocked.senderPhotoUri, message.senderPhotoUri)
        assertEquals(blocked.subscriptionId, message.subscriptionId)
        assertEquals(blocked.isScheduled, message.isScheduled)
    }

    @Test
    fun `round-trip fromMessage then toMessage preserves all Message fields`() {
        val original = createMessage(
            id = 77L,
            body = "round trip",
            type = 1,
            status = 3,
            date = 1697000000,
            read = false,
            threadId = 400L,
            isMMS = false,
            senderPhoneNumber = "1112223333",
            senderName = "Charlie",
            senderPhotoUri = "content://photo/charlie",
            subscriptionId = 0,
            isScheduled = false
        )

        val blocked = BlockedMessage.fromMessage(original, System.currentTimeMillis())
        val restored = blocked.toMessage()

        assertEquals(original.id, restored.id)
        assertEquals(original.body, restored.body)
        assertEquals(original.type, restored.type)
        assertEquals(original.status, restored.status)
        assertEquals(original.participants, restored.participants)
        assertEquals(original.date, restored.date)
        assertEquals(original.read, restored.read)
        assertEquals(original.threadId, restored.threadId)
        assertEquals(original.isMMS, restored.isMMS)
        assertEquals(original.attachment, restored.attachment)
        assertEquals(original.senderPhoneNumber, restored.senderPhoneNumber)
        assertEquals(original.senderName, restored.senderName)
        assertEquals(original.senderPhotoUri, restored.senderPhotoUri)
        assertEquals(original.subscriptionId, restored.subscriptionId)
        assertEquals(original.isScheduled, restored.isScheduled)
    }

    @Test
    fun `areItemsTheSame returns true for same id`() {
        val msg = createMessage(id = 10L)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(body = "different"), 2000L)

        assertTrue(BlockedMessage.areItemsTheSame(b1, b2))
    }

    @Test
    fun `areItemsTheSame returns false for different id`() {
        val b1 = BlockedMessage.fromMessage(createMessage(id = 10L), 1000L)
        val b2 = BlockedMessage.fromMessage(createMessage(id = 20L), 2000L)

        assertFalse(BlockedMessage.areItemsTheSame(b1, b2))
    }

    @Test
    fun `areContentsTheSame returns true for identical content`() {
        val msg = createMessage(id = 10L)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg, 2000L)

        assertTrue(BlockedMessage.areContentsTheSame(b1, b2))
    }

    @Test
    fun `areContentsTheSame returns false when body differs`() {
        val msg = createMessage(id = 10L, body = "original")
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(body = "changed"), 2000L)

        assertFalse(BlockedMessage.areContentsTheSame(b1, b2))
    }

    @Test
    fun `areContentsTheSame returns false when threadId differs`() {
        val msg = createMessage(id = 10L, threadId = 100L)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(threadId = 200L), 2000L)

        assertFalse(BlockedMessage.areContentsTheSame(b1, b2))
    }

    @Test
    fun `areContentsTheSame returns false when isMMS differs`() {
        val msg = createMessage(id = 10L, isMMS = false)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(isMMS = true), 2000L)

        assertFalse(BlockedMessage.areContentsTheSame(b1, b2))
    }

    @Test
    fun `areContentsTheSame returns false when senderName differs`() {
        val msg = createMessage(id = 10L, senderName = "Alice")
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(senderName = "Bob"), 2000L)

        assertFalse(BlockedMessage.areContentsTheSame(b1, b2))
    }

    @Test
    fun `areContentsTheSame returns false when isScheduled differs`() {
        val msg = createMessage(id = 10L, isScheduled = false)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(isScheduled = true), 2000L)

        assertFalse(BlockedMessage.areContentsTheSame(b1, b2))
    }

    @Test
    fun `areContentsTheSame ignores blockedTS difference`() {
        val msg = createMessage(id = 10L)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg, 9999L)

        assertTrue(BlockedMessage.areContentsTheSame(b1, b2))
    }

    @Test
    fun `isReceivedMessage returns true for inbox type`() {
        val msg = createMessage(type = 1) // Telephony.Sms.MESSAGE_TYPE_INBOX
        val blocked = BlockedMessage.fromMessage(msg, 1000L)

        assertTrue(blocked.isReceivedMessage())
    }

    @Test
    fun `isReceivedMessage returns false for sent type`() {
        val msg = createMessage(type = 2) // Telephony.Sms.MESSAGE_TYPE_SENT
        val blocked = BlockedMessage.fromMessage(msg, 1000L)

        assertFalse(blocked.isReceivedMessage())
    }

    @Test
    fun `millis converts date from seconds to milliseconds`() {
        val msg = createMessage(date = 1700000000)
        val blocked = BlockedMessage.fromMessage(msg, 1000L)

        assertEquals(1700000000L * 1000L, blocked.millis())
    }

    @Test
    fun `getStableId returns same value for same id and isMMS`() {
        val msg = createMessage(id = 42L, isMMS = false)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(body = "different"), 2000L)

        assertEquals(b1.getStableId(), b2.getStableId())
    }

    @Test
    fun `getStableId returns different value for different isMMS`() {
        val msg = createMessage(id = 42L, isMMS = false)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(isMMS = true), 2000L)

        assertFalse(b1.getStableId() == b2.getStableId())
    }

    @Test
    fun `getStableId returns different value for different id`() {
        val b1 = BlockedMessage.fromMessage(createMessage(id = 42L), 1000L)
        val b2 = BlockedMessage.fromMessage(createMessage(id = 43L), 2000L)

        assertFalse(b1.getStableId() == b2.getStableId())
    }

    @Test
    fun `getSelectionKey returns same value for same id`() {
        val msg = createMessage(id = 42L)
        val b1 = BlockedMessage.fromMessage(msg, 1000L)
        val b2 = BlockedMessage.fromMessage(msg.copy(body = "different"), 2000L)

        assertEquals(b1.getSelectionKey(), b2.getSelectionKey())
    }
}
