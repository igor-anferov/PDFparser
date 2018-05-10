import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.commons.io.output.NullWriter;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main( String[] args ) throws IOException
    {
        if (args.length == 0) {
            System.out.println("Please specify path to PDF file (and password if needed)");
            return;
        }
        String password = args.length > 1 ? args[1] : "";
        PDDocument document = PDDocument.load(new File(args[0]), password);
        if (!document.getCurrentAccessPermission().canExtractContent()) {
            System.out.println("Have no permissions to extract PDF's content");
            return;
        }
//        RegionPDFRenderer renderer = new RegionPDFRenderer(document, 288);
//        RenderedImage r = renderer.renderRect(0, new Rectangle2D.Float(56.664f, 285.57f, 102.99f-56.664f, 415.79f-285.57f));
//        ImageIO.write(r, "png", new File("/Users/Igor/Downloads/output/test.png"));

        PDFextractor extractor = new PDFextractor();
        extractor.writeText(document, new NullWriter());
        extractor.GetDocument();
    }
}
