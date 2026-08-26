package br.com.aydasoft.nfescanner.model

import java.util.Locale

/**
 * Localiza uma chave válida no texto reconhecido pela câmera.
 *
 * O DV da NF-e é sempre validado antes de qualquer resultado ser aceito. As
 * substituições abaixo cobrem confusões comuns do OCR em trechos obrigatoriamente
 * numéricos e mantêm intacta a faixa reservada ao CNPJ alfanumérico.
 */
object NFeOcrKeyExtractor {

    private const val ACCESS_KEY_LENGTH = 44
    private const val ALPHANUMERIC_CNPJ_START = 6
    private const val ALPHANUMERIC_CNPJ_END = 18

    fun extract(candidateTexts: Sequence<String>): NFeAccessKey? {
        for (text in candidateTexts) {
            NFeAccessKey.extract(text)?.let { return it }

            val compact = text
                .uppercase(Locale.ROOT)
                .filter { it.isAsciiLetterOrDigit() }

            if (compact.length < ACCESS_KEY_LENGTH) continue

            for (start in 0..compact.length - ACCESS_KEY_LENGTH) {
                val candidate = compact.substring(start, start + ACCESS_KEY_LENGTH)

                candidateVariants(candidate)
                    .mapNotNull(NFeAccessKey::parse)
                    .firstOrNull()
                    ?.let { return it }
            }
        }

        return null
    }

    private fun candidateVariants(candidate: String): Sequence<String> = sequence {
        yield(candidate)

        yield(
            candidate.mapIndexed { index, character ->
                if (
                    index < ALPHANUMERIC_CNPJ_START ||
                    index >= ALPHANUMERIC_CNPJ_END
                ) {
                    character.asLikelyDigit()
                } else {
                    character
                }
            }.joinToString(separator = ""),
        )

        yield(candidate.map { it.asLikelyDigit() }.joinToString(separator = ""))
    }.distinct()

    private fun Char.isAsciiLetterOrDigit(): Boolean =
        this in '0'..'9' || this in 'A'..'Z'

    private fun Char.asLikelyDigit(): Char = when (this) {
        'O', 'Q', 'D' -> '0'
        'I', 'L' -> '1'
        'Z' -> '2'
        'S' -> '5'
        'G' -> '6'
        'B' -> '8'
        else -> this
    }
}
