package br.com.aydasoft.nfescanner.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NFeAccessKeyTest {

    @Test
    fun `accepts valid numeric access key`() {
        val key = NFeAccessKey.parse(NUMERIC_KEY)

        assertNotNull(key)
        assertEquals(NFeAccessKey.DocumentType.NFE, key?.documentType)
        assertEquals(NUMERIC_KEY, key?.value)
    }

    @Test
    fun `accepts valid alphanumeric access key and normalizes lowercase`() {
        val key = NFeAccessKey.parse(ALPHANUMERIC_KEY.lowercase())

        assertNotNull(key)
        assertEquals(ALPHANUMERIC_KEY, key?.value)
    }

    @Test
    fun `extracts key from NFC-e style QR URL`() {
        val url = "https://consulta.exemplo.gov.br/qrcode?p=$NUMERIC_KEY|2|1|abc"

        assertEquals(NUMERIC_KEY, NFeAccessKey.extract(url)?.value)
    }

    @Test
    fun `extracts key formatted in groups of four`() {
        val formatted = NUMERIC_KEY.chunked(4).joinToString(" ")

        assertEquals(NUMERIC_KEY, NFeAccessKey.extract(formatted)?.value)
    }

    @Test
    fun `rejects invalid check digit`() {
        val invalid = NUMERIC_KEY.dropLast(1) + "1"

        assertNull(NFeAccessKey.parse(invalid))
        assertFalse(NFeAccessKey.isValid(invalid))
    }

    @Test
    fun `rejects unsupported document model even with recalculated check digit`() {
        val base = NUMERIC_KEY.take(20) + "57" + NUMERIC_KEY.substring(22, 43)
        val candidate = base + NFeAccessKey.calculateCheckDigit(base)

        assertNull(NFeAccessKey.parse(candidate))
    }

    @Test
    fun `rejects invalid state code even with recalculated check digit`() {
        val base = "99" + NUMERIC_KEY.substring(2, 43)
        val candidate = base + NFeAccessKey.calculateCheckDigit(base)

        assertNull(NFeAccessKey.parse(candidate))
    }

    @Test
    fun `calculates numeric and alphanumeric access key check digits`() {
        assertEquals(0, NFeAccessKey.calculateCheckDigit(NUMERIC_KEY.take(43)))
        assertEquals(0, NFeAccessKey.calculateCheckDigit(ALPHANUMERIC_KEY.take(43)))
        assertTrue(NFeAccessKey.isValid(ALPHANUMERIC_KEY))
    }

    private companion object {
        const val NUMERIC_KEY = "35260812345678000195550010000001231123456780"
        const val ALPHANUMERIC_KEY = "35260812ABC34501DE35550010000001231123456780"
    }
}

