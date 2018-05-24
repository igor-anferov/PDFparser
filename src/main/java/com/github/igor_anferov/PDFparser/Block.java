package com.github.igor_anferov.PDFparser;

import javafx.util.Pair;

import javax.xml.bind.annotation.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.stream.Collectors;

import static java.lang.StrictMath.max;

public class Block {
    enum Type {
        Numbered,
        NumberedLabel,
        PlainText,
        Formula,
    }
    static class BlockType {
        @XmlValue
        Type type;
        @XmlTransient
        List<String> number;
        @XmlTransient
        String delim;
        @XmlTransient
        Comparator<String> cmp;
        @XmlTransient
        int numberType;
        @XmlTransient
        String labelPrefix;

        BlockType()
        {
            type = Type.PlainText;
            number = new ArrayList<>();
        }
    }

    @XmlAttribute
    BlockType type;
    @XmlElement(name = "text")
    public List<Line> lines;
    @XmlTransient
    public boolean endOfPage = false;
    @XmlAttribute
    public Style.AlignType alignment = Style.AlignType.Unknown;
    @XmlTransient
    private RegionPDFRenderer renderer;
    @XmlElementWrapper(name = "subBlocks")
    @XmlElement(name = "subBlock")
    public List<Block> sons;

    Block(RegionPDFRenderer renderer)
    {
        lines = new ArrayList<>();
        type = new BlockType();
        sons = new ArrayList<>();
        this.renderer = renderer;
    }

    public String toString() {
        return lines.stream().map(Line::toString).collect(Collectors.joining("\n"));
    }

    public int getFirstPage()
    {
        return getFirstLine().getFirstPage();
    }

    public int getLastPage()
    {
        return getLastLine().getLastPage();
    }

    public float xMin()
    {
        assert (!lines.isEmpty());
        float res = lines.get(0).xMin();
        for (Line line : lines)
            if (line.xMin() < res)
                res = line.xMin();
        return res;
    }

    public float xMax()
    {
        assert (!lines.isEmpty());
        float res = lines.get(0).xMax();
        for (Line line : lines)
            if (line.xMax() > res)
                res = line.xMax();
        return res;
    }

    public float getEps()
    {
        assert (!lines.isEmpty());
        float res = lines.get(0).getEps();
        for (Line line : lines)
            if (line.getEps() > res)
                res = line.getEps();
        return res;
    }

    public Map<Integer, OnPagePosition> GetPositionsHistogram()
    {
        TreeMap<Integer, OnPagePosition> map = new TreeMap<>();
        for (Line line : lines) {
            for (OnPagePosition onPagePosition : line.onPagePositions) {
                if (map.containsKey(onPagePosition.page)) {
                    OnPagePosition pos = map.get(onPagePosition.page);
                    pos.ExtendTo(onPagePosition);
                } else {
                    map.put(onPagePosition.page, onPagePosition.clone());
                }
            }
        }
        return map;
    }

    public List<RenderedImage> GetRender() throws IOException {
        float xField = getMedianCharWidth() / 2;
        float yField = getMedianCharHeight() / 1.3f;
        List<RenderedImage> ret = new ArrayList<>();
        for (Map.Entry<Integer, OnPagePosition> entry : GetPositionsHistogram().entrySet()) {
            getMedianStyle();
            Rectangle.Float region = new Rectangle.Float(
                    entry.getValue().xMin - xField,
                    entry.getValue().yMin - yField,
                    entry.getValue().xMax - entry.getValue().xMin + 2 * xField,
                    entry.getValue().yMax - entry.getValue().yMin + 1.8f * yField
            );

            ret.add(renderer.renderRect(entry.getKey() - 1, region));
        }
        return ret;
    }

    public void Append(Line l)
    {
        lines.add(l);
    }

    public void Append(Block other)
    {
        lines.addAll(other.lines);
        endOfPage |= other.endOfPage;
        alignment = Style.AlignType.values()[max(alignment.ordinal(), other.alignment.ordinal())];
    }

    public void MergeLines()
    {
        while (lines.size() >  1)
            lines.get(0).Merge(lines.remove(1));
    }

    public Style getMedianStyle()
    {
        Map<Style, Integer> diagramm = new HashMap<>();
        for (Line line : lines)
            diagramm.merge(line.getMedianStyle(), 1, Integer::sum);
        Style common = null;
        Integer commonCnt = 0;
        for (Map.Entry<Style, Integer> styleIntegerEntry : diagramm.entrySet())
            if (styleIntegerEntry.getValue() >= commonCnt) {
                commonCnt = styleIntegerEntry.getValue();
                common = styleIntegerEntry.getKey();
            }
        return common;
    }

    public float getMedianCharHeight()
    {
        List<Float> l = new ArrayList<>();
        for (Line line : lines)
            for (OnPagePosition pos : line.onPagePositions)
                l.add(pos.yMax - pos.yMin);
        l.sort(Float::compare);
        l = l.subList((int)(l.size() * 0.33), (int)(l.size() * 0.67));

        return l.stream().reduce(0f, Float::sum) / l.size();
    }

