package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_messages",
    indices = [(Index(value = ["id"], unique = true))]
)
data class BlockedMessage(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "blocked_ts") var blockedTS: Long
)
