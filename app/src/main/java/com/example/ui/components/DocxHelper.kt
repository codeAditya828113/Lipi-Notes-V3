package com.example.ui.components

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DocxHelper {

    /**
     * Parses the text content from a .docx file's word/document.xml
     */
    fun parseDocxText(inputStream: InputStream): List<String> {
        val paragraphs = mutableListOf<String>()
        try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zipInputStream.readBytes().toString(Charsets.UTF_8)
                    paragraphs.addAll(extractParagraphsFromXml(xmlContent))
                    break
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        } catch (e: Exception) {
            Log.e("DocxHelper", "Failed to parse DOCX text", e)
        }
        return if (paragraphs.isEmpty()) listOf("Empty Document") else paragraphs
    }

    /**
     * Extracts text runs within paragraph tags in document.xml
     */
    private fun extractParagraphsFromXml(xml: String): List<String> {
        val result = mutableListOf<String>()
        // Word paragraphs: <w:p>...</w:p>
        // Word text runs: <w:t>...</w:t>
        val pRegex = Regex("<w:p\\b[^>]*>(.*?)</w:p>")
        val tRegex = Regex("<w:t\\b[^>]*>(.*?)</w:t>")
        
        val pMatches = pRegex.findAll(xml)
        for (pMatch in pMatches) {
            val pContent = pMatch.groupValues[1]
            val tMatches = tRegex.findAll(pContent)
            val pText = tMatches.map { it.groupValues[1] }.joinToString("")
            if (pText.isNotBlank()) {
                val cleanText = pText
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                result.add(cleanText)
            }
        }
        return result
    }

    /**
     * Creates a valid, minimal Microsoft Word (.docx) file containing the specified paragraphs and optional embedded images
     */
    fun createDocxFile(
        title: String,
        paragraphs: List<String>,
        context: android.content.Context? = null,
        images: List<com.example.data.ImageElement> = emptyList()
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        try {
            // Process images if available
            val imageEntries = mutableListOf<Triple<String, String, ByteArray>>() // (filename, rId, bytes)
            if (context != null && images.isNotEmpty()) {
                images.filter { !it.isHidden }.forEachIndexed { index, img ->
                    try {
                        val bitmap = PdfHelper.loadSoftwareBitmap(context, img.uri)
                        if (bitmap != null) {
                            val imgBaos = ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, imgBaos)
                            val bytes = imgBaos.toByteArray()
                            val fileName = "image${index + 1}.png"
                            val rId = "rId${index + 2}"
                            imageEntries.add(Triple(fileName, rId, bytes))
                        }
                    } catch (e: Exception) {
                        Log.e("DocxHelper", "Failed to process image ${img.uri} for DOCX", e)
                    }
                }
            }

            // 1. Write [Content_Types].xml
            val contentTypesXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Default Extension="png" ContentType="image/png"/>
                  <Default Extension="jpeg" ContentType="image/jpeg"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
            """.trimIndent()
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Write _rels/.rels
            val relsXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
            """.trimIndent()
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(relsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2b. Write word/_rels/document.xml.rels
            val docRelsXmlBuilder = StringBuilder()
            docRelsXmlBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            docRelsXmlBuilder.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
            for (entry in imageEntries) {
                docRelsXmlBuilder.append("""<Relationship Id="${entry.second}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/${entry.first}"/>""")
            }
            docRelsXmlBuilder.append("</Relationships>")

            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(docRelsXmlBuilder.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Write media files
            for (entry in imageEntries) {
                zos.putNextEntry(ZipEntry("word/media/${entry.first}"))
                zos.write(entry.third)
                zos.closeEntry()
            }

            // 3. Write word/document.xml
            val docXmlBuilder = StringBuilder()
            docXmlBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            docXmlBuilder.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">""")
            docXmlBuilder.append("<w:body>")

            // Title
            docXmlBuilder.append("<w:p>")
            docXmlBuilder.append("<w:pPr><w:jc w:val=\"center\"/></w:pPr>")
            docXmlBuilder.append("<w:r>")
            docXmlBuilder.append("<w:rPr><w:b/><w:sz w:val=\"36\"/></w:rPr>")
            docXmlBuilder.append("<w:t>${escapeXml(title)}</w:t>")
            docXmlBuilder.append("</w:r>")
            docXmlBuilder.append("</w:p>")

            // Add an empty paragraph as spacing
            docXmlBuilder.append("<w:p/>")

            // Paragraphs
            for (paragraph in paragraphs) {
                docXmlBuilder.append("<w:p>")
                docXmlBuilder.append("<w:r>")
                docXmlBuilder.append("<w:t>${escapeXml(paragraph)}</w:t>")
                docXmlBuilder.append("</w:r>")
                docXmlBuilder.append("</w:p>")
            }

            // Embedded Images
            if (imageEntries.isNotEmpty()) {
                docXmlBuilder.append("<w:p>")
                docXmlBuilder.append("<w:r><w:rPr><w:b/></w:rPr><w:t>--- ATTACHED NOTE IMAGES ---</w:t></w:r>")
                docXmlBuilder.append("</w:p>")

                imageEntries.forEachIndexed { idx, entry ->
                    docXmlBuilder.append("<w:p>")
                    docXmlBuilder.append("<w:pPr><w:jc w:val=\"center\"/></w:pPr>")
                    docXmlBuilder.append("<w:r>")
                    docXmlBuilder.append("<w:drawing>")
                    docXmlBuilder.append("""<wp:inline distT="0" distB="0" distL="0" distR="0">""")
                    docXmlBuilder.append("""<wp:extent cx="3810000" cy="2857500"/>""")
                    docXmlBuilder.append("""<wp:docPr id="${idx + 1}" name="Picture ${idx + 1}"/>""")
                    docXmlBuilder.append("<a:graphic>")
                    docXmlBuilder.append("""<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">""")
                    docXmlBuilder.append("<pic:pic>")
                    docXmlBuilder.append("<pic:nvPicPr>")
                    docXmlBuilder.append("""<pic:cNvPr id="0" name="${entry.first}"/>""")
                    docXmlBuilder.append("<pic:cNvPicPr/>")
                    docXmlBuilder.append("</pic:nvPicPr>")
                    docXmlBuilder.append("<pic:blipFill>")
                    docXmlBuilder.append("""<a:blip r:embed="${entry.second}"/>""")
                    docXmlBuilder.append("<a:stretch><a:fillRect/></a:stretch>")
                    docXmlBuilder.append("</pic:blipFill>")
                    docXmlBuilder.append("<pic:spPr>")
                    docXmlBuilder.append("<a:xfrm>")
                    docXmlBuilder.append("""<a:off x="0" y="0"/>""")
                    docXmlBuilder.append("""<a:ext cx="3810000" cy="2857500"/>""")
                    docXmlBuilder.append("</a:xfrm>")
                    docXmlBuilder.append("""<a:prstGeom prst="rect"><a:avLst/></a:prstGeom>""")
                    docXmlBuilder.append("</pic:spPr>")
                    docXmlBuilder.append("</pic:pic>")
                    docXmlBuilder.append("</a:graphicData>")
                    docXmlBuilder.append("</a:graphic>")
                    docXmlBuilder.append("</wp:inline>")
                    docXmlBuilder.append("</w:drawing>")
                    docXmlBuilder.append("</w:r>")
                    docXmlBuilder.append("</w:p>")
                }
            }

            docXmlBuilder.append("</w:body>")
            docXmlBuilder.append("</w:document>")

            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(docXmlBuilder.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

        } catch (e: Exception) {
            Log.e("DocxHelper", "Failed to package DOCX file", e)
        } finally {
            zos.close()
        }

        return baos.toByteArray()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