    public float getMedianCharWidth()
    {
        List<Float> l = new ArrayList<>();
        for (Line line : lines)
            for (OnPagePosition pos : line.onPagePositions)
                l.add(pos.xMax - pos.xMin);
        l.sort(Float::compare);
        l = l.subList((int)(l.size() * 0.33), (int)(l.size() * 0.67));

        return l.stream().reduce(0f, Float::sum) / l.size();
    }

    public float getMaxLineWidth()
    {
        float maxWidth = 0;
        for (Line line : lines)
            if (line.Width() > maxWidth)
                maxWidth = line.Width();
        return maxWidth;
    }

    public boolean isLeftAligned()
    {
        if (lines.isEmpty())
            return true;
        float xMin, xMax;
        float maxWidth = lines.get(0).Width();
        xMin = xMax = lines.get(0).xMin();
        for (Line line : lines) {
            if (line.Width() > maxWidth)
                maxWidth = line.Width();
            float min = line.xMin();
            if (min < xMin)
                xMin = min;
            if (min > xMax)
                xMax = min;
        }
        return xMax - xMin < maxWidth / 80;
    }

    public Line getLastLine()
    {
        if (lines.isEmpty())
            return null;
        return lines.get(lines.size() - 1);
    }

    public Line getFirstLine()
    {
        if (lines.isEmpty())
            return null;
        return lines.get(0);
    }

    public void FillAlignment()
    {
        if (lines.size() <= 1) {
            alignment = Style.AlignType.Unknown;
            return;
        }
        alignment = lines.get(0).GetAlignWith(lines.get(1));
        if (lines.size() == 2 || alignment == Style.AlignType.Unknown) {
            return;
        }
        for (int i = 1; i < lines.size() - 1; i++) {
            Style.AlignType newAlignment = lines.get(i).GetAlignWith(lines.get(i + 1));
            if (newAlignment == Style.AlignType.Unknown) {
                alignment = Style.AlignType.Unknown;
                return;
            }
            do {
                switch (alignment) {
                    case Right:
                        if (i == 1 && (newAlignment == Style.AlignType.Full ||
                                (newAlignment == Style.AlignType.Left && i == lines.size() - 2))) {
                            alignment = Style.AlignType.Full;
                            continue;
                        }
                    case Left:
                    case Center:
                        if (newAlignment == Style.AlignType.Full || newAlignment == alignment)
                            break;
                        else
                            alignment = Style.AlignType.Unknown;
                        break;
                    case Full:
                        if (i == lines.size() - 2 && newAlignment == Style.AlignType.Left)
                            break;
                        alignment = newAlignment;
                        break;
                }
            } while (false);
        }
    }

    static Pattern labelPrefix = Pattern.compile("^\\s*((?:\\p{Z}?(?>[\\p{L}\\p{M}.]{1,20})){1,3})\\s*\\d", Pattern.UNICODE_CHARACTER_CLASS);
    static private List<Pair<Pattern, Comparator<String>>> numbered = new ArrayList<>();
    static {
        numbered.add(new Pair<>(Pattern.compile("^\\s*(\\d+(?:(?<delim>[\\pP\\pS]+)(?:\\d+(?:\\k<delim>\\d+)*)?)?)", Pattern.UNICODE_CHARACTER_CLASS),
                Comparator.comparingInt(Integer::parseInt)));
    }
    public void FillType()
    {
        String forSearch = getFirstLine().text;
        Matcher prefix = labelPrefix.matcher(forSearch);
        if (prefix.lookingAt()) {
            type.labelPrefix = prefix.group(1);
            forSearch = forSearch.substring(prefix.end(1));
        }
        for (int i = 0; i < numbered.size(); i++) {
            Matcher m = numbered.get(i).getKey().matcher(forSearch);
            if (!m.lookingAt())
                continue;
            type.delim = m.group("delim");
            if (type.delim == null) type.delim = "";
            type.number = Arrays.stream(m.group(1).split(type.delim == null ? "\n" : Pattern.quote(type.delim))).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            type.type = type.labelPrefix == null ? Type.Numbered : Type.NumberedLabel;
            type.cmp = numbered.get(i).getValue();
            type.numberType = i;
            return;
        }
    }

    public String getHeaders() {
        StringBuilder res = new StringBuilder();
        if (sons != null && !sons.isEmpty()) {
            res.append(toString());
            for (String s : sons.stream().map(Block::getHeaders).collect(Collectors.joining("\n")).split("\n"))
                if (!s.isEmpty())
                    res = res.append("\n    ").append(s);
        }
        return res.toString();
    }

    public boolean looksLikeFormula()
    {
        int y = 0;
        int n = 0;
        for (Line line : lines) {
            if (line.looksLikeFormula())
                y++;
            else
                n++;
        }
        return y > n;
    }
}
