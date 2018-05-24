package com.github.igor_anferov.PDFparser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

import static java.lang.Math.round;

public class RegionPDFRenderer {

    private static final int POINTS_IN_INCH = 72;

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final int resolutionDotPerInch;

    RegionPDFRenderer(PDDocument document, int resolutionDotPerInch) {
        this.document = document;
        this.renderer = new PDFRenderer(document);
        this.resolutionDotPerInch = resolutionDotPerInch;
    }

    RenderedImage renderRect(int pageIndex, Rectangle2D.Float rect) throws IOException {
        BufferedImage image = createImage(rect);
        Graphics2D graphics = createGraphics(image, rect);
        renderer.renderPageToGraphics(pageIndex, graphics);
        graphics.dispose();
        return image;
    }

    private BufferedImage createImage(Rectangle2D.Float rect) {
        float scale = resolutionDotPerInch / POINTS_IN_INCH;
        int bitmapWidth  = round(rect.width  * scale);
        int bitmapHeight = round(rect.height * scale);
        return new BufferedImage(bitmapWidth, bitmapHeight, BufferedImage.TYPE_INT_RGB);
    }

    private Graphics2D createGraphics(BufferedImage image, Rectangle2D.Float rect) {
        double scale = resolutionDotPerInch / POINTS_IN_INCH;
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        transform.concatenate(AffineTransform.getTranslateInstance(-rect.x, -rect.y));

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.setTransform(transform);
        return graphics;
    }

}
