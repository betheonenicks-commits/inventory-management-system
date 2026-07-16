package com.iams.asset.infrastructure.label;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Renders an asset's barcode + QR + human-readable number as a single label
 * image in PNG, SVG, or PDF (FR-AST-02, FR-SCN-07). Composites onto a raster
 * canvas for PNG/PDF; assembles an equivalent SVG document by hand for SVG,
 * since ZXing has no SVG writer. Label rendering never touches the database
 * beyond the values already on the asset - it can be retried/re-downloaded
 * indefinitely without ever blocking or repeating registration.
 */
@Service
@EnableConfigurationProperties(LabelProperties.class)
public class LabelRenderService {

    private static final double MM_PER_INCH = 25.4;
    private static final double POINTS_PER_MM = 2.8346;
    private static final int PADDING_PX = 6;
    private static final int TEXT_ROW_HEIGHT_PX = 22;

    private final BarcodeQrService barcodeQrService;
    private final LabelProperties labelProperties;

    public LabelRenderService(BarcodeQrService barcodeQrService, LabelProperties labelProperties) {
        this.barcodeQrService = barcodeQrService;
        this.labelProperties = labelProperties;
    }

    public List<LabelProperties.Size> availableSizes() {
        return labelProperties.getSizes();
    }

    public Optional<LabelProperties.Size> findSize(String key) {
        return labelProperties.getSizes().stream().filter(s -> s.getKey().equals(key)).findFirst();
    }

    public byte[] render(String barcodeValue, String qrPayload, String displayText, LabelFormat format, LabelProperties.Size size) {
        int dpi = labelProperties.getDpi();
        int widthPx = mmToPx(size.getWidthMm(), dpi);
        int heightPx = mmToPx(size.getHeightMm(), dpi);

        return switch (format) {
            case PNG -> renderPng(barcodeValue, qrPayload, displayText, widthPx, heightPx);
            case SVG -> renderSvg(barcodeValue, qrPayload, displayText, widthPx, heightPx).getBytes(StandardCharsets.UTF_8);
            case PDF -> renderPdf(barcodeValue, qrPayload, displayText, widthPx, heightPx, size);
        };
    }

    /**
     * US-RPT-11: one print-ready PDF, one page per label, every page the
     * configured physical size - the same composite pipeline as the single
     * label PDF, batched into a single document.
     */
    public byte[] renderBatchPdf(List<BatchLabel> labels, LabelProperties.Size size) {
        int dpi = labelProperties.getDpi();
        int widthPx = mmToPx(size.getWidthMm(), dpi);
        int heightPx = mmToPx(size.getHeightMm(), dpi);
        float pageWidthPt = (float) (size.getWidthMm() * POINTS_PER_MM);
        float pageHeightPt = (float) (size.getHeightMm() * POINTS_PER_MM);

        try (PDDocument document = new PDDocument()) {
            for (BatchLabel label : labels) {
                BufferedImage canvas = compositeImage(label.barcodeValue(), label.qrPayload(), label.displayText(),
                        widthPx, heightPx);
                PDPage page = new PDPage(new PDRectangle(pageWidthPt, pageHeightPt));
                document.addPage(page);
                PDImageXObject image = LosslessFactory.createFromImage(document, canvas);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(image, 0, 0, pageWidthPt, pageHeightPt);
                }
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new LabelRenderException("Failed to render batch label PDF", e);
        }
    }

    public record BatchLabel(String barcodeValue, String qrPayload, String displayText) {
    }

    private int mmToPx(double mm, int dpi) {
        return (int) Math.round(mm / MM_PER_INCH * dpi);
    }

    private int qrSize(int heightPx) {
        return Math.max(1, heightPx - TEXT_ROW_HEIGHT_PX - 2 * PADDING_PX);
    }

    private int barcodeWidth(int widthPx, int qrSize) {
        return Math.max(1, widthPx - qrSize - 3 * PADDING_PX);
    }

    private byte[] renderPng(String barcodeValue, String qrPayload, String displayText, int widthPx, int heightPx) {
        BufferedImage canvas = compositeImage(barcodeValue, qrPayload, displayText, widthPx, heightPx);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new LabelRenderException("Failed to write PNG label", e);
        }
    }

    private BufferedImage compositeImage(String barcodeValue, String qrPayload, String displayText, int widthPx, int heightPx) {
        int qrSize = qrSize(heightPx);
        int barcodeWidth = barcodeWidth(widthPx, qrSize);

        BitMatrix qrMatrix = barcodeQrService.encodeQr(qrPayload, qrSize);
        BitMatrix barcodeMatrix = barcodeQrService.encodeBarcode(barcodeValue, barcodeWidth, qrSize);

        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(qrMatrix);
        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(barcodeMatrix);

        BufferedImage canvas = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, widthPx, heightPx);

            g.drawImage(qrImage, PADDING_PX, PADDING_PX, null);
            g.drawImage(barcodeImage, PADDING_PX * 2 + qrSize, PADDING_PX, null);

            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            g.drawString(displayText, PADDING_PX, heightPx - PADDING_PX - 4);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private String renderSvg(String barcodeValue, String qrPayload, String displayText, int widthPx, int heightPx) {
        int qrSize = qrSize(heightPx);
        int barcodeWidth = barcodeWidth(widthPx, qrSize);

        BitMatrix qrMatrix = barcodeQrService.encodeQr(qrPayload, qrSize);
        BitMatrix barcodeMatrix = barcodeQrService.encodeBarcode(barcodeValue, barcodeWidth, qrSize);

        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(widthPx)
                .append("\" height=\"").append(heightPx)
                .append("\" viewBox=\"0 0 ").append(widthPx).append(' ').append(heightPx).append("\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");

        svg.append("<g transform=\"translate(").append(PADDING_PX).append(',').append(PADDING_PX).append(")\">\n");
        svg.append(BitMatrixSvgRenderer.renderFragment(qrMatrix, 1));
        svg.append("</g>\n");

        svg.append("<g transform=\"translate(").append(PADDING_PX * 2 + qrSize).append(',').append(PADDING_PX).append(")\">\n");
        svg.append(BitMatrixSvgRenderer.renderFragment(barcodeMatrix, 1));
        svg.append("</g>\n");

        svg.append("<text x=\"").append(PADDING_PX).append("\" y=\"").append(heightPx - PADDING_PX)
                .append("\" font-family=\"monospace\" font-size=\"14\" fill=\"#000000\">")
                .append(escapeXml(displayText)).append("</text>\n");

        svg.append("</svg>\n");
        return svg.toString();
    }

    private byte[] renderPdf(String barcodeValue, String qrPayload, String displayText, int widthPx, int heightPx, LabelProperties.Size size) {
        BufferedImage canvas = compositeImage(barcodeValue, qrPayload, displayText, widthPx, heightPx);

        float pageWidthPt = (float) (size.getWidthMm() * POINTS_PER_MM);
        float pageHeightPt = (float) (size.getHeightMm() * POINTS_PER_MM);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(pageWidthPt, pageHeightPt));
            document.addPage(page);
            PDImageXObject image = LosslessFactory.createFromImage(document, canvas);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(image, 0, 0, pageWidthPt, pageHeightPt);
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new LabelRenderException("Failed to render PDF label", e);
        }
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
