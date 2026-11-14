package com.romafrihet.server.lesson

import com.romafrihet.server.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.util.UUID

/** 저장소 파일의 메타데이터다. 문제와 보기가 이 행을 참조하지 않으면 고아 이미지다. */
@Entity
@Table(name = "question_images")
class QuestionImage private constructor(userId: UUID, storagePath: String, contentType: String, sizeBytes: Long) :
    BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_images_seq")
    @SequenceGenerator(name = "question_images_seq", sequenceName = "question_images_seq", allocationSize = 50)
    val id: Long? = null

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @Column(name = "storage_path", nullable = false, updatable = false)
    var storagePath: String = storagePath
        protected set

    @Column(name = "content_type", nullable = false, updatable = false, length = 50)
    var contentType: String = contentType
        protected set

    @Column(name = "size_bytes", nullable = false, updatable = false)
    var sizeBytes: Long = sizeBytes
        protected set

    companion object {
        fun create(userId: UUID, storagePath: String, contentType: String, sizeBytes: Long): QuestionImage =
            QuestionImage(userId, storagePath, contentType, sizeBytes)
    }
}
