package com.ll.wiseSaying

import java.io.File

class WiseSayingService(
    private val repo: WiseSayingRepository = WiseSayingFileRepository()
) {
    private val strip = Regex("""[^\w\s가-힣.,!?\-_/]""")

    fun register(content: String, author: String): Long {
        val saved = repo.save(WiseSaying(null, clean(content), clean(author)))
        return saved.id!!
    }

    fun modify(id: Long, content: String?, author: String?): Boolean {
        val found = repo.findById(id) ?: return false
        if (!content.isNullOrBlank()) found.content = clean(content)
        if (!author.isNullOrBlank()) found.author = clean(author)
        repo.save(found)
        return true
    }

    fun delete(id: Long): Boolean = repo.deleteById(id)

    fun get(id: Long): WiseSaying? = repo.findById(id)

    fun list(keywordType: String?, keyword: String?, page: Int, size: Int = 5): Paged<WiseSaying> {
        var data = repo.findAll()
        if (!keyword.isNullOrBlank()) {
            data = when (keywordType) {
                "author" -> data.filter { it.author.contains(keyword, true) }
                "content" -> data.filter { it.content.contains(keyword, true) }
                else -> data
            }
        }
        val total = data.size
        val p = if (page <= 0) 1 else page
        val from = (p - 1) * size
        val to = minOf(from + size, total)
        val items = if (from in 0..to) data.subList(from, to) else emptyList()
        val pages = maxOf(1, (total + size - 1) / size)
        return Paged(items, p, pages)
    }

    fun buildDataJson(path: String = "data.json") {
        val list = repo.findAll().sortedBy { it.id ?: 0L }
        val json = buildString {
            append("[\n")
            list.forEachIndexed { i, ws ->
                append("""  { "id": ${ws.id}, "content": "${escape(ws.content)}", "author": "${escape(ws.author)}" }""")
                if (i != list.lastIndex) append(",")
                append("\n")
            }
            append("]\n")
        }
        File(path).writeText(json)
    }

    private fun clean(s: String) = s.replace(strip, "").trim()
    private fun escape(x: String) = x.replace("\\", "\\\\").replace("\"", "\\\"")
}

data class Paged<T>(val items: List<T>, val page: Int, val totalPages: Int)
