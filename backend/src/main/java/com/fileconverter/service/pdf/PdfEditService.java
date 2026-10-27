package com.fileconverter.service.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class PdfEditService {

    private static final Logger log = LoggerFactory.getLogger(PdfEditService.class);

    public void addTextOverlay(File inputFile, int pageNumber, String text, float x, float y, float fontSize, String hexColor, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int pageIdx = Math.max(0, Math.min(doc.getNumberOfPages() - 1, pageNumber - 1));
            PDPage page = doc.getPage(pageIdx);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            Color color = Color.BLACK;
            if (hexColor != null && !hexColor.isEmpty()) {
                try {
                    color = Color.decode(hexColor.startsWith("#") ? hexColor : "#" + hexColor);
                } catch (Exception ignored) {}
            }

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.beginText();
                cs.setFont(font, fontSize > 0 ? fontSize : 14.0f);
                cs.setNonStrokingColor(color);
                cs.newLineAtOffset(x, y);
                cs.showText(text);
                cs.endText();
            }
            doc.save(outputFile);
        }
    }

    public void addHighlight(File inputFile, int pageNumber, float x, float y, float width, float height, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int pageIdx = Math.max(0, Math.min(doc.getNumberOfPages() - 1, pageNumber - 1));
            PDPage page = doc.getPage(pageIdx);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                gs.setNonStrokingAlphaConstant(0.4f);
                cs.setGraphicsStateParameters(gs);
                cs.setNonStrokingColor(Color.YELLOW);
                cs.addRect(x, y, width, height);
                cs.fill();
            }
            doc.save(outputFile);
        }
    }

    public void addImageOverlay(File inputFile, File imageFile, int pageNumber, float x, float y, float width, float height, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int pageIdx = Math.max(0, Math.min(doc.getNumberOfPages() - 1, pageNumber - 1));
            PDPage page = doc.getPage(pageIdx);

            BufferedImage img = ImageIO.read(imageFile);
            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, img);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.drawImage(pdImage, x, y, width, height);
            }
            doc.save(outputFile);
        }
    }
}
