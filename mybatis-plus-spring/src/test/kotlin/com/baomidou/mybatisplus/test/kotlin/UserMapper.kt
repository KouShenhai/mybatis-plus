package com.baomidou.mybatisplus.test.kotlin

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper

interface UserMapper : BaseMapper<User> {

    fun hello(): String {
        return "hello baomidou!";
    }

    fun findById(id: Int): User? {
        return selectOne(KtQueryWrapper(User::class.java).eq(User::id, id));
    }

}
