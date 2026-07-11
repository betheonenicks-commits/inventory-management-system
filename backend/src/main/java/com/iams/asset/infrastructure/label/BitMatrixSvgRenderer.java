package com.iams.asset.infrastructure.label;

import com.google.zxing.common.BitMatrix;

/**
 * ZXing has no SVG writer. This renders a BitMatrix as an SVG <g> fragment: a
 * white background rect plus one black <rect> per contiguous run of set
 * modules in a row (run-length merged, so a solid barcode bar becomes one
 * wide rect instead of dozens of 1px-wide ones). Returns a fragment, not a
 * full document, so LabelRenderService can compose barcode + QR + text into
 * one label SVG via translate()'d groups.
 */
final class BitMatrixSvgRenderer {

    private BitMatrixSvgRenderer() {
    }

    static String renderFragment(BitMatrix matrix, int moduleScale) {
        int width = matrix.getWidth() * moduleScale;
        int height = matrix.getHeight() * moduleScale;

        StringBuilder svg = new StringBuilder();
        svg.append("<rect width=\"").append(width).append("\" height=\"").append(height).append("\" fill=\"#ffffff\"/>\n");

        for (int y = 0; y < matrix.getHeight(); y++) {
            int runStart = -1;
            for (int x = 0; x <= matrix.getWidth(); x++) {
                boolean set = x < matrix.getWidth() && matrix.get(x, y);
                if (set && runStart == -1) {
                    runStart = x;
                } else if (!set && runStart != -1) {
                    int rectX = runStart * moduleScale;
                    int rectY = y * moduleScale;
                    int rectWidth = (x - runStart) * moduleScale;
                    svg.append("<rect x=\"").append(rectX).append("\" y=\"").append(rectY)
                            .append("\" width=\"").append(rectWidth).append("\" height=\"").append(moduleScale)
                            .append("\" fill=\"#000000\"/>\n");
                    runStart = -1;
                }
            }
        }
        return svg.toString();
    }
}
