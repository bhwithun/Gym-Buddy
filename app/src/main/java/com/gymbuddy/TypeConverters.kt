package com.gymbuddy

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): Map<String, Int>? {
        if (value == null) return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromMap(map: Map<String, Int>?): String {
        return gson.toJson(map ?: emptyMap<String, Int>())
    }
}