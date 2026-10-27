package com.fileconverter.service.conversion;

import org.apache.poi.xwpf.usermodel.*;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentConversionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentConversionService.class);

    public void docxToTxt(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile);
             XWPFDocument doc = new XWPFDocument(fis);
             FileWriter fw = new FileWriter(outputFile, StandardCharsets.UTF_8)) {

            for (XWPFParagraph p : doc.getParagraphs()) {
                fw.write(p.getText());
                fw.write("\n\n");
            }
        }
    }

    public void docxToHtml(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile);
             XWPFDocument doc = new XWPFDocument(fis);
             FileWriter fw = new FileWriter(outputFile, StandardCharsets.UTF_8)) {

            fw.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + inputFile.getName() + "</title>");
            fw.write("<style>body{font-family:Calibri,Arial,sans-serif;max-width:800px;margin:40px auto;line-height:1.6;color:#333;padding:0 20px;}h1,h2,h3{color:#1a365d;}p{margin-bottom:1em;}</style>");
            fw.write("</head><body>");

            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) continue;
                String clean = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                String style = p.getStyle() != null ? p.getStyle().toLowerCase() : "";

                if (style.contains("heading1") || style.contains("title")) {
                    fw.write("<h1>" + clean + "</h1>\n");
                } else if (style.contains("heading2")) {
                    fw.write("<h2>" + clean + "</h2>\n");
                } else if (style.contains("heading3")) {
                    fw.write("<h3>" + clean + "</h3>\n");
                } else {
                    fw.write("<p>" + clean + "</p>\n");
                }
            }
            fw.write("</body></html>");
        }
    }

    public void txtToDocx(File inputFile, File outputFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
             XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            String line;
            while ((line = reader.readLine()) != null) {
                XWPFParagraph p = doc.createParagraph();
                p.setSpacingAfter(100);
                XWPFRun r = p.createRun();
                r.setText(line);
                r.setFontFamily("Calibri");
                r.setFontSize(11);
            }
            doc.write(fos);
        }
    }

    public void markdownToHtml(File inputFile, File outputFile) throws IOException {
        String md = java.nio.file.Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
        Parser parser = Parser.builder().build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlBody = renderer.render(document);

        String fullHtml = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Document</title>" +
                "<style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;max-width:800px;margin:40px auto;line-height:1.6;color:#2d3748;padding:0 20px;}code{background:#edf2f7;padding:2px 6px;border-radius:4px;}pre{background:#1a202c;color:#f7fafc;padding:16px;border-radius:6px;overflow-x:auto;}table{border-collapse:collapse;width:100%;margin:20px 0;}th,td{border:1px solid #e2e8f0;padding:8px 12px;text-align:left;}th{background:#f7fafc;}</style>" +
                "</head><body>" + htmlBody + "</body></html>";

        java.nio.file.Files.writeString(outputFile.toPath(), fullHtml, StandardCharsets.UTF_8);
    }

    public void htmlToMarkdown(File inputFile, File outputFile) throws IOException {
        String html = java.nio.file.Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
        // Clean basic HTML tags to Markdown
        String md = html
                .replaceAll("(?i)<h1[^>]*>(.*?)</h1>", "# $1\n\n")
                .replaceAll("(?i)<h2[^>]*>(.*?)</h2>", "## $1\n\n")
                .replaceAll("(?i)<h3[^>]*>(.*?)</h3>", "### $1\n\n")
                .replaceAll("(?i)<strong[^>]*>(.*?)</strong>", "**$1**")
                .replaceAll("(?i)<b[^>]*>(.*?)</b>", "**$1**")
                .replaceAll("(?i)<em[^>]*>(.*?)</em>", "*$1*")
                .replaceAll("(?i)<i[^>]*>(.*?)</i>", "*$1*")
                .replaceAll("(?i)<p[^>]*>(.*?)</p>", "$1\n\n")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)<li[^>]*>(.*?)</li>", "- $1\n")
                .replaceAll("(?i)<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");

        java.nio.file.Files.writeString(outputFile.toPath(), md.trim() + "\n", StandardCharsets.UTF_8);
    }
}
