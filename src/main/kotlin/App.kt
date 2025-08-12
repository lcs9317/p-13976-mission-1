package com.ll.wiseSaying

import java.io.File
import java.util.*

class App {
    private val scanner = Scanner(System.`in`)
    private var service = WiseSayingService()
    private var controller = WiseSayingController(scanner, service)

    fun run() {
        bootstrap()
        println("== 명언 앱 ==")
        while (true) {
            print("명령) ")
            if (!scanner.hasNextLine()) return
            val line = scanner.nextLine().trim()
            val rq = Rq(line)
            when (rq.action) {
                "종료" -> return
                "등록" -> controller.register()
                "목록" -> controller.list(rq)
                "삭제" -> controller.delete(rq)
                "수정" -> controller.modify(rq)
                "빌드" -> controller.build()
                else -> println("올바른 명령을 입력해주세요.")
            }
        }
    }

    private fun bootstrap() {
        val reset = System.getProperty("ws.reset") == "true" || System.getenv("WS_RESET") == "1"
        val seed = (System.getProperty("ws.seed") ?: System.getenv("WS_SEED"))?.toIntOrNull() ?: 0

        if (reset) {
            File("db/wiseSaying").deleteRecursively()
            service = WiseSayingService()
            controller = WiseSayingController(scanner, service)
        }

        if (seed > 0) {
            repeat(seed) { i ->
                service.register("명언 ${i + 1}", "작자미상 ${i + 1}")
            }
        }
    }
}
