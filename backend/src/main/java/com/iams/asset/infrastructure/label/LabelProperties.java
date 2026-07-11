package com.iams.asset.infrastructure.label;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iams.labels")
public class LabelProperties {

    private List<Size> sizes = new ArrayList<>();
    private int dpi = 203;

    public List<Size> getSizes() {
        return sizes;
    }

    public void setSizes(List<Size> sizes) {
        this.sizes = sizes;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public static class Size {
        private String key;
        private double widthMm;
        private double heightMm;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public double getWidthMm() {
            return widthMm;
        }

        public void setWidthMm(double widthMm) {
            this.widthMm = widthMm;
        }

        public double getHeightMm() {
            return heightMm;
        }

        public void setHeightMm(double heightMm) {
            this.heightMm = heightMm;
        }
    }
}
