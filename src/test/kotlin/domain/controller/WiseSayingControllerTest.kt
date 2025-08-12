package com.ll.wiseSaying

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class WiseSayingControllerTest {

    private val originalIn = System.`in`
    private val originalOut = System.out
    private val originalUserDir = System.getProperty("user.dir")

    @AfterEach
    fun tearDown() {
        System.setIn(originalIn)
        System.setOut(originalOut)
        System.setProperty("user.dir", originalUserDir)
        System.clearProperty("ws.db")
        System.clearProperty("ws.reset")
        System.clearProperty("ws.seed")
    }

    private fun runAppWith(
        input: String,
        dbDir: Path,
        reset: Boolean = false,
        seed: Int = 0,
        cwdDir: Path? = null
    ): String {
        System.setProperty("ws.db", dbDir.toString())
        if (reset) System.setProperty("ws.reset", "true")
        if (seed > 0) System.setProperty("ws.seed", seed.toString())
        if (cwdDir != null) System.setProperty("user.dir", cwdDir.toString())

        val out = ByteArrayOutputStream()
        System.setIn(ByteArrayInputStream((input.trimEnd() + "\n").toByteArray(StandardCharsets.UTF_8)))
        System.setOut(PrintStream(out, true, StandardCharsets.UTF_8))

        App().run()
        return out.toString(StandardCharsets.UTF_8)
    }

    private fun findDataJson(vararg candidates: Path): Path {
        for (dir in candidates) {
            val p = dir.resolve("data.json")
            if (Files.exists(p)) return p
        }
        val userDir = Path.of(System.getProperty("user.dir"))
        val p1 = userDir.resolve("data.json")
        if (Files.exists(p1)) return p1

        val cwd = Path.of("").toAbsolutePath()
        val p2 = cwd.resolve("data.json")
        if (Files.exists(p2)) return p2

        Files.walk(cwd, 2).use { stream ->
            val hit = stream
                .filter { Files.isRegularFile(it) && it.fileName.toString() == "data.json" }
                .findFirst()
                .orElse(null)
            if (hit != null) return hit
        }
        throw AssertionError("data.json not found in candidates=${candidates.toList()} user.dir=$userDir cwd=$cwd")
    }

    @Test
    fun `초기화와 시드`() {
        val db = Files.createTempDirectory("ws-seed")
        val out = runAppWith(
            """
            목록
            종료
            """.trimIndent(),
            dbDir = db,
            reset = true,
            seed = 3
        )
        assertThat(out).contains("번호 / 작가 / 명언")
        assertThat(out).contains("3 / 작자미상 3 / 명언 3")
        assertThat(out).contains("2 / 작자미상 2 / 명언 2")
        assertThat(out).contains("1 / 작자미상 1 / 명언 1")
        assertThat(out).contains("페이지 : [1]")
    }

    @Test
    fun `등록-목록-종료, 번호 증가 및 노출`() {
        val db = Files.createTempDirectory("ws-app1")
        val out = runAppWith(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            목록
            종료
            """.trimIndent(),
            dbDir = db,
            reset = true
        )
        assertThat(out).contains("1번 명언이 등록되었습니다.")
        assertThat(out).contains("2번 명언이 등록되었습니다.")
        assertThat(out).contains("번호 / 작가 / 명언")
        assertThat(out).contains("2 / 작자미상 / 과거에 집착하지 마라.")
        assertThat(out).contains("1 / 작자미상 / 현재를 사랑하라.")
        assertThat(out).contains("페이지 : [1]")
    }

    @Test
    fun `삭제와 재삭제 예외메시지, 번호 재사용 안함`() {
        val db = Files.createTempDirectory("ws-app2")
        val out = runAppWith(
            """
            등록
            A
            AA
            등록
            B
            BB
            삭제?id=1
            삭제?id=1
            등록
            C
            CC
            목록
            종료
            """.trimIndent(),
            dbDir = db,
            reset = true
        )
        assertThat(out).contains("1번 명언이 삭제되었습니다.")
        assertThat(out).contains("1번 명언은 존재하지 않습니다.")
        assertThat(out).contains("3번 명언이 등록되었습니다.")
        assertThat(out).contains("3 / CC / C")
        assertThat(out).doesNotContain("1 /")
    }

    @Test
    fun `수정 - 존재하지 않음 메시지, 성공 수정 후 목록 반영`() {
        val db = Files.createTempDirectory("ws-app3")
        val out = runAppWith(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            수정?id=3
            수정?id=2
            현재와 자신을 사랑하라.
            홍길동
            목록
            종료
            """.trimIndent(),
            dbDir = db,
            reset = true
        )
        assertThat(out).contains("3번 명언은 존재하지 않습니다.")
        assertThat(out).contains("명언(기존) : 과거에 집착하지 마라.")
        assertThat(out).contains("작가(기존) : 작자미상")
        assertThat(out).contains("2 / 홍길동 / 현재와 자신을 사랑하라.")
    }

    @Test
    fun `파일 영속성 - 재시작 후에도 목록 유지`() {
        val db = Files.createTempDirectory("ws-app4")
        runAppWith(
            """
            등록
            명언 1
            작가 1
            등록
            명언 2
            작가 2
            종료
            """.trimIndent(),
            dbDir = db,
            reset = true
        )
        val out2 = runAppWith(
            """
            목록
            종료
            """.trimIndent(),
            dbDir = db
        )
        assertThat(out2).contains("2 / 작가 2 / 명언 2")
        assertThat(out2).contains("1 / 작가 1 / 명언 1")
    }

    @Test
    fun `검색과 페이징 - 최신 10개, 5개씩, 키워드 반영`() {
        val db = Files.createTempDirectory("ws-app5")
        val sb = StringBuilder()
        repeat(10) { i ->
            sb.appendLine("등록")
            sb.appendLine("명언 ${i + 1}")
            sb.appendLine("작자미상 ${i + 1}")
        }
        sb.appendLine("목록")
        sb.appendLine("목록?page=2")
        sb.appendLine("목록?keywordType=content&keyword=명언 9")
        sb.appendLine("목록?keywordType=author&keyword=작자미상 1")
        sb.appendLine("종료")

        val out = runAppWith(sb.toString(), dbDir = db, reset = true)

        assertThat(out).contains("번호 / 작가 / 명언")
        assertThat(out).contains("10 / 작자미상 10 / 명언 10")
        assertThat(out).contains("6 / 작자미상 6 / 명언 6")
        assertThat(out).contains("페이지 : [1] / 2")

        assertThat(out).contains("5 / 작자미상 5 / 명언 5")
        assertThat(out).contains("1 / 작자미상 1 / 명언 1")
        assertThat(out).contains("페이지 : 1 / [2]")

        assertThat(out).contains("검색타입 : content")
        assertThat(out).contains("검색어 : 명언 9")
        assertThat(out).contains("9 / 작자미상 9 / 명언 9")

        assertThat(out).contains("검색타입 : author")
        assertThat(out).contains("검색어 : 작자미상 1")
        assertThat(out).contains("1 / 작자미상 1 / 명언 1")
    }

    @Test
    fun `빌드 - data_json 생성`() {
        val db = Files.createTempDirectory("ws-app6")
        val cwd = Files.createTempDirectory("ws-out6")
        val out = runAppWith(
            """
            등록
            명언 1
            작가 1
            등록
            명언 2
            작가 2
            빌드
            종료
            """.trimIndent(),
            dbDir = db,
            reset = true,
            cwdDir = cwd
        )
        assertThat(out).contains("data.json 파일의 내용이 갱신되었습니다.")

        val dataJson = findDataJson(cwd, db, Path.of(System.getProperty("user.dir")))
        val json = Files.readString(dataJson)
        assertThat(json).contains("""{ "id": 1, "content": "명언 1", "author": "작가 1" }""")
        assertThat(json).contains("""{ "id": 2, "content": "명언 2", "author": "작가 2" }""")
    }
}
