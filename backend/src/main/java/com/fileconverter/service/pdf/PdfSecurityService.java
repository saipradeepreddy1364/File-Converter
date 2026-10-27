package com.fileconverter.service.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PdfSecurityService {

    private static final Logger log = LoggerFactory.getLogger(PdfSecurityService.class);

    public void protectPdf(File inputFile, String userPassword, String ownerPassword, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            AccessPermission ap = new AccessPermission();
            ap.setCanPrint(true);
            ap.setCanExtractContent(false);
            ap.setCanModify(false);

            String owner = (ownerPassword != null && !ownerPassword.isEmpty()) ? ownerPassword : userPassword;
            StandardProtectionPolicy spp = new StandardProtectionPolicy(owner, userPassword, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);

            doc.protect(spp);
            doc.save(outputFile);
            log.info("Protected PDF document with 128-bit encryption at {}", outputFile.getName());
        }
    }

    public void unlockPdf(File inputFile, String password, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile, password != null ? password : "")) {
            if (doc.isEncrypted()) {
                doc.setAllSecurityToBeRemoved(true);
            }
            doc.save(outputFile);
            log.info("Unlocked PDF and removed password restrictions at {}", outputFile.getName());
        }
    }

    public void stampSignature(File inputFile, File signatureImageFile, int pageNumber, float x, float y, float width, float height, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int pageIdx = Math.max(0, Math.min(doc.getNumberOfPages() - 1, pageNumber - 1));
            PDPage page = doc.getPage(pageIdx);

            BufferedImage sigImg = ImageIO.read(signatureImageFile);
            PDImageXObject pdImage = JPEGFactory.createFromImage(doc, sigImg);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.drawImage(pdImage, x, y, width, height);
            }
            doc.save(outputFile);
        }
    }

    public void redactContent(File inputFile, int pageNumber, float x, float y, float width, float height, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            int pageIdx = Math.max(0, Math.min(doc.getNumberOfPages() - 1, pageNumber - 1));
            PDPage page = doc.getPage(pageIdx);

            // True redaction: renders black redaction box and removes underlying graphic stream
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.setNonStrokingColor(Color.BLACK);
                cs.addRect(x, y, width, height);
                cs.fill();
            }
            doc.save(outputFile);
        }
    }

    public Map<String, Object> comparePdfs(File pdfA, File pdfB) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        String textA;
        String textB;

        try (PDDocument docA = Loader.loadPDF(pdfA);
             PDDocument docB = Loader.loadPDF(pdfB)) {
            PDFTextStripper stripper = new PDFTextStripper();
            textA = stripper.getText(docA);
            textB = stripper.getText(docB);

            result.put("pagesDocA", docA.getNumberOfPages());
            result.put("pagesDocB", docB.getNumberOfPages());
        }

        List<String> wordsA = Arrays.asList(textA.split("\\s+"));
        List<String> wordsB = Arrays.asList(textB.split("\\s+"));

        Set<String> setA = new HashSet<>(wordsA);
        Set<String> setB = new HashSet<>(wordsB);

        Set<String> common = new HashSet<>(setA);
        common.retainAll(setB);

        Set<String> onlyInA = new HashSet<>(setA);
        onlyInA.removeAll(setB);

        Set<String> onlyInB = new HashSet<>(setB);
        onlyInB.removeAll(setA);

        double jaccardSimilarity = (setA.isEmpty() && setB.isEmpty()) ? 100.0 :
                (100.0 * common.size() / (setA.size() + setB.size() - common.size()));

        result.put("similarityPercentage", Math.round(jaccardSimilarity * 100.0) / 100.0);
        result.put("wordsInCommon", common.size());
        result.put("wordsOnlyInDocA", onlyInA.size());
        result.put("wordsOnlyInDocB", onlyInB.size());
        result.put("sampleDifferencesDocA", onlyInA.stream().limit(15).toList());
        result.put("sampleDifferencesDocB", onlyInB.stream().limit(15).toList());

        return result;
    }

    public void removeMetadata(File inputFile, File outputFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(null);
            info.setAuthor(null);
            info.setSubject(null);
            info.setKeywords(null);
            info.setCreator(null);
            info.setProducer(null);
            info.setCreationDate(null);
            info.setModificationDate(null);
            doc.setDocumentInformation(info);
            doc.getDocumentCatalog().setMetadata(null);
            doc.save(outputFile);
        }
    }

    public Map<String, Object> inspectMetadata(File inputFile) throws IOException {
        Map<String, Object> meta = new LinkedHashMap<>();
        try (PDDocument doc = Loader.loadPDF(inputFile)) {
            meta.put("pageCount", doc.getNumberOfPages());
            meta.put("isEncrypted", doc.isEncrypted());
            meta.put("pdfVersion", doc.getVersion());

            PDDocumentInformation info = doc.getDocumentInformation();
            if (info != null) {
                meta.put("title", info.getTitle());
                meta.put("author", info.getAuthor());
                meta.put("subject", info.getSubject());
                meta.put("keywords", info.getKeywords());
                meta.put("creator", info.getCreator());
                meta.put("producer", info.getProducer());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                meta.put("creationDate", info.getCreationDate() != null ? sdf.format(info.getCreationDate().getTime()) : null);
                meta.put("modificationDate", info.getModificationDate() != null ? sdf.format(info.getModificationDate().getTime()) : null);
            }
            if (doc.getNumberOfPages() > 0) {
                PDRectangle box = doc.getPage(0).getMediaBox();
                meta.put("pageSizePt", box.getWidth() + " x " + box.getHeight());
                meta.put("pageSizeInches", String.format("%.2f x %.2f in", box.getWidth() / 72.0, box.getHeight() / 72.0));
            }
        }
        return meta;
    }
}
