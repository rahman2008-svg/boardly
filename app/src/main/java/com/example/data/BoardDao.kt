package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM boards ORDER BY updatedAt DESC")
    fun getAllBoards(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :id")
    fun getBoardById(id: Long): Flow<BoardEntity?>

    @Query("SELECT * FROM boards WHERE id = :id")
    suspend fun getBoardByIdOneShot(id: Long): BoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity): Long

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Delete
    suspend fun deleteBoard(board: BoardEntity)
    
    @Query("DELETE FROM boards WHERE id = :id")
    suspend fun deleteBoardById(id: Long)
}
