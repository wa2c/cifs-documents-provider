package com.wa2c.android.cifsdocumentsprovider.common.values

enum class HostSortType(
    val intValue: Int,
) {
    DetectionAscend(10),
    DetectionDescend(1),
    HostNameAscend(20),
    HostNameDescend(21),
    IpAddressAscend(30),
    IpAddressDescend(31),
    ;

    companion object {

        /** Default sort type */
        val default = DetectionAscend

        /**
         * Find soft type or default (TimeAscend).
         */
        fun findByValueOrDefault(value: Int?): HostSortType {
            return entries.firstOrNull { it.intValue == value } ?: default
        }

    }
}
