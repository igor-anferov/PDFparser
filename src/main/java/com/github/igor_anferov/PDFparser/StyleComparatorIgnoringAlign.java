package com.github.igor_anferov.PDFparser;

import java.util.Comparator;

public class StyleComparatorIgnoringAlign implements Comparator<Style> {
    @Override
    public int compare(Style o1, Style o2) {
        Style.AlignType o1align = o1.align;
        Style.AlignType o2align = o2.align;
        o1.align = o2.align = Style.AlignType.Unknown;
        int res = o1.compareTo(o2);
        o1.align = o1align;
        o2.align = o2align;
        return res;
    }
}
