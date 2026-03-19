package com.wscanplus.core.db

import androidx.room.TypeConverter
import com.wscanplus.core.threat.EnvironmentType
import com.wscanplus.core.threat.HeuristicType
import com.wscanplus.core.threat.ThreatSource
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringSet(value: Set<String>): String = JSONArray(value.toList()).toString()

    @TypeConverter
    fun toStringSet(value: String): Set<String> {
        if (value.isBlank()) return emptySet()
        val array = JSONArray(value)
        return buildSet {
            for (index in 0 until array.length()) {
                add(array.getString(index))
            }
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getString(index))
            }
        }
    }

    @TypeConverter
    fun fromThreatSource(value: ThreatSource): String = value.name

    @TypeConverter
    fun toThreatSource(value: String): ThreatSource = ThreatSource.valueOf(value)

    @TypeConverter
    fun fromHeuristicType(value: HeuristicType?): String? = value?.name

    @TypeConverter
    fun toHeuristicType(value: String?): HeuristicType? = value?.let { HeuristicType.valueOf(it) }

    @TypeConverter
    fun fromEnvironmentType(value: EnvironmentType): String = value.name

    @TypeConverter
    fun toEnvironmentType(value: String): EnvironmentType = EnvironmentType.valueOf(value)
}
