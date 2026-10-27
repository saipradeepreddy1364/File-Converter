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
            else if (lower.endsWith(".pptx")) mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            else if (lower.endsWith(".ppt")) mediaType = MediaType.parseMediaType("application/vnd.ms-powerpoint");
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
                new ToolMetadata("merge-pdf", "Merge PDF", "Organize PDF", "Combine multiple PDFs in your chosen order into one document.", "Layers", "Popular", "/api/pdf/merge", Arrays.asList(".pdf"), Arrays.asList(".pdf"), true),
                new ToolMetadata("split-pdf", "Split PDF", "Organize PDF", "Extract selected pages or separate every page into a standalone PDF.", "Scissors", "Essential", "/api/pdf/split", Arrays.asList(".pdf"), Arrays.asList(".pdf", ".zip"), false),
                new ToolMetadata("remove-pages", "Remove Pages", "Organize PDF", "Delete unnecessary pages from your PDF file with custom ranges.", "Trash2", "", "/api/pdf/remove-pages", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("rotate-pdf", "Rotate PDF", "Organize PDF", "Rotate pages 90, 180, or 270 degrees permanently.", "RotateCw", "", "/api/pdf/rotate", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("page-numbers", "Add Page Numbers", "Organize PDF", "Stamp formatted page numbering with custom positioning and formatting.", "Hash", "", "/api/pdf/page-numbers", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("watermark-pdf", "Add Watermark", "Organize PDF", "Apply text or image watermarks with custom opacity and rotation angle.", "Stamp", "", "/api/pdf/watermark", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("reverse-pdf", "Reverse Page Order", "Organize PDF", "Invert the entire sequence of pages in your PDF.", "ArrowDownUp", "", "/api/pdf/reverse", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("extract-images", "Extract Images", "Organize PDF", "Extract all embedded images from a PDF into a ZIP archive.", "ImageDown", "", "/api/pdf/extract-images", Arrays.asList(".pdf"), Arrays.asList(".zip"), false),
                new ToolMetadata("extract-text", "Extract Text", "Organize PDF", "Extract all readable embedded text content into a clean text file.", "FileText", "", "/api/pdf/extract-text", Arrays.asList(".pdf"), Arrays.asList(".txt"), false)
        ));

        // 2. Optimize PDF
        catalog.put("Optimize PDF", Arrays.asList(
                new ToolMetadata("compress-pdf", "Compress PDF", "Optimize PDF", "Reduce PDF file size with Low, Medium, or High compression algorithms.", "Zap", "Popular", "/api/pdf/optimize/compress", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("repair-pdf", "Repair PDF", "Optimize PDF", "Analyze and repair damaged cross-reference tables and corrupt PDF trailers.", "Wrench", "", "/api/pdf/optimize/repair", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("flatten-pdf", "Flatten PDF", "Optimize PDF", "Flatten form fields and interactive annotations into permanent content.", "Minimize2", "", "/api/pdf/optimize/flatten", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false)
        ));

        // 3. Convert to PDF
        catalog.put("Convert to PDF", Arrays.asList(
                new ToolMetadata("pptx-to-pdf", "PowerPoint to PDF", "Convert to PDF", "Convert PPT and PPTX presentation slides into formatted PDF documents.", "Presentation", "Popular", "/api/convert/to-pdf", Arrays.asList(".pptx", ".ppt"), Arrays.asList(".pdf"), false),
                new ToolMetadata("jpg-to-pdf", "JPG / PNG to PDF", "Convert to PDF", "Transform images (JPG, PNG, WebP, BMP, TIFF) into a high-res PDF.", "FileImage", "Popular", "/api/convert/to-pdf", Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"), Arrays.asList(".pdf"), true),
                new ToolMetadata("docx-to-pdf", "DOCX to PDF", "Convert to PDF", "Convert Microsoft Word documents into clean, formatted PDF files.", "FileType", "", "/api/convert/to-pdf", Arrays.asList(".docx"), Arrays.asList(".pdf"), false),
                new ToolMetadata("xlsx-to-pdf", "Excel to PDF", "Convert to PDF", "Render spreadsheet sheets into landscape formatted PDF tables.", "Table", "", "/api/convert/to-pdf", Arrays.asList(".xlsx", ".xls"), Arrays.asList(".pdf"), false),
                new ToolMetadata("csv-to-pdf", "CSV to PDF", "Convert to PDF", "Convert comma-separated data tables into aligned PDF documents.", "FileSpreadsheet", "", "/api/convert/to-pdf", Arrays.asList(".csv"), Arrays.asList(".pdf"), false),
                new ToolMetadata("txt-to-pdf", "TXT to PDF", "Convert to PDF", "Convert plain text documents with word-wrap and pagination into PDF.", "FileText", "", "/api/convert/to-pdf", Arrays.asList(".txt"), Arrays.asList(".pdf"), false),
                new ToolMetadata("md-to-pdf", "Markdown to PDF", "Convert to PDF", "Parse Markdown files and compile into clean PDF format.", "BookOpen", "", "/api/convert/to-pdf", Arrays.asList(".md", ".markdown"), Arrays.asList(".pdf"), false)
        ));

        // 4. Convert from PDF
        catalog.put("Convert from PDF", Arrays.asList(
                new ToolMetadata("pdf-to-pptx", "PDF to PowerPoint (PPTX)", "Convert from PDF", "Convert PDF pages into high-resolution editable presentation slides.", "Presentation", "Popular", "/api/convert/from-pdf", Arrays.asList(".pdf"), Arrays.asList(".pptx"), false),
                new ToolMetadata("pdf-to-jpg", "PDF to Images", "Convert from PDF", "Render every PDF page into high-resolution JPG, PNG, or WebP images.", "Image", "Popular", "/api/convert/from-pdf", Arrays.asList(".pdf"), Arrays.asList(".zip", ".jpg", ".png", ".webp"), false),
                new ToolMetadata("pdf-to-docx", "PDF to DOCX", "Convert from PDF", "Convert PDF to editable Word document using structural parser & OCR.", "FileWord", "Popular", "/api/convert/from-pdf", Arrays.asList(".pdf"), Arrays.asList(".docx"), false),
                new ToolMetadata("pdf-to-txt", "PDF to TXT", "Convert from PDF", "Extract clean plain text with automatic OCR fallback for scanned pages.", "FileText", "", "/api/convert/from-pdf", Arrays.asList(".pdf"), Arrays.asList(".txt"), false),
                new ToolMetadata("pdf-to-markdown", "PDF to Markdown", "Convert from PDF", "Convert PDF paragraphs, headings, and lists into structured Markdown.", "Code", "", "/api/convert/from-pdf", Arrays.asList(".pdf"), Arrays.asList(".md"), false),
                new ToolMetadata("pdf-to-html", "PDF to HTML", "Convert from PDF", "Transform PDF documents into responsive HTML web pages.", "Globe", "", "/api/convert/from-pdf", Arrays.asList(".pdf"), Arrays.asList(".html"), false),
                new ToolMetadata("pdf-to-tables", "PDF Tables to Excel/CSV", "Convert from PDF", "Extract structured tabular data from PDF into CSV or XLSX spreadsheets.", "Grid", "", "/api/convert/from-pdf", Arrays.asList(".pdf"), Arrays.asList(".csv", ".xlsx"), false)
        ));

        // 5. PDF Security
        catalog.put("PDF Security", Arrays.asList(
                new ToolMetadata("protect-pdf", "Protect PDF", "PDF Security", "Encrypt PDF with 128-bit AES password and restrict modification.", "Lock", "Security", "/api/pdf/security/protect", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("unlock-pdf", "Unlock PDF", "PDF Security", "Remove password restrictions from authorized password-protected PDFs.", "Unlock", "", "/api/pdf/security/unlock", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("redact-pdf", "Redact PDF", "PDF Security", "Permanently redact sensitive rectangular zones and scrub underlying content.", "EyeOff", "Security", "/api/pdf/security/redact", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("compare-pdf", "Compare PDFs", "PDF Security", "Diff two PDF documents and calculate semantic similarity score.", "GitCompare", "", "/api/pdf/security/compare", Arrays.asList(".pdf"), Arrays.asList(".json"), false),
                new ToolMetadata("remove-metadata", "Remove Metadata", "PDF Security", "Scrub author, creator, GPS, and creation timestamps for privacy.", "ShieldCheck", "", "/api/pdf/security/remove-metadata", Arrays.asList(".pdf"), Arrays.asList(".pdf"), false),
                new ToolMetadata("inspect-metadata", "Inspect Metadata", "PDF Security", "Examine embedded document properties, encryption, and PDF version.", "Info", "", "/api/pdf/security/metadata", Arrays.asList(".pdf"), Arrays.asList(".json"), false)
        ));

        // 6. PDF Intelligence and AI
        catalog.put("PDF Intelligence and AI", Arrays.asList(
                new ToolMetadata("ai-summarize", "Document Summarizer", "PDF Intelligence and AI", "Extractive NLP summarization identifying the most salient passages.", "Sparkles", "AI Local", "/api/ai/summarize", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("ai-qa", "Document Q&A", "PDF Intelligence and AI", "Ask questions about document contents and retrieve matched evidence.", "MessageSquare", "AI Local", "/api/ai/qa", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("ai-entities", "Extract Entities & Dates", "PDF Intelligence and AI", "Automatically discover emails, dates, phone numbers, and amounts.", "Search", "AI Local", "/api/ai/entities", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("ai-study-notes", "Study Notes & Flashcards", "PDF Intelligence and AI", "Generate interactive question/answer study flashcards from documents.", "GraduationCap", "AI Local", "/api/ai/study-notes", Arrays.asList(".pdf", ".txt"), Arrays.asList(".json"), false)
        ));

        // 7. Image Tools
        catalog.put("Image Tools", Arrays.asList(
                new ToolMetadata("convert-image", "Image Format Converter", "Image Tools", "Convert JPG, PNG, WebP, BMP, and TIFF with transparency preservation.", "Repeat", "Essential", "/api/convert/image/format", Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"), Arrays.asList(".png", ".jpg", ".webp", ".bmp"), false),
                new ToolMetadata("resize-image", "Resize Image", "Image Tools", "Change image dimensions with high-quality bicubic interpolation.", "Maximize", "", "/api/convert/image/resize", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false),
                new ToolMetadata("crop-image", "Crop Image", "Image Tools", "Crop precise pixel bounding boxes from any image.", "Crop", "", "/api/convert/image/crop", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false),
                new ToolMetadata("rotate-image", "Rotate & Flip Image", "Image Tools", "Rotate 90/180/270 degrees and mirror horizontally or vertically.", "RefreshCw", "", "/api/convert/image/rotate", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false),
                new ToolMetadata("compress-image", "Compress Image", "Image Tools", "Reduce file size with fine-grained lossy or lossless compression.", "Sliders", "", "/api/convert/image/compress", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".jpg", ".png", ".webp"), false)
        ));

        // 8. Spreadsheet and Data Tools
        catalog.put("Spreadsheet and Data Tools", Arrays.asList(
                new ToolMetadata("csv-to-xlsx", "CSV to Excel (XLSX)", "Spreadsheet and Data Tools", "Convert CSV files into genuine Microsoft Excel workbooks.", "Table", "Popular", "/api/data/csv-to-xlsx", Arrays.asList(".csv"), Arrays.asList(".xlsx"), false),
                new ToolMetadata("xlsx-to-csv", "Excel to CSV", "Spreadsheet and Data Tools", "Export any Excel sheet into comma-separated values format.", "FileSpreadsheet", "", "/api/data/xlsx-to-csv", Arrays.asList(".xlsx", ".xls"), Arrays.asList(".csv"), false),
                new ToolMetadata("csv-to-json", "CSV to JSON", "Spreadsheet and Data Tools", "Transform tabular records into structured JSON array of objects.", "Braces", "", "/api/data/csv-to-json", Arrays.asList(".csv"), Arrays.asList(".json"), false),
                new ToolMetadata("json-to-csv", "JSON to CSV", "Spreadsheet and Data Tools", "Flatten JSON data arrays into spreadsheet-compatible CSV.", "FileSpreadsheet", "", "/api/data/json-to-csv", Arrays.asList(".json"), Arrays.asList(".csv"), false),
                new ToolMetadata("format-json", "Format & Validate JSON", "Spreadsheet and Data Tools", "Pretty-print, indent, and validate JSON syntax.", "CheckCircle", "", "/api/data/format-json", Arrays.asList(".json", ".txt"), Arrays.asList(".json"), false),
                new ToolMetadata("xml-to-json", "XML to JSON & Vice Versa", "Spreadsheet and Data Tools", "Bi-directional conversion between XML data trees and JSON.", "Binary", "", "/api/data/xml-to-json", Arrays.asList(".xml", ".json"), Arrays.asList(".json", ".xml"), false)
        ));

        // 9. OCR and Scanned Document Tools (In-House Engine)
        catalog.put("OCR and Scanned Document Tools", Arrays.asList(
                new ToolMetadata("ocr-studio", "In-House OCR Studio", "OCR and Scanned Document Tools", "Extract text, detect bounding boxes, and deskew scanned PDFs or images.", "ScanText", "Core Engine", "/api/ocr/process", Arrays.asList(".pdf", ".jpg", ".png", ".webp", ".tiff", ".bmp"), Arrays.asList(".txt", ".docx", ".pdf", ".json"), false),
                new ToolMetadata("ocr-searchable-pdf", "Scanned PDF to Searchable PDF", "OCR and Scanned Document Tools", "Overlay an invisible text layer to make scanned PDFs searchable and selectable.", "Search", "In-House", "/api/ocr/process", Arrays.asList(".pdf", ".jpg", ".png"), Arrays.asList(".pdf"), false),
                new ToolMetadata("ocr-to-docx", "Scanned PDF / Image to DOCX", "OCR and Scanned Document Tools", "Convert image-based documents into editable Word (.docx) files.", "FileWord", "In-House", "/api/ocr/process", Arrays.asList(".pdf", ".jpg", ".png", ".webp"), Arrays.asList(".docx"), false),
                new ToolMetadata("ocr-evaluate", "OCR Engine Evaluation & Benchmarks", "OCR and Scanned Document Tools", "Run genuine CER (Character Error Rate) and WER benchmarks on labeled datasets.", "BarChart2", "Evaluation", "/api/ocr/evaluate", Collections.emptyList(), Arrays.asList(".json"), false)
        ));

        // 10. Archive and Utility Tools
        catalog.put("Archive and Utility Tools", Arrays.asList(
                new ToolMetadata("create-zip", "Create ZIP Archive", "Archive and Utility Tools", "Compress multiple files into a secure ZIP archive.", "Archive", "", "/api/tools/create-zip", Collections.emptyList(), Arrays.asList(".zip"), true),
                new ToolMetadata("extract-zip", "Extract ZIP Archive", "Archive and Utility Tools", "Safely unpack ZIP files with path traversal and zip-bomb protection.", "FolderArchive", "", "/api/tools/extract-zip", Arrays.asList(".zip"), Collections.emptyList(), false),
                new ToolMetadata("file-hashes", "File Hash Calculator", "Archive and Utility Tools", "Compute cryptographic MD5, SHA-1, and SHA-256 hashes of any file.", "Fingerprint", "Security", "/api/tools/hashes", Collections.emptyList(), Arrays.asList(".json"), false),
                new ToolMetadata("file-metadata", "EXIF & Metadata Inspector", "Archive and Utility Tools", "Inspect camera, GPS, and technical header tags of documents & images.", "Tag", "", "/api/tools/metadata", Collections.emptyList(), Arrays.asList(".json"), false),
                new ToolMetadata("word-count", "Word & Character Counter", "Archive and Utility Tools", "Count words, characters, sentences, paragraphs, and reading time.", "FileBarChart", "", "/api/tools/word-count", Arrays.asList(".txt", ".md", ".pdf"), Arrays.asList(".json"), false),
                new ToolMetadata("qr-generate", "QR Code Generator", "Archive and Utility Tools", "Generate high-resolution QR codes from URLs or text with error correction.", "QrCode", "", "/api/tools/qr/generate", Collections.emptyList(), Arrays.asList(".png"), false),
                new ToolMetadata("qr-scan", "QR Code Scanner", "Archive and Utility Tools", "Decode and read QR codes and barcodes from uploaded images.", "Scan", "", "/api/tools/qr/scan", Arrays.asList(".jpg", ".png", ".webp"), Arrays.asList(".json"), false)
        ));

        return ResponseEntity.ok(catalog);
    }
}
