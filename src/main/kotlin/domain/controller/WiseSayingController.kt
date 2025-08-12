package com.ll.wiseSaying

import java.util.*

class WiseSayingController(
    private val scanner: Scanner,
    private val service: WiseSayingService = WiseSayingService()
) {
    fun register() {
        print("명언 : ")
        val content = scanner.nextLine()
        print("작가 : ")
        val author = scanner.nextLine()
        val id = service.register(content, author)
        println("${id}번 명언이 등록되었습니다.")
    }

    fun list(rq: Rq) {
        val kt = rq.get("keywordType")
        val kw = rq.get("keyword")
        val page = rq.getInt("page") ?: 1
        if (!kw.isNullOrBlank()) {
            println("----------------------")
            println("검색타입 : ${kt ?: ""}")
            println("검색어 : $kw")
            println("----------------------")
        }
        val paged = service.list(kt, kw, page, 5)
        println("번호 / 작가 / 명언")
        println("----------------------")
        paged.items.forEach { println("${it.id} / ${it.author} / ${it.content}") }
        println("----------------------")
        val pagesStr = (1..paged.totalPages)
            .joinToString(" / ") { i -> if (i == paged.page) "[$i]" else "$i" }
        println("페이지 : $pagesStr")
    }

    fun delete(rq: Rq) {
        val id = rq.getLong("id")
        if (id == null) {
            println("id를 정확히 입력해주세요.")
            return
        }
        if (service.delete(id)) println("${id}번 명언이 삭제되었습니다.")
        else println("${id}번 명언은 존재하지 않습니다.")
    }

    fun modify(rq: Rq) {
        val id = rq.getLong("id")
        if (id == null) {
            println("id를 정확히 입력해주세요.")
            return
        }
        val before = service.get(id)
        if (before == null) {
            println("${id}번 명언은 존재하지 않습니다.")
            return
        }
        println("명언(기존) : ${before.content}")
        print("명언 : ")
        val newContent = scanner.nextLine()
        println("작가(기존) : ${before.author}")
        print("작가 : ")
        val newAuthor = scanner.nextLine()
        service.modify(id, newContent, newAuthor)
    }

    fun build() {
        service.buildDataJson()
        println("data.json 파일의 내용이 갱신되었습니다.")
    }
}
