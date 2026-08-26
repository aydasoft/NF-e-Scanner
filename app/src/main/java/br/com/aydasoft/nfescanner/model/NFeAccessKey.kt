package br.com.aydasoft.nfescanner.model

import java.util.Locale

/**
 * Chave de acesso de NF-e/NFC-e, incluindo o formato com CNPJ alfanumérico.
 *
 * Formato atual: [0-9]{6}[A-Z0-9]{12}[0-9]{26}
 */
@JvmInline
value class NFeAccessKey private constructor(val value: String) {

    val model: String
        get() = value.substring(MODEL_START_INDEX, MODEL_END_INDEX)

    val documentType: DocumentType
        get() = when (model) {
            MODEL_NFE -> DocumentType.NFE
            MODEL_NFCE -> DocumentType.NFCE
            else -> error("Modelo já validado, mas não reconhecido: $model")
        }

    val formatted: String
        get() = value.chunked(4).joinToString(" ")

    enum class DocumentType {
        NFE,
        NFCE,
    }

    companion object {
        private const val MODEL_START_INDEX = 20
        private const val MODEL_END_INDEX = 22
        private const val MODEL_NFE = "55"
        private const val MODEL_NFCE = "65"
        private const val BASE_LENGTH = 43
        private const val FULL_LENGTH = 44

        private val exactPattern =
            Regex("""[0-9]{6}[A-Z0-9]{12}[0-9]{26}""")

        private val accessKeyInTextPattern =
            Regex("""[0-9]{6}[A-Z0-9]{12}[0-9]{26}""")

        private val validStateCodes = setOf(
            "11", "12", "13", "14", "15", "16", "17",
            "21", "22", "23", "24", "25", "26", "27", "28", "29",
            "31", "32", "33", "35",
            "41", "42", "43",
            "50", "51", "52", "53",
        )

        /** Retorna a chave apenas quando todos os campos estruturais e o DV são válidos. */
        fun parse(candidate: String?): NFeAccessKey? {
            if (candidate.isNullOrBlank()) return null

            val normalized = candidate
                .trim()
                .uppercase(Locale.ROOT)

            if (!exactPattern.matches(normalized)) return null
            if (!hasValidStructure(normalized)) return null
            if (!hasValidCheckDigit(normalized)) return null

            return NFeAccessKey(normalized)
        }

        /**
         * Extrai uma chave de um código de barras, texto formatado ou URL de QR Code.
         */
        fun extract(rawValue: String?): NFeAccessKey? {
            if (rawValue.isNullOrBlank()) return null

            val normalized = rawValue.uppercase(Locale.ROOT)

            accessKeyInTextPattern.findAll(normalized)
                .mapNotNull { parse(it.value) }
                .firstOrNull()
                ?.let { return it }

            val withoutCommonSeparators = normalized
                .replace(Regex("""[\s.\-_/]"""), "")

            return parse(withoutCommonSeparators)
        }

        fun isValid(candidate: String?): Boolean = parse(candidate) != null

        /** Calcula o DV de uma base de 43 caracteres. */
        fun calculateCheckDigit(base: String): Int {
            require(base.length == BASE_LENGTH) {
                "A base da chave deve possuir $BASE_LENGTH caracteres."
            }
            require(base.all { it.isDigit() || it in 'A'..'Z' }) {
                "A base deve conter apenas números e letras maiúsculas."
            }

            var sum = 0
            var weight = 2

            for (index in base.lastIndex downTo 0) {
                // Regra CNPJ alfanumérico: valor ASCII menos 48.
                val characterValue = base[index].code - '0'.code
                sum += characterValue * weight
                weight = if (weight == 9) 2 else weight + 1
            }

            val remainder = sum % 11
            return if (remainder == 0 || remainder == 1) 0 else 11 - remainder
        }

        private fun hasValidStructure(value: String): Boolean {
            if (value.length != FULL_LENGTH) return false
            if (value.substring(0, 2) !in validStateCodes) return false

            val month = value.substring(4, 6).toIntOrNull() ?: return false
            if (month !in 1..12) return false

            val model = value.substring(MODEL_START_INDEX, MODEL_END_INDEX)
            if (model != MODEL_NFE && model != MODEL_NFCE) return false

            return true
        }

        private fun hasValidCheckDigit(value: String): Boolean {
            val expected = calculateCheckDigit(value.take(BASE_LENGTH))
            val actual = value.last().digitToIntOrNull() ?: return false
            return expected == actual
        }
    }
}

