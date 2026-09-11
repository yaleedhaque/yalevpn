package com.yaleed.vpnresearch.shizuku

/**
 * Runs inside a Shizuku user-service process (uid 2000 shell / root), where
 * `settings` (the Android settings provider) is writable — mirroring what the
 * adb-rooted research flow already proved for android_id spoofing.
 */
class ShizukuService : IShizukuService.Stub() {

    override fun uid(): Int = android.os.Process.myUid()

    override fun run(cmd: Array<String>): String {
        return try {
            val p = Runtime.getRuntime().exec(cmd)
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val exit = p.waitFor()
            "exit=$exit\n$out$err"
        } catch (e: Exception) {
            "ERR ${e.message}"
        }
    }

    override fun readAndroidId(): String {
        val r = run(arrayOf("/system/bin/settings", "get", "secure", "android_id"))
        return r
            .removePrefix("exit=")
            .substringAfter('\n', "")
            .trim()
    }

    override fun writeAndroidId(value: String): Int {
        val r = run(arrayOf("/system/bin/settings", "put", "secure", "android_id", value))
        return if (r.startsWith("exit=0")) 0 else 1
    }
}