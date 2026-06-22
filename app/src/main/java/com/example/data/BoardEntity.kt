package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val backgroundType: String = "GRID", // "GRID", "DOTS", "RULED", "BLANK"
    val elementsJson: String = "[]", // Serialized json array of elements
    val isLocked: Boolean = false
)
