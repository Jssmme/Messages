package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.messages.models.BlockReason

@Dao
interface BlockReasonsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(reasons: List<BlockReason>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(reason: BlockReason)

    @Query("SELECT * FROM block_reasons WHERE message_id = :messageId")
    fun getReasonsForMessage(messageId: Long): List<BlockReason>

    @Query("SELECT * FROM block_reasons WHERE thread_id = :threadId")
    fun getReasonsForThread(threadId: Long): List<BlockReason>

    @Query("SELECT * FROM block_reasons WHERE thread_id IN (:threadIds)")
    fun getReasonsForThreads(threadIds: List<Long>): List<BlockReason>

    @Query("DELETE FROM block_reasons WHERE message_id = :messageId")
    fun deleteByMessageId(messageId: Long)

    @Query("DELETE FROM block_reasons WHERE thread_id = :threadId")
    fun deleteByThreadId(threadId: Long)

    @Query("DELETE FROM block_reasons")
    fun deleteAll()
}
