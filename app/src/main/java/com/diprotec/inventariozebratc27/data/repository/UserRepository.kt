package com.diprotec.inventariozebratc27.data.repository

import com.diprotec.inventariozebratc27.data.local.entity.UserEntity
import com.diprotec.inventariozebratc27.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun fetchRemoteUsers(): List<UserDto>

    suspend fun replaceAllUsers(list: List<UserEntity>)

    fun observeUsers(): Flow<List<UserEntity>>
}