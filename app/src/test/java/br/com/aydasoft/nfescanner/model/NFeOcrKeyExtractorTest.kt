package br.com.aydasoft.nfescanner.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NFeOcrKeyExtractorTest {

    @Test
    fun `extracts the key printed in the damaged label`() {
        val texts = sequenceOf(
            "DANFE Simplificada",
            "NF: 1416 Série: 1",
            "31260847149152000203550010000014161416097271",
        )

        assertEquals(PHOTO_KEY, NFeOcrKeyExtractor.extract(texts)?.value)
    }

    @Test
    fun `joins groups and ignores a label on the same line`() {
        val grouped = PHOTO_KEY.chunked(4).joinToString(" ")

        assertEquals(
            PHOTO_KEY,
            NFeOcrKeyExtractor.extract(sequenceOf("CHAVE DE ACESSO $grouped"))?.value,
        )
    }

    @Test
    fun `corrects common OCR substitutions in a numeric key`() {
        val ocrText = PHOTO_KEY
            .replace('0', 'O')
            .replace('1', 'I')

        assertEquals(
            PHOTO_KEY,
            NFeOcrKeyExtractor.extract(sequenceOf(ocrText))?.value,
        )
    }

    @Test
    fun `rejects OCR text when the check digit is invalid`() {
        val invalid = PHOTO_KEY.dropLast(1) + "2"

        assertNull(NFeOcrKeyExtractor.extract(sequenceOf(invalid)))
    }

    private companion object {
        const val PHOTO_KEY = "31260847149152000203550010000014161416097271"
    }
}
