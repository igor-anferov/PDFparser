package com.github.igor_anferov.PDFparser;

import com.github.igor_anferov.PDFOutlineExtractor.Header;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.commons.io.output.NullWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.lang.Math.max;

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
        PDFextractor extractor = new PDFextractor();
        extractor.writeText(document, new NullWriter());
        Document doc = extractor.GetDocument();
        System.out.println(doc.getHeaders());
    }
}
