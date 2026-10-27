import type { ToolMetadata } from '../types';

export const DEFAULT_CATALOG: Record<string, ToolMetadata[]> = {
  "Convert to PDF": [
    {
      id: "pptx-to-pdf",
      name: "PowerPoint to PDF",
      category: "Convert to PDF",
      description: "Convert PPT and PPTX presentation slides into formatted PDF documents.",
      icon: "Presentation",
      badge: "Popular",
      route: "/api/convert/to-pdf",
      acceptedFormats: [".pptx", ".ppt"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "jpg-to-pdf",
      name: "Images to PDF",
      category: "Convert to PDF",
      description: "Transform images (JPG, PNG, WebP, BMP) into a high-resolution PDF.",
      icon: "FileImage",
      badge: "Popular",
      route: "/api/convert/to-pdf",
      acceptedFormats: [".jpg", ".jpeg", ".png", ".webp", ".bmp"],
      outputFormats: [".pdf"],
      isMultiFile: true,
      multiFile: true
    },
    {
      id: "docx-to-pdf",
      name: "Word (DOCX) to PDF",
      category: "Convert to PDF",
      description: "Convert Microsoft Word documents into clean, formatted PDF files.",
      icon: "FileType",
      route: "/api/convert/to-pdf",
      acceptedFormats: [".docx"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "xlsx-to-pdf",
      name: "Excel to PDF",
      category: "Convert to PDF",
      description: "Render spreadsheet sheets into landscape formatted PDF tables.",
      icon: "Table",
      route: "/api/convert/to-pdf",
      acceptedFormats: [".xlsx", ".xls"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "csv-to-pdf",
      name: "CSV to PDF",
      category: "Convert to PDF",
      description: "Convert comma-separated data tables into aligned PDF documents.",
      icon: "FileSpreadsheet",
      route: "/api/convert/to-pdf",
      acceptedFormats: [".csv"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "txt-to-pdf",
      name: "TXT to PDF",
      category: "Convert to PDF",
      description: "Convert plain text documents with word-wrap and pagination into PDF.",
      icon: "FileText",
      route: "/api/convert/to-pdf",
      acceptedFormats: [".txt"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "md-to-pdf",
      name: "Markdown to PDF",
      category: "Convert to PDF",
      description: "Parse Markdown files and compile into clean PDF format.",
      icon: "BookOpen",
      route: "/api/convert/to-pdf",
      acceptedFormats: [".md", ".markdown"],
      outputFormats: [".pdf"],
      isMultiFile: false
    }
  ],
  "Organize PDF": [
    {
      id: "merge-pdf",
      name: "Merge PDF",
      category: "Organize PDF",
      description: "Combine multiple PDFs in your chosen order into one document.",
      icon: "Layers",
      badge: "Popular",
      route: "/api/pdf/merge",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: true,
      multiFile: true
    },
    {
      id: "split-pdf",
      name: "Split PDF",
      category: "Organize PDF",
      description: "Extract selected pages or separate every page into a standalone PDF.",
      icon: "Scissors",
      badge: "Essential",
      route: "/api/pdf/split",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf", ".zip"],
      isMultiFile: false
    },
    {
      id: "remove-pages",
      name: "Remove Pages",
      category: "Organize PDF",
      description: "Delete unnecessary pages from your PDF file with custom page numbers.",
      icon: "Trash2",
      route: "/api/pdf/remove-pages",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "rotate-pdf",
      name: "Rotate PDF",
      category: "Organize PDF",
      description: "Rotate pages 90, 180, or 270 degrees clockwise permanently.",
      icon: "RotateCw",
      route: "/api/pdf/rotate",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "page-numbers",
      name: "Add Page Numbers",
      category: "Organize PDF",
      description: "Stamp formatted page numbering with custom positioning and formatting.",
      icon: "Hash",
      route: "/api/pdf/page-numbers",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "watermark-pdf",
      name: "Add Watermark",
      category: "Organize PDF",
      description: "Apply text watermarks with custom opacity and rotation angle.",
      icon: "Stamp",
      route: "/api/pdf/watermark",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "reverse-pdf",
      name: "Reverse Page Order",
      category: "Organize PDF",
      description: "Invert the entire sequence of pages in your PDF instantly.",
      icon: "ArrowDownUp",
      route: "/api/pdf/reverse",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "extract-images",
      name: "Extract Images",
      category: "Organize PDF",
      description: "Extract all embedded images from a PDF into a ZIP archive.",
      icon: "ImageDown",
      route: "/api/pdf/extract-images",
      acceptedFormats: [".pdf"],
      outputFormats: [".zip"],
      isMultiFile: false
    },
    {
      id: "extract-text",
      name: "Extract Text",
      category: "Organize PDF",
      description: "Extract all readable embedded text content into a clean text file.",
      icon: "FileText",
      route: "/api/pdf/extract-text",
      acceptedFormats: [".pdf"],
      outputFormats: [".txt"],
      isMultiFile: false
    }
  ],
  "Optimize PDF": [
    {
      id: "compress-pdf",
      name: "Compress PDF",
      category: "Optimize PDF",
      description: "Reduce PDF file size with Low, Medium, or High compression algorithms.",
      icon: "Zap",
      badge: "Popular",
      route: "/api/pdf/optimize/compress",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "repair-pdf",
      name: "Repair PDF",
      category: "Optimize PDF",
      description: "Analyze and repair damaged cross-reference tables and corrupt PDF trailers.",
      icon: "Wrench",
      route: "/api/pdf/optimize/repair",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "flatten-pdf",
      name: "Flatten PDF",
      category: "Optimize PDF",
      description: "Flatten form fields and interactive annotations into permanent content.",
      icon: "Minimize2",
      route: "/api/pdf/optimize/flatten",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    }
  ],
  "Convert from PDF": [
    {
      id: "pdf-to-pptx",
      name: "PDF to PowerPoint (PPTX)",
      category: "Convert from PDF",
      description: "Convert PDF pages into high-resolution editable presentation slides.",
      icon: "Presentation",
      badge: "Popular",
      route: "/api/convert/from-pdf",
      acceptedFormats: [".pdf"],
      outputFormats: [".pptx"],
      isMultiFile: false
    },
    {
      id: "pdf-to-jpg",
      name: "PDF to Images",
      category: "Convert from PDF",
      description: "Render every PDF page into high-resolution JPG, PNG, or WebP images.",
      icon: "Image",
      badge: "Popular",
      route: "/api/convert/from-pdf",
      acceptedFormats: [".pdf"],
      outputFormats: [".zip", ".jpg", ".png", ".webp"],
      isMultiFile: false
    },
    {
      id: "pdf-to-docx",
      name: "PDF to Word (DOCX)",
      category: "Convert from PDF",
      description: "Convert PDF to editable Word document using structural parser & OCR.",
      icon: "FileWord",
      badge: "Popular",
      route: "/api/convert/from-pdf",
      acceptedFormats: [".pdf"],
      outputFormats: [".docx"],
      isMultiFile: false
    },
    {
      id: "pdf-to-txt",
      name: "PDF to Text",
      category: "Convert from PDF",
      description: "Extract clean plain text with automatic OCR fallback for scanned pages.",
      icon: "FileText",
      route: "/api/convert/from-pdf",
      acceptedFormats: [".pdf"],
      outputFormats: [".txt"],
      isMultiFile: false
    },
    {
      id: "pdf-to-markdown",
      name: "PDF to Markdown",
      category: "Convert from PDF",
      description: "Convert PDF paragraphs, headings, and lists into structured Markdown.",
      icon: "Code",
      route: "/api/convert/from-pdf",
      acceptedFormats: [".pdf"],
      outputFormats: [".md"],
      isMultiFile: false
    },
    {
      id: "pdf-to-html",
      name: "PDF to HTML",
      category: "Convert from PDF",
      description: "Transform PDF documents into responsive HTML web pages.",
      icon: "Globe",
      route: "/api/convert/from-pdf",
      acceptedFormats: [".pdf"],
      outputFormats: [".html"],
      isMultiFile: false
    },
    {
      id: "pdf-to-tables",
      name: "PDF Tables to Excel/CSV",
      category: "Convert from PDF",
      description: "Extract structured tabular data from PDF into CSV or XLSX spreadsheets.",
      icon: "Grid",
      route: "/api/convert/from-pdf",
      acceptedFormats: [".pdf"],
      outputFormats: [".csv", ".xlsx"],
      isMultiFile: false
    }
  ],
  "PDF Security": [
    {
      id: "protect-pdf",
      name: "Protect PDF (Encrypt)",
      category: "PDF Security",
      description: "Encrypt PDF with 128-bit AES password and restrict modification.",
      icon: "Lock",
      badge: "Security",
      route: "/api/pdf/security/protect",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "unlock-pdf",
      name: "Unlock PDF (Decrypt)",
      category: "PDF Security",
      description: "Remove password restrictions from authorized password-protected PDFs.",
      icon: "Unlock",
      route: "/api/pdf/security/unlock",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "redact-pdf",
      name: "Redact PDF",
      category: "PDF Security",
      description: "Permanently redact sensitive rectangular zones and scrub underlying content.",
      icon: "EyeOff",
      badge: "Security",
      route: "/api/pdf/security/redact",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "compare-pdf",
      name: "Compare PDFs",
      category: "PDF Security",
      description: "Diff two PDF documents and calculate semantic similarity score.",
      icon: "GitCompare",
      route: "/api/pdf/security/compare",
      acceptedFormats: [".pdf"],
      outputFormats: [".json"],
      isMultiFile: true
    },
    {
      id: "remove-metadata",
      name: "Sanitize & Remove Metadata",
      category: "PDF Security",
      description: "Scrub author, creator, GPS, and creation timestamps for privacy.",
      icon: "ShieldCheck",
      route: "/api/pdf/security/remove-metadata",
      acceptedFormats: [".pdf"],
      outputFormats: [".pdf"],
      isMultiFile: false
    }
  ],
  "OCR & In-House Intelligence": [
    {
      id: "ocr-studio",
      name: "In-House OCR Studio",
      category: "OCR & In-House Intelligence",
      description: "Extract text, detect bounding boxes, and deskew scanned PDFs or images.",
      icon: "ScanText",
      badge: "In-House",
      route: "/api/ocr/process",
      acceptedFormats: [".pdf", ".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"],
      outputFormats: [".txt", ".docx", ".pdf"],
      isMultiFile: false
    },
    {
      id: "ocr-searchable-pdf",
      name: "Scanned to Searchable PDF",
      category: "OCR & In-House Intelligence",
      description: "Overlay an invisible text layer to make scanned PDFs searchable and selectable.",
      icon: "Search",
      badge: "In-House",
      route: "/api/ocr/process",
      acceptedFormats: [".pdf", ".jpg", ".png"],
      outputFormats: [".pdf"],
      isMultiFile: false
    },
    {
      id: "ocr-to-docx",
      name: "Scanned Image to DOCX",
      category: "OCR & In-House Intelligence",
      description: "Convert image-based documents into editable Word (.docx) files.",
      icon: "FileWord",
      badge: "In-House",
      route: "/api/ocr/process",
      acceptedFormats: [".pdf", ".jpg", ".png", ".webp"],
      outputFormats: [".docx"],
      isMultiFile: false
    },
    {
      id: "ai-summarize",
      name: "Document Summarizer",
      category: "OCR & In-House Intelligence",
      description: "Extractive NLP summarization identifying the most salient passages.",
      icon: "Sparkles",
      badge: "AI Local",
      route: "/api/ai/summarize",
      acceptedFormats: [".pdf", ".txt"],
      outputFormats: [".json"],
      isMultiFile: false
    },
    {
      id: "ai-entities",
      name: "Extract Entities & Dates",
      category: "OCR & In-House Intelligence",
      description: "Automatically discover emails, dates, phone numbers, and monetary amounts.",
      icon: "Search",
      badge: "AI Local",
      route: "/api/ai/entities",
      acceptedFormats: [".pdf", ".txt"],
      outputFormats: [".json"],
      isMultiFile: false
    }
  ],
  "Image Tools": [
    {
      id: "convert-image",
      name: "Image Format Converter",
      category: "Image Tools",
      description: "Convert JPG, PNG, WebP, BMP, and TIFF with transparency preservation.",
      icon: "Repeat",
      badge: "Essential",
      route: "/api/convert/image/format",
      acceptedFormats: [".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"],
      outputFormats: [".png", ".jpg", ".webp", ".bmp"],
      isMultiFile: false
    },
    {
      id: "resize-image",
      name: "Resize Image",
      category: "Image Tools",
      description: "Change image dimensions with high-quality bicubic interpolation.",
      icon: "Maximize",
      route: "/api/convert/image/resize",
      acceptedFormats: [".jpg", ".png", ".webp"],
      outputFormats: [".jpg", ".png", ".webp"],
      isMultiFile: false
    },
    {
      id: "compress-image",
      name: "Compress Image",
      category: "Image Tools",
      description: "Reduce file size with fine-grained lossy or lossless compression.",
      icon: "Sliders",
      route: "/api/convert/image/compress",
      acceptedFormats: [".jpg", ".png", ".webp"],
      outputFormats: [".jpg", ".png", ".webp"],
      isMultiFile: false
    }
  ],
  "Data & Utilities": [
    {
      id: "csv-to-xlsx",
      name: "CSV to Excel (XLSX)",
      category: "Data & Utilities",
      description: "Convert CSV files into genuine Microsoft Excel workbooks.",
      icon: "Table",
      badge: "Popular",
      route: "/api/data/csv-to-xlsx",
      acceptedFormats: [".csv"],
      outputFormats: [".xlsx"],
      isMultiFile: false
    },
    {
      id: "xlsx-to-csv",
      name: "Excel to CSV",
      category: "Data & Utilities",
      description: "Export any Excel sheet into comma-separated values format.",
      icon: "FileSpreadsheet",
      route: "/api/data/xlsx-to-csv",
      acceptedFormats: [".xlsx", ".xls"],
      outputFormats: [".csv"],
      isMultiFile: false
    },
    {
      id: "csv-to-json",
      name: "CSV to JSON",
      category: "Data & Utilities",
      description: "Transform tabular records into structured JSON array of objects.",
      icon: "Braces",
      route: "/api/data/csv-to-json",
      acceptedFormats: [".csv"],
      outputFormats: [".json"],
      isMultiFile: false
    },
    {
      id: "qr-generate",
      name: "QR Code Generator",
      category: "Data & Utilities",
      description: "Generate high-resolution QR codes from URLs or text with error correction.",
      icon: "QrCode",
      route: "/api/tools/qr/generate",
      acceptedFormats: [],
      outputFormats: [".png"],
      isMultiFile: false
    },
    {
      id: "qr-scan",
      name: "QR Code Scanner",
      category: "Data & Utilities",
      description: "Decode and read QR codes and barcodes from uploaded images.",
      icon: "Scan",
      route: "/api/tools/qr/scan",
      acceptedFormats: [".jpg", ".png", ".webp"],
      outputFormats: [".json"],
      isMultiFile: false
    },
    {
      id: "file-hashes",
      name: "File Hash Calculator",
      category: "Data & Utilities",
      description: "Compute cryptographic MD5, SHA-1, and SHA-256 hashes of any file.",
      icon: "Fingerprint",
      badge: "Security",
      route: "/api/tools/hashes",
      acceptedFormats: [],
      outputFormats: [".json"],
      isMultiFile: false
    }
  ]
};
