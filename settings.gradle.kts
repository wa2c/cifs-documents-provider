pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "CIFS Documents Provider"

include(":app")
include(":common")
include(":data:data")
include(":data:storage:interfaces")
include(":data:storage:apache")
include(":data:storage:jcifsng")
include(":data:storage:smbj")
include(":domain")
include(":presentation")

includeBuild("tools/string_converter")
