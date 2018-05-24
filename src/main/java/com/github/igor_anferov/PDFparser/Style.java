package com.github.igor_anferov.PDFparser;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.hash;

import org.apache.commons.lang3.StringUtils;

public class Style implements Comparable {
    enum AlignType {
        Unknown,
        Multiple,
        Right,
        Left,
        Full,
        Center,
    }

    String font;
    float size;
    AlignType align = AlignType.Unknown;

    @Override
    public int hashCode() {
        return font.hashCode() ^ hash(size) ^ align.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        Style other = (Style) obj;
        return font.equals(other.font) && size == other.size && align.equals(other.align);
    }

    public boolean equalsIgnoringAlign(Object obj) {
        Style other = (Style) obj;
        return font.equals(other.font) && size == other.size;
    }

    public boolean almostEquals(Style other)
    {
        if (size != other.size)
            return false;
        int diff = StringUtils.getLevenshteinDistance(font, other.font);
        int maxLen = max(font.length(), other.font.length());
        return diff < maxLen / 3;
    }

    @Override
    public int compareTo(Object o) {
        if (size > ((Style)o).size)
            return -1;
        if (size < ((Style)o).size)
            return 1;
        if (font.toLowerCase().contains("bold") && !((Style)o).font.toLowerCase().contains("bold"))
            return -1;
        if (!font.toLowerCase().contains("bold") && ((Style)o).font.toLowerCase().contains("bold"))
            return 1;
        if (align.ordinal() > ((Style)o).align.ordinal())
            return -1;
        if (align.ordinal() < ((Style)o).align.ordinal())
            return 1;
        if (font.toLowerCase().contains("italic") && !((Style)o).font.toLowerCase().contains("italic"))
            return -1;
        if (!font.toLowerCase().contains("italic") && ((Style)o).font.toLowerCase().contains("italic"))
            return 1;
        return -1 * font.compareTo(((Style)o).font);
    }
}