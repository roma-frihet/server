package com.romafrihet.server.subject

import com.romafrihet.server.TestcontainersConfiguration
import com.romafrihet.server.config.JpaConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

/** 과목은 마이그레이션 시드가 기준이므로 실제 스키마에서 확인한다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class, JpaConfig::class)
@Testcontainers(disabledWithoutDocker = true)
class SubjectRepositoryTest(@Autowired private val subjectRepository: SubjectRepository) {
    @Test
    fun `중학교 과목은 교육과정 순서대로 내려간다`() {
        val names = subjectRepository.findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.MIDDLE).map { it.name }

        assertThat(names).startsWith("국어", "사회", "역사", "도덕", "수학", "과학")
    }

    @Test
    fun `교과군이 함께 저장된다`() {
        val science = subjectRepository
            .findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.MIDDLE)
            .single { it.name == "과학" }

        assertThat(science.subjectGroup).isEqualTo("과학")
    }

    @Test
    fun `학교급으로 거르면 다른 학교급 과목은 섞이지 않는다`() {
        val middle = subjectRepository.findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.MIDDLE)
        val high = subjectRepository.findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.HIGH)

        assertThat(middle.map { it.schoolLevel }).containsOnly(SchoolLevel.MIDDLE)
        assertThat(high.map { it.schoolLevel }).containsOnly(SchoolLevel.HIGH)
        assertThat(middle.map { it.name }).doesNotContain("물리학", "통합과학1")
        assertThat(high.map { it.name }).contains("물리학", "지구과학", "통합과학1")
    }

    @Test
    fun `같은 이름이라도 학교급이 다르면 다른 과목이다`() {
        val middle = subjectRepository.findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.MIDDLE)
        val high = subjectRepository.findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.HIGH)

        val middleMusic = middle.single { it.name == "음악" }
        val highMusic = high.single { it.name == "음악" }

        assertThat(middleMusic.id).isNotEqualTo(highMusic.id)
    }
}
