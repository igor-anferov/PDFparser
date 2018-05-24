package com.github.igor_anferov.PDFparser;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Line {
    @XmlValue
    public String text;
    @XmlTransient
    public List<Style> styles;
    @XmlTransient
    public List<OnPagePosition> onPagePositions;

    public String toString() {
        return text;
    }

    static private Pattern noHeadingAndTrailingSpaces = Pattern.compile("\\S+(?:\\s+\\S+)*");

    public void Trim()
    {
        Matcher m = noHeadingAndTrailingSpaces.matcher(text);
        if (!m.find()) {
            text = "";
            styles = new ArrayList<>();
            onPagePositions = new ArrayList<>();
        } else {
            text = text.substring(m.start(), m.end());
            styles = styles.subList(m.start(), m.end());
            onPagePositions = onPagePositions.subList(m.start(), m.end());
        }
        assert (text.length() == styles.size() && text.length() == onPagePositions.size());
    }

    public void AppendWord(Line other)
    {
        if (other.text.isEmpty())
            return;
        if (text.isEmpty()) {
            text = other.text;
            styles = other.styles;
            onPagePositions = other.onPagePositions;
        } else {
            text += " " + other.text;
            styles.add(styles.get(styles.size()-1));
            styles.addAll(other.styles);
            OnPagePosition pos = new OnPagePosition();
            pos.page = onPagePositions.get(onPagePositions.size()-1).page;
            pos.yMin = onPagePositions.get(onPagePositions.size()-1).yMin;
            pos.yMax = onPagePositions.get(onPagePositions.size()-1).yMax;
            pos.xMin = onPagePositions.get(onPagePositions.size()-1).xMax;
            pos.xMax = other.onPagePositions.get(0).xMin;
            onPagePositions.add(pos);
            onPagePositions.addAll(other.onPagePositions);
        }
        assert (text.length() == styles.size() && text.length() == onPagePositions.size());
    }

    public void Append(Line other)
    {
        text += other.text;
        styles.addAll(other.styles);
        onPagePositions.addAll(other.onPagePositions);
    }

    private Line getLastChar() {
        if (text.isEmpty())
            return null;
        Line ch = new Line();
        ch.text = text.substring(text.length() - 1);
        ch.styles = styles.subList(styles.size() - 1, styles.size());
        ch.onPagePositions = onPagePositions.subList(onPagePositions.size() - 1, onPagePositions.size());
        return ch;
    }

    private Line getFirstChar() {
        if (text.isEmpty())
            return null;
        Line ch = new Line();
        ch.text = text.substring(0, 1);
        ch.styles = styles.subList(0, 1);
        ch.onPagePositions = onPagePositions.subList(0, 1);
        return ch;
    }

    public int getFirstPage()
    {
        return getFirstChar().onPagePositions.get(0).page;
    }

    public int getLastPage()
    {
        return getLastChar().onPagePositions.get(0).page;
    }

    static private Pattern endsWithHyphen = Pattern.compile("^.*\\w(‐|-|–)$", Pattern.UNICODE_CHARACTER_CLASS);

    public boolean EndsWithHyphen()
    {
        return endsWithHyphen.matcher(text).matches();
    }

    public void Merge(Line other)
    {
        if (EndsWithHyphen()) {
            text = text.substring(0, text.length()-1);
            styles.remove(styles.size() - 1);
            onPagePositions.remove(onPagePositions.size() - 1);
        } else {
            Line space = getLastChar();
            if (space != null) {
                space.text = " ";
                Append(space);
            }
        }
        Append(other);
    }

    boolean isEmpty()
    {
        return text.isEmpty();
    }

    public Style getMedianStyle()
    {
        Map<Style, Integer> diagramm = new HashMap<>();
        for (Style s : styles)
            diagramm.merge(s, 1, Integer::sum);
        Style common = null;
        Integer commonCnt = 0;
        for (Map.Entry<Style, Integer> styleIntegerEntry : diagramm.entrySet())
            if (styleIntegerEntry.getValue() >= commonCnt) {
                commonCnt = styleIntegerEntry.getValue();
                common = styleIntegerEntry.getKey();
            }
        return common;
    }

    public float xMin()
    {
        assert (!isEmpty());
        float res = onPagePositions.get(0).xMin;
        for (OnPagePosition onPagePosition : onPagePositions)
            if (onPagePosition.xMin < res)
                res = onPagePosition.xMin;
        return res;
    }

    public float xMax()
    {
        assert (!isEmpty());
        float res = onPagePositions.get(0).xMax;
        for (OnPagePosition onPagePosition : onPagePositions)
            if (onPagePosition.xMax > res)
                res = onPagePosition.xMax;
        return res;
    }

    public float yMin()
    {
        assert (!isEmpty());
        float res = onPagePositions.get(0).yMin;
        for (OnPagePosition onPagePosition : onPagePositions)
            if (onPagePosition.yMin < res)
                res = onPagePosition.yMin;
        return res;
    }

    public float yMax()
    {
        assert (!isEmpty());
        float res = onPagePositions.get(0).yMax;
        for (OnPagePosition onPagePosition : onPagePositions)
            if (onPagePosition.yMax > res)
                res = onPagePosition.yMax;
        return res;
    }

    public float getEps() {
        float eps = Float.MAX_VALUE;
        for (OnPagePosition onPagePosition : onPagePositions) {
            if (onPagePosition.xMax - onPagePosition.xMin < eps)
                eps = onPagePosition.xMax - onPagePosition.xMin;
        }
        return min(eps, Width() / 80f);
    }

    public float Width()
    {
        return xMax() - xMin();
    }

    public float Height()
    {
        return yMax() - yMin();
    }

    public boolean IsLeftAlignedWith(Line other)
    {
        float eps = max(Width(), other.Width()) / 80;
        return abs(xMin() - other.xMin()) < eps;
    }

    public boolean IsRightAlignedWith(Line other)
    {
        float eps = max(Width(), other.Width()) / 80;
        return abs(xMax() - other.xMax()) < eps;
    }

    public boolean IsCenterAlignedWith(Line other)
    {
        float eps = max(Width(), other.Width()) / 80;
        return abs((other.xMin() - xMin()) - (xMax() - other.xMax())) < eps;
    }

    public boolean IsFullAlignedWith(Line other)
    {
        return IsRightAlignedWith(other) && IsLeftAlignedWith(other);
    }

    public Style.AlignType GetAlignWith(Line other)
    {
        if (IsFullAlignedWith(other))
            return Style.AlignType.Full;
        if (IsCenterAlignedWith(other))
            return Style.AlignType.Center;
        if (IsLeftAlignedWith(other))
            return Style.AlignType.Left;
        if (IsRightAlignedWith(other))
            return Style.AlignType.Right;
        return Style.AlignType.Unknown;
    }

    public boolean Near(Line other)
    {
        return     getLastPage() == other.getFirstPage()
                && (other.yMin() <= yMin() && yMin() <= other.yMax()
                    || abs(yMax() - other.yMin()) < 2 * min(Height(), other.Height()))
                ||
                   getFirstPage() == other.getLastPage()
                && (yMin() <= other.yMin() && other.yMin() <= yMax()
                    || abs(yMin() - other.yMax()) < 2 * min(Height(), other.Height()));
    }

    interface AlmostEquals {
        public boolean AlmostEquals(String o1, String o2);
    }

    interface AlmostEqualsAndComparator extends AlmostEquals, Comparator<String> {}

    static class IntsAlmostEquals implements AlmostEqualsAndComparator {
        @Override
        public boolean AlmostEquals(String o1, String o2) {
            if (abs(Integer.parseInt(o1) - Integer.parseInt(o2)) < 10)
                return true;
            return false;
        }
        @Override
        public int compare(String o1, String o2) {
            Integer o1int = Integer.parseInt(o1);
            Integer o2int = Integer.parseInt(o2);
            return o1int < o2int ? -1 : o1int > o2int ? 1 : 0;
        }
    }

    static class RomanNumsAlmostEquals implements AlmostEqualsAndComparator {
        @Override
        public boolean AlmostEquals(String o1, String o2) {
            return StringUtils.getLevenshteinDistance(o1, o2) <= 4;
        }

        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    }

    static class StringsAreEqual implements AlmostEqualsAndComparator {
        @Override
        public boolean AlmostEquals(String o1, String o2) {
            return o1.equals(o2);
        }

        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    }

    public boolean HaveSameStyles(Line other)
    {
        Set<Style> myStyles = new HashSet<>();
        myStyles.addAll(styles);
        Set<Style> hisStyles = new HashSet<>();
        hisStyles.addAll(other.styles);
        Set<Style> myButNotHisStyles = new HashSet<>(myStyles);
        myButNotHisStyles.removeAll(hisStyles);
        if (myStyles.equals(myButNotHisStyles))
            return false;
        if (!getMedianStyle().almostEquals(other.getMedianStyle()))
            return false;
        return true;
    }

    static List<Pair<String, AlmostEqualsAndComparator>> subs = new ArrayList<>();
    static {
        subs.add(new Pair<>("[^1234567890]+", new IntsAlmostEquals()));
        subs.add(new Pair<>("[^IVX]+", new RomanNumsAlmostEquals()));
        subs.add(new Pair<>("[^ivx]+", new RomanNumsAlmostEquals()));
        subs.add(new Pair<>("[\\pN\\pP\\pZxviXVI]+", new StringsAreEqual()));
    }
    public boolean AlmostEquals(Line other)
    {
        if (!HaveSameStyles(other))
            return false;
        for (Pair<String, AlmostEqualsAndComparator> sub : subs) {
            String[] aNums = text.split(sub.getKey());
            String[] bNums = other.text.split(sub.getKey());
            aNums = Arrays.stream(aNums).filter(s -> !s.isEmpty()).toArray(String[]::new);
            bNums = Arrays.stream(bNums).filter(s -> !s.isEmpty()).toArray(String[]::new);
            if (aNums.length != bNums.length)
                return false;
            Arrays.sort(aNums, sub.getValue());
            Arrays.sort(bNums, sub.getValue());
            for (int i = 0; i < aNums.length; i++)
                if (!sub.getValue().AlmostEquals(aNums[i], bNums[i]))
                    return false;
        }
        return true;
    }

    static Pattern formulaChars = Pattern.compile("[\\p{Sm}\\p{N}\\p{P}]", Pattern.UNICODE_CHARACTER_CLASS);

    public boolean looksLikeFormula()
    {

        Matcher m = formulaChars.matcher(text);
        int count = 0;
        while (m.find())
            count++;
        if (count > text.length() / 3)
            return true;
        return false;
    }
}
