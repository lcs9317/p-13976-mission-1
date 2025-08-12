package com.ll.wiseSaying

import java.io.File

interface WiseSayingRepository {
    fun save(entity: WiseSaying): WiseSaying
    fun findById(id: Long): WiseSaying?
    fun findAll(): List<WiseSaying>
    fun deleteById(id: Long): Boolean
}

class WiseSayingFileRepository(baseDir: String = System.getProperty("ws.db", "db/wiseSaying")): WiseSayingRepository {
    private val dir = File(baseDir)
    private val lastIdFile = File(dir, "lastId.txt")

    init {
        if (!dir.exists()) dir.mkdirs()
        if (!lastIdFile.exists()) lastIdFile.writeText("0")
    }

    private fun genNextId(): Long {
        val last = lastIdFile.readText().trim().toLongOrNull() ?: 0L
        val next = last + 1
        lastIdFile.writeText(next.toString())
        return next
    }

    override fun save(entity: WiseSaying): WiseSaying {
        if (entity.id == null) entity.id = genNextId()
        val json = """
            {
              "id": ${entity.id},
              "content": "${escape(entity.content)}",
              "author": "${escape(entity.author)}"
            }
        """.trimIndent()
        File(dir, "${entity.id}.json").writeText(json)
        return entity
    }

    override fun findById(id: Long): WiseSaying? {
        val f = File(dir, "$id.json")
        if (!f.exists()) return null
        return parse(f.readText())
    }

    override fun findAll(): List<WiseSaying> {
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.mapNotNull { parse(it.readText()) }
            ?.sortedByDescending { it.id ?: 0L } ?: emptyList()
    }

    override fun deleteById(id: Long): Boolean {
        val f = File(dir, "$id.json")
        return f.exists() && f.delete()
    }

    private fun parse(s: String): WiseSaying? {
        fun pickStr(k: String): String? = Regex(""""$k"\s*:\s*"(.*?)"""").find(s)?.groupValues?.getOrNull(1)?.let { unescape(it) }
        fun pickNum(k: String): Long? = Regex(""""$k"\s*:\s*(\d+)""").find(s)?.groupValues?.getOrNull(1)?.toLongOrNull()
        val id = pickNum("id") ?: return null
        val content = pickStr("content") ?: return null
        val author = pickStr("author") ?: return null
        return WiseSaying(id, content, author)
    }

    private fun escape(x: String) = x.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
    private fun unescape(x: String) = x.replace("\\\"", "\"").replace("\\\\", "\\")
}
