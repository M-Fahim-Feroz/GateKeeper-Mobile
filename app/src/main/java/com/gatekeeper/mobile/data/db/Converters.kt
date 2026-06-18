package com.gatekeeper.mobile.data.db

import androidx.room.TypeConverter
import com.gatekeeper.mobile.data.db.entity.AccessStatus
import com.gatekeeper.mobile.data.db.entity.ConfidenceLevel
import com.gatekeeper.mobile.data.db.entity.DetectionSource
import com.gatekeeper.mobile.data.db.entity.HardwareResourceType

class Converters {
    @TypeConverter
    fun fromHardwareResourceType(value: HardwareResourceType?): String? = value?.name

    @TypeConverter
    fun toHardwareResourceType(value: String?): HardwareResourceType? = value?.let { HardwareResourceType.valueOf(it) }

    @TypeConverter
    fun fromAccessStatus(value: AccessStatus?): String? = value?.name

    @TypeConverter
    fun toAccessStatus(value: String?): AccessStatus? = value?.let { AccessStatus.valueOf(it) }

    @TypeConverter
    fun fromDetectionSource(value: DetectionSource?): String? = value?.name

    @TypeConverter
    fun toDetectionSource(value: String?): DetectionSource? = value?.let { DetectionSource.valueOf(it) }

    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel?): String? = value?.name

    @TypeConverter
    fun toConfidenceLevel(value: String?): ConfidenceLevel? = value?.let { ConfidenceLevel.valueOf(it) }
}
