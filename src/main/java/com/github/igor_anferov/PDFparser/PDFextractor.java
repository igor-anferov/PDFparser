package com.github.igor_anferov.PDFparser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static java.lang.Math.max;

public class PDFextractor extends PDFTextStripper {
    String name;
    private RegionPDFRenderer renderer;
    private Document document = null;
    private Block block = null;
    private Line line = null;
    private int pageNum = 1;

    PDFextractor() throws IOException {
        super();
    }

    public Document GetDocument()
    {
        document.RemoveHeadersAndFooters();
        document.fillPositions();
        document.mergeFirstLinesWithRest();
        document.FillBlocksAlignments();
        document.mergeEopBlocks();
        document.MergeLinesInsideBlocks();
        document.FillBlocksTypes();
        document.FillStylesHist();
        document.fillHierarchy();
        return document;
    }

    @Override
    public void writeText(PDDocument doc, Writer outputStream) throws IOException
    {
        renderer = new RegionPDFRenderer(doc, 288);
        name = doc.getDocumentInformation().getTitle();
        document = new Document(name);
        super.writeText(doc, outputStream);
    }

    @Override
    protected void writeString(String t, List<TextPosition> textPositions) throws IOException
    {
        StringBuilder text = new StringBuilder();

        List<Style> styles = new ArrayList<>();
        List<OnPagePosition> onPagePositions = new ArrayList<>();

        for (TextPosition textPosition : textPositions) {
            Style style = new Style();
            OnPagePosition onPagePosition = new OnPagePosition();
            onPagePosition.page = pageNum;
            onPagePosition.xMin = textPosition.getX();
            onPagePosition.xMax = textPosition.getX() + textPosition.getWidth();
            onPagePosition.yMin = textPosition.getY() - textPosition.getHeight();
            onPagePosition.yMax = textPosition.getY();
            style.font = textPosition.getFont().getName();
            style.size = max(textPosition.getFontSize(), textPosition.getFontSizeInPt());
            String curtext = textPosition.toString();
            for (int i = 0; i < curtext.length(); ++i) {
                styles.add(style);
                onPagePositions.add(onPagePosition);
            }
            text.append(curtext);
        }

        assert (text.length() == styles.size() && text.length() == onPagePositions.size());

        Line currentLine = new Line();
        currentLine.text = text.toString();

        for (int i = 0; i < currentLine.text.length(); i++) {
            if (styles.get(i).size == 0) {
                currentLine.text = currentLine.text.substring(0, i) + currentLine.text.substring(i+1);
                styles.remove(i);
                onPagePositions.remove(i);
                i--;
            }
        }

        currentLine.styles = styles;
        currentLine.onPagePositions = onPagePositions;

        currentLine.Trim();

        if (currentLine.isEmpty())
            return;

        if (line == null)
            line = currentLine;
        else
            line.AppendWord(currentLine);
    }

//    @Override
//    protected void writeWordSeparator() throws IOException
//    {
//        if (line == null || line.isEmpty())
//            return;
//        line.text += " ";
//        line.styles.add(line.styles.get(line.styles.size()-1));
//        line.onPagePositions.add(line.onPagePositions.get(line.onPagePositions.size()-1));
//    }

    @Override
    protected void writeLineSeparator() throws IOException
    {
        if (line == null)
            return;
        line.Trim();
        if (line.isEmpty())
            return;
        if (block == null) {
            block = new Block(renderer);
            block.Append(line);
        } else {
            if (block.getLastLine().HaveSameStyles(line)
                    && (line.GetAlignWith(block.getLastLine()) != Style.AlignType.Unknown
                        || block.getLastLine().EndsWithHyphen())
                    && line.Near(block.getLastLine())) {
                block.Append(line);
            } else {
                document.Append(block);
                block = new Block(renderer);
                block.Append(line);
            }
        }
        line = null;
    }

    @Override
    protected void writePageEnd() throws IOException
    {
        if (line != null)
            writeLineSeparator();
        line = null;
        if (block == null)
            return;
        block.endOfPage = true;
        document.eopBlocks.add(document.blocks.size());
        document.Append(block);
        block = null;
        pageNum++;
    }

    @Override
    protected void endDocument(PDDocument document) throws IOException {
        if (line != null || block != null)
            writePageEnd();
    }
}
