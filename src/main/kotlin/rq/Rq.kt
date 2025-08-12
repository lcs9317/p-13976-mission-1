package com.ll.wiseSaying

class Rq(raw: String) {
    val action: String
    private val params: Map<String, String>
    init {
        val t = raw.trim()
        val q = t.indexOf('?')
        if (q == -1) {
            action = t
            params = emptyMap()
        } else {
            action = t.substring(0, q)
            val query = t.substring(q + 1)
            params = query.split('&').filter { it.isNotBlank() }.associate {
                val i = it.indexOf('=')
                if (i == -1) it to "" else it.substring(0, i) to it.substring(i + 1)
            }
        }
    }
    fun get(name: String): String? = params[name]
    fun getInt(name: String): Int? = params[name]?.toIntOrNull()
    fun getLong(name: String): Long? = params[name]?.toLongOrNull()
}
