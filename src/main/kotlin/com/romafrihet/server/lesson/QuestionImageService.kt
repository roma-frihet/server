package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheckNotNull
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.dto.QuestionImageDto
import com.romafrihet.server.storage.StorageClient
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.UUID

/** 외부 저장소를 호출하므로 트랜잭션을 두지 않는다. */
@Service
class QuestionImageService(
    private val storageClient: StorageClient,
    private val questionImageRepository: QuestionImageRepository,
) {
    /**
     * 클라이언트가 보낸 Content-Type은 믿지 않고 파일 앞부분으로 형식을 판별한다.
     * 소유자는 메타데이터에 남기므로 경로에는 추측할 수 없는 값만 쓰고 파일 이름도 넣지 않는다.
     */
    fun upload(command: QuestionCommand.UploadImage): QuestionImageDto {
        val format = assertCheckNotNull(ImageFormat.detect(command.bytes)) { ErrorCode.QUESTION_IMAGE_INVALID }
        val path = "${UUID.randomUUID()}.${format.extension}"
        storageClient.upload(path, command.bytes, format.mediaType)
        val image =
            questionImageRepository.save(
                QuestionImage.create(
                    userId = command.userId,
                    storagePath = path,
                    contentType = format.mediaType.toString(),
                    sizeBytes = command.bytes.size.toLong(),
                ),
            )
        return QuestionImageDto(id = requireNotNull(image.id), url = storageClient.createSignedUrls(listOf(path))[path])
    }

    fun deleteAll(paths: List<String>) = storageClient.delete(paths)

    private enum class ImageFormat(val extension: String, val mediaType: MediaType) {
        PNG("png", MediaType.IMAGE_PNG),
        JPEG("jpg", MediaType.IMAGE_JPEG),
        WEBP("webp", MediaType.parseMediaType("image/webp")),
        ;

        companion object {
            private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
            private val RIFF_SIGNATURE = "RIFF".toByteArray()
            private val WEBP_SIGNATURE = "WEBP".toByteArray()
            private const val WEBP_SIGNATURE_OFFSET = 8

            fun detect(bytes: ByteArray): ImageFormat? = when {
                bytes.hasAt(0, PNG_SIGNATURE) -> PNG
                bytes.hasAt(0, JPEG_SIGNATURE) -> JPEG
                bytes.hasAt(0, RIFF_SIGNATURE) && bytes.hasAt(WEBP_SIGNATURE_OFFSET, WEBP_SIGNATURE) -> WEBP
                else -> null
            }

            private fun ByteArray.hasAt(offset: Int, signature: ByteArray): Boolean =
                size >= offset + signature.size && signature.indices.all { this[offset + it] == signature[it] }
        }
    }
}
