package com.example.coursetable.data.remote

/**
 * 教务系统学校配置
 */
data class PortalSchoolConfig(
    val schoolCode: String,
    val schoolName: String,
    val entryUrl: String,
    val host: String,
) {
    companion object {
        /**
         * 支持的学校列表
         */
        val supportedSchools = listOf(
            PortalSchoolConfig(
                schoolCode = NcstPortal.SCHOOL_CODE,
                schoolName = NcstPortal.SCHOOL_NAME,
                entryUrl = NcstPortal.ENTRY_URL,
                host = NcstPortal.HOST,
            ),
            PortalSchoolConfig(
                schoolCode = XzcitPortal.SCHOOL_CODE,
                schoolName = XzcitPortal.SCHOOL_NAME,
                entryUrl = XzcitPortal.ENTRY_URL,
                host = XzcitPortal.HOST,
            ),
        )

        /**
         * 根据学校代码获取配置
         */
        fun findByCode(code: String): PortalSchoolConfig? {
            return supportedSchools.firstOrNull { it.schoolCode == code }
        }

        /**
         * 默认学校（华北理工大学）
         */
        val default: PortalSchoolConfig
            get() = supportedSchools.first()
    }
}
