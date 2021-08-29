package com.wa2c.android.cifsdocumentsprovider.common.values

import androidx.annotation.MenuRes
import com.wa2c.android.cifsdocumentsprovider.R

enum class HostSortType(
    val intValue: Int,
    @MenuRes val menuId: Int
) {
    DetectionAscend(10, R.id.host_menu_sort_type_detection_ascend),
    DetectionDescend(11, R.id.host_menu_sort_type_detection_descend),
    HostNameAscend(20, R.id.host_menu_sort_type_host_name_ascend),
    HostNameDescend(21, R.id.host_menu_sort_type_host_name_descend),
    IpAddressAscend(30, R.id.host_menu_sort_type_ip_address_ascend),
    IpAddressDescend(31, R.id.host_menu_sort_type_ip_address_descend),
    ;

    companion object {

        /** Default sort type */
        val DEFAULT = DetectionAscend

        /**
         * Find soft type or default (TimeAscend).
         */
        fun findByValueOrDefault(value: Int): HostSortType {
            return values().firstOrNull { it.intValue == value } ?: DEFAULT
        }

    }
}