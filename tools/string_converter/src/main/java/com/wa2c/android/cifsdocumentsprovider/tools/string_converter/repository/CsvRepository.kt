package com.wa2c.android.cifsdocumentsprovider.tools.string_converter.repository

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import com.wa2c.android.cifsdocumentsprovider.tools.string_converter.model.CsvRow
import java.io.FileInputStream
import java.net.URL
import java.nio.charset.Charset

/**
 * CSV file repository
 */
class CsvRepository {

    private val parser = CSVParserBuilder()
        .withEscapeChar(Char.MIN_VALUE)
        .build()

    fun downloadCsv(url: String): String {
        return URL(url).openStream().bufferedReader(charset = Charsets.UTF_8).use { input ->
            input.readText()
        }
    }

    fun readCsv(csvFilePath: String): List<CsvRow> {
        return FileInputStream(csvFilePath).reader(CSV_CHARSET).use { reader ->
            val rawList = CSVReaderBuilder(reader)
                .withCSVParser(parser)
                .build()
                .use { it.readAll() }
            rawList
                .asSequence()
                .drop(2)
                .let { seq ->
                    val titleRow = seq.first()
                    val codeMap = titleRow.drop(2).mapIndexed { index, code -> index to code }.toMap()
                    seq.map { row ->
                        val langText = row.drop(2).mapIndexed { index, text -> codeMap[index]!! to text }.toMap()
                        CsvRow(
                            title = row[0],
                            resourceId = row[1],
                            langText = langText,
                        )
                    }
                }.toList()
        }
    }

    companion object {
        private val CSV_CHARSET = Charset.forName("UTF-8")
    }
}
