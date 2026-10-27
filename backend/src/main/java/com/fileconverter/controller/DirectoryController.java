package com.fileconverter.controller;

import com.fileconverter.model.ToolMetadata;
import com.fileconverter.service.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@RestController
@CrossOrigin(originPatterns = "*")
public class DirectoryController {

    private final StorageService storageService;

    @Autowired
    public DirectoryController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/api/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        try {
            Resource resource = storageService.loadAsResource(filename);
            String lower = filename.toLowerCase();
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

            if (lower.endsWith(".pdf")) mediaType = MediaType.APPLICATION_PDF;
            else if (lower.endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mediaType = MediaType.IMAGE_JPEG;
            else if (lower.endsWith(".webp")) mediaType = MediaType.parseMediaType("image/webp");
            else if (lower.endsWith(".docx")) mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            else if (lower.endsWith(".xlsx")) mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            else if (lower.endsWith(".txt")) mediaType = MediaType.TEXT_PLAIN;
            else if (lower.endsWith(".html")) mediaType = MediaType.TEXT_HTML;
            else if (lower.endsWith(".json")) mediaType = MediaType.APPLICATION_JSON;
            else if (lower.endsWith(".zip")) mediaType = MediaType.parseMediaType("application/zip");

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/tools/catalog")
    public ResponseEntity<Map<String, List<ToolMetadata>>> getToolCatalog() {
        Map<String, List<ToolMetadata>> catalog = new LinkedHashMap<>();

        // 1. Organize PDF
        catalog.put("Organize PDF", Arrays.asList(
                new ToolMetadata("merge-pdf", "Merge PDF", "Organize PDF", "Combine multiple PDFs in your chosen order into one document.", "Layers", "Popular", "/tool/merge-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), true),
                new ToolMetadata("split-pdf", "Split PDF", "Organize PDF", "Extract selected pages or separate every page into a standalone PDF.", "Scissors", "Essential", "/tool/split-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf", ".zip"), false),
                new ToolMetadata("remove-pages", "Remove Pages", "Organize PDF", "Delete unnecessary pages from your PDF file with custom ranges.", "Trash2", "", "/tool/remove-pages", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("rotate-pdf", "Rotate PDF", "Organize PDF", "Rotate pages 90, 180, or 270 degrees permanently.", "RotateCw", "", "/tool/rotate-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("page-numbers", "Add Page Numbers", "Organize PDF", "Stamp formatted page numbering with custom positioning and formatting.", "Hash", "", "/tool/page-numbers", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("watermark-pdf", "Add Watermark", "Organize PDF", "Apply text or image watermarks with custom opacity and rotation angle.", "Stamp", "", "/tool/watermark-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("reverse-pdf", "Reverse Page Order", "Organize PDF", "Invert the entire sequence of pages in your PDF.", "ArrowDownUp", "", "/tool/reverse-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("extract-images", "Extract Images", "Organize PDF", "Extract all embedded images from a PDF into a ZIP archive.", "ImageDown", "", "/tool/extract-images", Arrays.asList(".pdf"), Arrays.asList(".zip"), false),
                new ToolMetadata("extract-text", "Extract Text", "Organize PDF", "Extract all readable embedded text content into a clean text file.", "FileText", "", "/tool/extract-text", Arrays.asList(".pdf"), Arrays.asList(".txt"), false)
        ));

        // 2. Optimize PDF
        catalog.put("Optimize PDF", Arrays.asList(
                new ToolMetadata("compress-pdf", "Compress PDF", "Optimize PDF", "Reduce PDF file size with Low, Medium, or High compression algorithms.", "Zap", "Popular", "/tool/compress-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("repair-pdf", "Repair PDF", "Optimize PDF", "Analyze and repair damaged cross-reference tables and corrupt PDF trailers.", "Wrench", "", "/tool/repair-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("flatten-pdf", "Flatten PDF", "Optimize PDF", "Flatten form fields and interactive annotations into permanent content.", "Minimize2", "", "/tool/flatten-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false)
        ));

        // 3. Convert to PDF
        catalog.put("Convert to PDF", Arrays.asList(
                new ToolMetadata("jpg-to-pdf", "JPG / PNG to PDF", "Convert to PDF", "Transform images (JPG, PNG, WebP, BMP, TIFF) into a high-res PDF.", "FileImage", "Popular", "/tool/jpg-to-pdf", Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"), Arrays.asList(".pdf"), true),
                new ToolMetadata("docx-to-pdf", "DOCX to PDF", "Convert to PDF", "Convert Microsoft Word documents into clean, formatted PDF files.", "FileType", "", "/tool/docx-to-pdf", Arrays.asList(".docx"), Arrays.asList(".pdf"), false),
                new ToolMetadata("xlsx-to-pdf", "Excel to PDF", "Convert to PDF", "Render spreadsheet sheets into landscape formatted PDF tables.", "Table", "", "/tool/xlsx-to-pdf", Arrays.asList(".xlsx", ".xls"), Arrays.asList(".pdf"), false),
                new ToolMetadata("csv-to-pdf", "CSV to PDF", "Convert to PDF", "Convert comma-separated data tables into aligned PDF documents.", "FileSpreadsheet", "", "/tool/csv-to-pdf", Arrays.asList(".csv"), Arrays.asList(".pdf"), false),
                new ToolMetadata("txt-to-pdf", "TXT to PDF", "Convert to PDF", "Convert plain text documents with word-wrap and pagination into PDF.", "FileText", "", "/tool/txt-to-pdf", Arrays.asList(".txt"), Arrays.asList(".pdf"), false),
                new ToolMetadata("md-to-pdf", "Markdown to PDF", "Convert to PDF", "Parse Markdown files and compile into clean PDF format.", "BookOpen", "", "/tool/md-to-pdf", Arrays.asList(".md", ".markdown"), Arrays.asList(".pdf"), false)
        ));

        // 4. Convert from PDF
        catalog.put("Convert from PDF", Arrays.asList(
                new ToolMetadata("pdf-to-jpg", "PDF to Images", "Convert from PDF", "Render every PDF page into high-resolution JPG, PNG, or WebP images.", "Image", "Popular", "/tool/pdf-to-jpg", Arrays.asList(".pdf"), Arrays.asList(".zip", ".jpg", ".png", ".webp"), false),
                new ToolMetadata("pdf-to-docx", "PDF to DOCX", "Convert from PDF", "Convert PDF to editable Word document using structural parser & OCR.", "FileWord", "Popular", "/tool/pdf-to-docx", Arrays.asList(".pdf"), Arrays.asList(".docx"), false),
                new ToolMetadata("pdf-to-txt", "PDF to TXT", "Convert from PDF", "Extract clean plain text with automatic OCR fallback for scanned pages.", "FileText", "", "/tool/pdf-to-txt", Arrays.asList(".pdf"), Arrays.asList(".txt"), false),
                new ToolMetadata("pdf-to-markdown", "PDF to Markdown", "Convert from PDF", "Convert PDF paragraphs, headings, and lists into structured Markdown.", "Code", "", "/tool/pdf-to-markdown", Arrays.asList(".pdf"), Arrays.asList(".md"), false),
                new ToolMetadata("pdf-to-html", "PDF to HTML", "Convert from PDF", "Transform PDF documents into responsive HTML web pages.", "Globe", "", "/tool/pdf-to-html", Arrays.asList(".pdf"), Arrays.asList(".html"), false),
                new ToolMetadata("pdf-to-tables", "PDF Tables to Excel/CSV", "Convert from PDF", "Extract structured tabular data from PDF into CSV or XLSX spreadsheets.", "Grid", "", "/tool/pdf-to-tables", Arrays.asList(".pdf"), Arrays.asList(".csv", ".xlsx"), false)
        ));

        // 5. Edit PDF
        catalog.put("Edit PDF", Arrays.asList(
                new ToolMetadata("edit-text", "Add Text to PDF", "Edit PDF", "Stamp custom text with custom font size, color, and coordinates.", "Type", "", "/tool/edit-text", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("edit-highlight", "Highlight PDF", "Edit PDF", "Add translucent yellow highlight boxes over important passages.", "Highlighter", "", "/tool/edit-highlight", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("edit-image", "Add Image / Signature", "Edit PDF", "Insert image stamps, logos, or handwritten signatures onto any page.", "Signature", "", "/tool/edit-image", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false)
        ));

        // 6. PDF Security
        catalog.put("PDF Security", Arrays.asList(
                new ToolMetadata("protect-pdf", "Protect PDF", "PDF Security", "Encrypt PDF with 128-bit AES password and restrict modification.", "Lock", "Security", "/tool/protect-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("unlock-pdf", "Unlock PDF", "PDF Security", "Remove password restrictions from authorized password-protected PDFs.", "Unlock", "", "/tool/unlock-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("redact-pdf", "Redact PDF", "PDF Security", "Permanently redact sensitive rectangular zones and scrub underlying content.", "EyeOff", "Security", "/tool/redact-pdf", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("compare-pdf", "Compare PDFs", "PDF Security", "Diff two PDF documents and calculate semantic similarity score.", "GitCompare", "", "/tool/compare-pdf", Arrays.asList(".pdf"), Arrays.asList(".json"), false),
                new ToolMetadata("remove-metadata", "Remove Metadata", "PDF Security", "Scrub author, creator, GPS, and creation timestamps for privacy.", "ShieldCheck", "", "/tool/remove-metadata", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("inspect-metadata", "Inspect Metadata", "PDF Security", "Examine embedded document properties, encryption, and PDF version.", "Info", "", "/tool/inspect-metadata", Arrays.asList(".pdf"), Arrays.asList(".json"), false)
        ));

        // 7. PDF Intelligence and AI
        catalog.put("PDF Intelligence and AI", Arrays.asList(
                new ToolMetadata("ai-summarize", "Document Summarizer", "PDF Intelligence and AI", "Extractive NLP summarization identifying the most salient passages.", "Sparkles", "AI Local", "/tool/ai-summarize", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("ai-qa", "Document Q&A", "PDF Intelligence and AI", "Ask questions about document contents and retrieve matched evidence.", "MessageSquare", "AI Local", "/tool/ai-qa", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("ai-entities", "Extract Entities & Dates", "PDF Intelligence and AI", "Automatically discover emails, dates, phone numbers, and amounts.", "Search", "AI Local", "/tool/ai-entities", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("ai-study-notes", "Study Notes & Flashcards", "PDF Intelligence and AI", "Generate interactive question/answer study flashcards from documents.", "GraduationCap", "AI Local", "/tool/ai-study-notes", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false)
        ));

        // 8. Image Tools
        catalog.put("Image Tools", Arrays.asList(
                new ToolMetadata("convert-image", "Image Format Converter", "Image Tools", "Convert JPG, PNG, WebP, BMP, and TIFF with transparency preservation.", "Repeat", "Essential", "/tool/convert-image", Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"), Arrays.asList(".png", ".jpg", ".webp", ".bmp"), false),
                new ToolMetadata("resize-image", "Resize Image", "Image Tools", "Change image dimensions with high-quality bicubic interpolation.", "Maximize", "", "/tool/resize-image", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false),
                new ToolMetadata("crop-image", "Crop Image", "Image Tools", "Crop precise pixel bounding boxes from any image.", "Crop", "", "/tool/crop-image", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false),
                new ToolMetadata("rotate-image", "Rotate & Flip Image", "Image Tools", "Rotate 90/180/270 degrees and mirror horizontally or vertically.", "RefreshCw", "", "/tool/rotate-image", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false),
                new ToolMetadata("compress-image", "Compress Image", "Image Tools", "Reduce file size with fine-grained lossy or lossless compression.", "Sliders", "", "/tool/compress-image", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false)
        ));

        // 9. Document Tools
        catalog.put("Document Tools", Arrays.asList(
                new ToolMetadata("docx-to-txt", "DOCX to TXT", "Document Tools", "Extract clean text paragraphs from Word DOCX files.", "FileText", "", "/tool/docx-to-txt", Arrays.asList(".docx"), Arrays.asList(".txt"), false),
                new ToolMetadata("docx-to-html", "DOCX to HTML", "Document Tools", "Convert styled Word documents into formatted HTML pages.", "FileCode", "", "/tool/docx-to-html", Arrays.asList(".docx"), Arrays.asList(".html"), false),
                new ToolMetadata("txt-to-docx", "TXT to DOCX", "Document Tools", "Turn plain text files into editable Microsoft Word documents.", "FileWord", "", "/tool/txt-to-docx", Arrays.asList(".txt"), Arrays.asList(".docx"), false),
                new ToolMetadata("md-to-html", "Markdown to HTML", "Document Tools", "Convert Markdown syntax, headings, and tables into styled HTML.", "Code", "", "/tool/md-to-html", Arrays.asList(".md"), Arrays.asList(".html"), false),
                new ToolMetadata("html-to-md", "HTML to Markdown", "Document Tools", "Strip HTML tags and convert clean structure back to Markdown.", "Terminal", "", "/tool/html-to-md", Arrays.asList(".html"), Arrays.asList(".md"), false)
        ));

        // 10. Spreadsheet and Data Tools
        catalog.put("Spreadsheet and Data Tools", Arrays.asList(
                new ToolMetadata("csv-to-xlsx", "CSV to Excel (XLSX)", "Spreadsheet and Data Tools", "Convert CSV files into genuine Microsoft Excel workbooks.", "Table", "Popular", "/tool/csv-to-xlsx", Arrays.asList(".csv"), Arrays.asList(".xlsx"), false),
                new ToolMetadata("xlsx-to-csv", "Excel to CSV", "Spreadsheet and Data Tools", "Export any Excel sheet into comma-separated values format.", "FileSpreadsheet", "", "/tool/xlsx-to-csv", Arrays.asList(".xlsx", ".xls"), Arrays.asList(".csv"), false),
                new ToolMetadata("csv-to-json", "CSV to JSON", "Spreadsheet and Data Tools", "Transform tabular records into structured JSON array of objects.", "Braces", "", "/tool/csv-to-json", Arrays.asList(".csv"), Arrays.asList(".json"), false),
                new ToolMetadata("json-to-csv", "JSON to CSV", "Spreadsheet and Data Tools", "Flatten JSON data arrays into spreadsheet-compatible CSV.", "FileSpreadsheet", "", "/tool/json-to-csv", Arrays.asList(".json"), Arrays.asList(".csv"), false),
                new ToolMetadata("format-json", "Format & Validate JSON", "Spreadsheet and Data Tools", "Pretty-print, indent, and validate JSON syntax.", "CheckCircle", "", "/tool/format-json", Arrays.asList(".json", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("xml-to-json", "XML to JSON & Vice Versa", "Spreadsheet and Data Tools", "Bi-directional conversion between XML data trees and JSON.", "Binary", "", "/tool/xml-to-json", Arrays.asList(".xml", ".json"), Arrays.asList(".json", ".xml"), false)
        ));

        // 11. OCR and Scanned Document Tools (In-House Engine)
        catalog.put("OCR and Scanned Document Tools", Arrays.asList(
                new ToolMetadata("ocr-studio", "In-House OCR Studio", "OCR and Scanned Document Tools", "Extract text, detect bounding boxes, and deskew scanned PDFs or images.", "ScanText", "Core Engine", "/tool/ocr-studio", Arrays.asList(".pdf", ".jpg", ".png", ".webp", ".tiff", ".bmp"), Arrays.asList(".txt", ".docx", ".pdf", ".json"), false),
                new ToolMetadata("ocr-searchable-pdf", "Scanned PDF to Searchable PDF", "OCR and Scanned Document Tools", "Overlay an invisible text layer to make scanned PDFs searchable and selectable.", "Search", "In-House", "/tool/ocr-searchable-pdf", Arrays.asList(".pdf", ".jpg", ".png"), Arrays.asList(".pdf"), false),
                new ToolMetadata("ocr-to-docx", "Scanned PDF / Image to DOCX", "OCR and Scanned Document Tools", "Convert image-based documents into editable Word (.docx) files.", "FileWord", "In-House", "/tool/ocr-to-docx", Arrays.asList(".pdf", ".jpg", ".png", ".webp"), Arrays.asList(".docx"), false),
                new ToolMetadata("ocr-evaluate", "OCR Engine Evaluation & Benchmarks", "OCR and Scanned Document Tools", "Run genuine CER (Character Error Rate) and WER benchmarks on labeled datasets.", "BarChart2", "Evaluation", "/tool/ocr-evaluate", Collections.emptyList(), Arrays.asList(".json"), false)
        ));

        // 12. Archive and Utility Tools
        catalog.put("Archive and Utility Tools", Arrays.asList(
                new ToolMetadata("create-zip", "Create ZIP Archive", "Archive and Utility Tools", "Compress multiple files into a secure ZIP archive.", "Archive", "", "/tool/create-zip", Collections.emptyList(), Arrays.asList(".zip"), true),
                new ToolMetadata("extract-zip", "Extract ZIP Archive", "Archive and Utility Tools", "Safely unpack ZIP files with path traversal and zip-bomb protection.", "FolderArchive", "", "/tool/extract-zip", Arrays.asList(".zip"), Collections.emptyList(), false),
                new ToolMetadata("file-hashes", "File Hash Calculator", "Archive and Utility Tools", "Compute cryptographic MD5, SHA-1, and SHA-256 hashes of any file.", "Fingerprint", "Security", "/tool/file-hashes", Collections.emptyList(), Arrays.asList(".json"), false),
                new ToolMetadata("file-metadata", "EXIF & Metadata Inspector", "Archive and Utility Tools", "Inspect camera, GPS, and technical header tags of documents & images.", "Tag", "", "/tool/file-metadata", Collections.emptyList(), Arrays.asList(".json"), false),
                new ToolMetadata("word-count", "Word & Character Counter", "Archive and Utility Tools", "Count words, characters, sentences, paragraphs, and reading time.", "FileBarChart", "", "/tool/word-count", Arrays.asList(".txt", ".md", ".pdf"), Arrays.asList(".json"), false),
                new ToolMetadata("qr-generate", "QR Code Generator", "Archive and Utility Tools", "Generate high-resolution QR codes from URLs or text with error correction.", "QrCode", "", "/tool/qr-generate", Collections.emptyList(), Arrays.asList(".png"), false),
                new ToolMetadata("qr-scan", "QR Code Scanner", "Archive and Utility Tools", "Decode and read QR codes and barcodes from uploaded images.", "Scan", "", "/tool/qr-scan", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".json"), false)
        ));

        return ResponseEntity.ok(catalog);
    }
}
