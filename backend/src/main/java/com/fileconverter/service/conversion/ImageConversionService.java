package com.fileconverter.service.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ImageConversionService {

    private static final Logger log = LoggerFactory.getLogger(ImageConversionService.class);

    public void convertFormat(File inputFile, String targetFormat, String bgColorHex, File outputFile) throws IOException {
        BufferedImage src = ImageIO.read(inputFile);
        if (src == null) {
            throw new IllegalArgumentException("Cannot decode input image: " + inputFile.getName());
        }

        String fmt = targetFormat.toLowerCase();
        boolean targetSupportsAlpha = fmt.equals("png") || fmt.equals("webp");

        BufferedImage outImg;
        if (!targetSupportsAlpha && src.getColorModel().hasAlpha()) {
            // Fill transparent pixels with background color (default White)
            Color bg = Color.WHITE;
            if (bgColorHex != null && !bgColorHex.isEmpty()) {
                try {
                    bg = Color.decode(bgColorHex.startsWith("#") ? bgColorHex : "#" + bgColorHex);
                } catch (Exception ignored) {}
            }
            outImg = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outImg.createGraphics();
            g.setColor(bg);
            g.fillRect(0, 0, src.getWidth(), src.getHeight());
            g.drawImage(src, 0, 0, null);
            g.dispose();
        } else {
            outImg = src;
        }

        ImageIO.write(outImg, fmt, outputFile);
    }

    public void resizeImage(File inputFile, int targetWidth, int targetHeight, boolean keepAspectRatio, File outputFile) throws IOException {
        BufferedImage src = ImageIO.read(inputFile);
        if (src == null) throw new IllegalArgumentException("Cannot decode image");

        int w = src.getWidth();
        int h = src.getHeight();

        int finalW = targetWidth;
        int finalH = targetHeight;

        if (keepAspectRatio) {
            if (finalW > 0 && finalH <= 0) {
                finalH = (int) ((double) finalW / w * h);
            } else if (finalH > 0 && finalW <= 0) {
                finalW = (int) ((double) finalH / h * w);
            } else if (finalW > 0 && finalH > 0) {
                double ratio = Math.min((double) finalW / w, (double) finalH / h);
                finalW = (int) (w * ratio);
                finalH = (int) (h * ratio);
            }
        }

        finalW = Math.max(1, finalW);
        finalH = Math.max(1, finalH);

        int imageType = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(finalW, finalH, imageType);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, finalW, finalH, null);
        g.dispose();

        String fmt = getFileExtension(inputFile.getName());
        ImageIO.write(resized, fmt.isEmpty() ? "png" : fmt, outputFile);
    }

    public void cropImage(File inputFile, int x, int y, int width, int height, File outputFile) throws IOException {
        BufferedImage src = ImageIO.read(inputFile);
        if (src == null) throw new IllegalArgumentException("Cannot decode image");

        int safeX = Math.max(0, Math.min(src.getWidth() - 1, x));
        int safeY = Math.max(0, Math.min(src.getHeight() - 1, y));
        int safeW = Math.max(1, Math.min(src.getWidth() - safeX, width));
        int safeH = Math.max(1, Math.min(src.getHeight() - safeY, height));

        BufferedImage cropped = src.getSubimage(safeX, safeY, safeW, safeH);
        String fmt = getFileExtension(inputFile.getName());
        ImageIO.write(cropped, fmt.isEmpty() ? "png" : fmt, outputFile);
    }

    public void rotateAndFlip(File inputFile, int rotationDegrees, boolean flipHorizontal, boolean flipVertical, File outputFile) throws IOException {
        BufferedImage src = ImageIO.read(inputFile);
        if (src == null) throw new IllegalArgumentException("Cannot decode image");

        int w = src.getWidth();
        int h = src.getHeight();

        double rad = Math.toRadians(rotationDegrees);
        int newW = (rotationDegrees % 180 != 0) ? h : w;
        int newH = (rotationDegrees % 180 != 0) ? w : h;

        int imgType = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage rotated = new BufferedImage(newW, newH, imgType);
        Graphics2D g = rotated.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        AffineTransform at = new AffineTransform();
        at.translate((newW - w) / 2.0, (newH - h) / 2.0);
        at.rotate(rad, w / 2.0, h / 2.0);

        if (flipHorizontal) {
            at.scale(-1, 1);
            at.translate(-w, 0);
        }
        if (flipVertical) {
            at.scale(1, -1);
            at.translate(0, -h);
        }

        g.setTransform(at);
        g.drawImage(src, 0, 0, null);
        g.dispose();

        String fmt = getFileExtension(inputFile.getName());
        ImageIO.write(rotated, fmt.isEmpty() ? "png" : fmt, outputFile);
    }

    public void compressImage(File inputFile, float quality, File outputFile) throws IOException {
        BufferedImage src = ImageIO.read(inputFile);
        if (src == null) throw new IllegalArgumentException("Cannot decode image");

        float q = Math.max(0.05f, Math.min(1.0f, quality));
        String ext = getFileExtension(inputFile.getName()).toLowerCase();

        if (ext.equals("png") && src.getColorModel().hasAlpha()) {
            // PNG is lossless, write directly
            ImageIO.write(src, "png", outputFile);
            return;
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(src, "jpeg", outputFile);
            return;
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(q);
            }
            BufferedImage rgbImg = src;
            if (src.getColorModel().hasAlpha()) {
                rgbImg = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgbImg.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, src.getWidth(), src.getHeight());
                g.drawImage(src, 0, 0, null);
                g.dispose();
            }
            writer.write(null, new IIOImage(rgbImg, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public void batchConvertImages(List<File> files, String targetFormat, File outputZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {
            for (File file : files) {
                BufferedImage bImg = ImageIO.read(file);
                if (bImg != null) {
                    String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    ZipEntry entry = new ZipEntry(baseName + "." + targetFormat.toLowerCase());
                    zos.putNextEntry(entry);
                    ImageIO.write(bImg, targetFormat.toLowerCase(), zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private String getFileExtension(String name) {
        int idx = name.lastIndexOf('.');
        return (idx > 0) ? name.substring(idx + 1) : "";
    }
}
