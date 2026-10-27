package com.fileconverter.service.utility;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class UtilityService {

    private static final Logger log = LoggerFactory.getLogger(UtilityService.class);

    private static final int MAX_EXTRACT_ENTRIES = 1000;
    private static final long MAX_UNZIPPED_SIZE = 250 * 1024 * 1024; // 250 MB guard against zip bombs

    public void createZipArchive(List<File> files, File outputZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {
            for (File f : files) {
                ZipEntry entry = new ZipEntry(f.getName());
                zos.putNextEntry(entry);
                Files.copy(f.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    public List<String> extractZipArchive(File zipFile, File destinationDir) throws IOException {
        List<String> extractedFiles = new ArrayList<>();
        int entryCount = 0;
        long totalBytes = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_EXTRACT_ENTRIES) {
                    throw new SecurityException("Archive contains too many entries (zip bomb protection)");
                }

                String cleanName = entry.getName();
                File target = new File(destinationDir, cleanName);

                // Path traversal protection
                String canonicalDest = destinationDir.getCanonicalPath();
                String canonicalTarget = target.getCanonicalPath();
                if (!canonicalTarget.startsWith(canonicalDest + File.separator) && !canonicalTarget.equals(canonicalDest)) {
                    throw new SecurityException("Zip entry is trying to escape destination dir (path traversal attack): " + cleanName);
                }

                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    target.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(target)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            totalBytes += len;
                            if (totalBytes > MAX_UNZIPPED_SIZE) {
                                throw new SecurityException("Extracted size exceeds safety limit of 250MB (zip bomb protection)");
                            }
                            fos.write(buffer, 0, len);
                        }
                    }
                    extractedFiles.add(target.getName());
                }
                zis.closeEntry();
            }
        }
        return extractedFiles;
    }

    public Map<String, String> calculateFileHashes(File file) throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        hashes.put("MD5", calculateHash(file, "MD5"));
        hashes.put("SHA-1", calculateHash(file, "SHA-1"));
        hashes.put("SHA-256", calculateHash(file, "SHA-256"));
        return hashes;
    }

    private String calculateHash(File file, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public Map<String, Object> inspectFileMetadata(File file) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", file.getName());
        result.put("sizeBytes", file.length());
        result.put("sizeFormatted", formatFileSize(file.length()));
        result.put("detectedMagicType", detectMimeType(file));

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            Map<String, String> tags = new LinkedHashMap<>();
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    tags.put(directory.getName() + " - " + tag.getTagName(), tag.getDescription());
                }
            }
            result.put("exifMetadata", tags);
        } catch (Exception e) {
            result.put("exifMetadata", Collections.emptyMap());
        }

        return result;
    }

    public Map<String, Object> countTextMetrics(String text) {
        Map<String, Object> res = new LinkedHashMap<>();
        if (text == null || text.isEmpty()) {
            res.put("words", 0);
            res.put("charactersWithSpaces", 0);
            res.put("charactersNoSpaces", 0);
            res.put("lines", 0);
            res.put("paragraphs", 0);
            return res;
        }

        String trimmed = text.trim();
        String[] words = trimmed.split("\\s+");
        String[] lines = text.split("\r?\n");
        String[] paragraphs = text.split("\r?\n\r?\n");

        res.put("words", trimmed.isEmpty() ? 0 : words.length);
        res.put("charactersWithSpaces", text.length());
        res.put("charactersNoSpaces", text.replaceAll("\\s+", "").length());
        res.put("lines", lines.length);
        res.put("paragraphs", paragraphs.length);
        res.put("estimatedReadingTimeMinutes", Math.max(1, (int) Math.ceil(words.length / 200.0)));

        return res;
    }

    public void generateQrCode(String content, int width, int height, File outputFile) throws Exception {
        QRCodeWriter qrWriter = new QRCodeWriter();
        int w = width > 0 ? width : 300;
        int h = height > 0 ? height : 300;

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrWriter.encode(content, BarcodeFormat.QR_CODE, w, h, hints);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", outputFile.toPath());
    }

    public String scanQrCode(File imageFile) throws Exception {
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) throw new IllegalArgumentException("Cannot decode image for QR scanning");

        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        com.google.zxing.Reader reader = new MultiFormatReader();
        Result result = reader.decode(bitmap);
        return result.getText();
    }

    public String detectMimeType(File file) {
        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read >= 4) {
                // PDF: %PDF
                if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
                    return "application/pdf";
                }
                // PNG: 89 50 4E 47
                if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                    return "image/png";
                }
                // JPEG: FF D8 FF
                if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                    return "image/jpeg";
                }
                // WebP: RIFF ... WEBP
                if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46) {
                    return "image/webp";
                }
                // ZIP / DOCX / XLSX: 50 4B 03 04
                if (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04) {
                    return "application/zip-or-ooxml";
                }
            }
        } catch (Exception ignored) {}
        return "application/octet-stream";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
