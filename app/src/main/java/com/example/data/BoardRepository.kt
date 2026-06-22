package com.example.data

import kotlinx.coroutines.flow.Flow

class BoardRepository(private val boardDao: BoardDao) {
    
    val allBoards: Flow<List<BoardEntity>> = boardDao.getAllBoards()

    fun getBoardById(id: Long): Flow<BoardEntity?> = boardDao.getBoardById(id)

    suspend fun getBoardByIdOneShot(id: Long): BoardEntity? = boardDao.getBoardByIdOneShot(id)

    suspend fun insertBoard(board: BoardEntity): Long = boardDao.insertBoard(board)

    suspend fun updateBoard(board: BoardEntity) = boardDao.updateBoard(board)

    suspend fun deleteBoard(board: BoardEntity) = boardDao.deleteBoard(board)
    
    suspend fun deleteBoardById(id: Long) = boardDao.deleteBoardById(id)
}
