package com.iams.asset.infrastructure.label;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Generates Code128 (barcode) and QR (error-correction level M minimum,
 * FR-SCN-07) BitMatrix values from the asset's already-persisted
 * barcode_value / qr_payload. Produces values, not images - image rendering
 * (PNG/SVG/PDF) is LabelRenderService's job, always computed on demand from
 * these values, never stored as a blob.
 */
@Component
public class BarcodeQrService {

    public BitMatrix encodeBarcode(String value, int widthPx, int heightPx) {
        try {
            // OneDimensionalCodeWriter's 4-arg encode() is unchecked (no WriterException
            // in its signature) but can still throw IllegalArgumentException for
            // characters Code128 can't represent - guard that into our own exception type.
            return new Code128Writer().encode(value, BarcodeFormat.CODE_128, widthPx, heightPx);
        } catch (IllegalArgumentException e) {
            throw new LabelRenderException("Failed to encode barcode for value: " + value, e);
        }
    }

    public BitMatrix encodeQr(String value, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1
            );
            return new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
        } catch (WriterException e) {
            throw new LabelRenderException("Failed to encode QR for value: " + value, e);
        }
    }
}
