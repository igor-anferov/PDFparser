import javafx.util.Pair;
import org.lionsoul.jcseg.util.Sort;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.max;

public class Document {
    public List<Block> blocks;
    public List<Integer> eopBlocks;

    class BlocksComparator implements Comparator<Block> {
        @Override
        public int compare(Block o1, Block o2) {
            if (o1 == o2)
                return 0;
            if (blocks.indexOf(o1) < blocks.indexOf(o2))
                return -1;
            else
                return 1;
        }
    }

    Document()
    {
        blocks = new ArrayList<>();
        eopBlocks = new ArrayList<>();
        eopBlocks.add(-1);
    }

    public void Append(Block b)
    {
        blocks.add(b);
    }

    private Pair<Integer, Integer> getLineBefore(Pair<Integer, Integer> prevLine)
    {
        for (int block = prevLine.getKey(),
             line = prevLine.getValue() - 1;
             block >= 0;
             line = blocks.get(block).lines.size()-1)
        {
            if (block < blocks.size() && line >= 0)
                return new Pair<>(block, line);
            if (--block < 0)
                break;
        }
        return null;
    }

    private Pair<Integer, Integer> getLineAfter(Pair<Integer, Integer> prevLine)
    {
        for (int block = prevLine.getKey(),
             line = prevLine.getValue() + 1;
             block < blocks.size();
             line = 0)
        {
            if (block >= 0 && line < blocks.get(block).lines.size())
                return new Pair<>(block, line);
            if (++block >= blocks.size())
                break;
        }
        return null;
    }

    private Line getLine(Pair<Integer, Integer> line)
    {
        return blocks.get(line.getKey()).lines.get(line.getValue());
    }

    enum matchingDirection {
        matchUp,
        matchDown
    }
    private Set<Pair<Integer, Integer>> match(matchingDirection direction,
                                              Pair<Integer, Integer> line1,
                                              Pair<Integer, Integer> line2
                                              )
    {
        Set<Pair<Integer, Integer>> ret = new HashSet<>();
        for (Pair<Integer, Integer> l1 = direction == matchingDirection.matchUp ? getLineBefore(line1): getLineAfter(line1),
                                    l2 = direction == matchingDirection.matchUp ? getLineBefore(line2): getLineAfter(line2);
             l1 != null && l2 != null;
             l1 = direction == matchingDirection.matchUp ? getLineBefore(l1): getLineAfter(l1),
             l2 = direction == matchingDirection.matchUp ? getLineBefore(l2): getLineAfter(l2))
            if (getLine(l1).AlmostEquals(getLine(l2))) {
                ret.add(l1);
                ret.add(l2);
            } else {
                break;
            }
        return ret;
    }

    private Set<Pair<Integer, Integer>> needsToBeRemoved(Integer eopBlocksIndex)
    {
        Set<Pair<Integer, Integer>> ret = new HashSet<>();
        for (int i = eopBlocksIndex + 1; i <= min(eopBlocks.size() - 1, eopBlocksIndex + 10); i++) {
            ret.addAll(match(matchingDirection.matchUp, new Pair<>(eopBlocks.get(i) + 1, 0), new Pair<>(eopBlocks.get(eopBlocksIndex) + 1, 0)));
            ret.addAll(match(matchingDirection.matchDown, new Pair<>(eopBlocks.get(i) + 1, -1), new Pair<>(eopBlocks.get(eopBlocksIndex) + 1, -1)));
        }
        return ret;
    }

    class reversePairSort implements Comparator<Pair<Integer, Integer>> {
        @Override
        public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
            if (o1.getKey() > o2.getKey())
                return -1;
            if (o1.getKey() < o2.getKey())
                return 1;
            if (o1.getValue() > o2.getValue())
                return -1;
            if (o1.getValue() < o2.getValue())
                return 1;
            return 0;
        }
    }

    public void RemoveHeadersAndFooters()
    {
        Set<Pair<Integer, Integer>> markedToRemove = new TreeSet<>(new reversePairSort());
        for (int i = 0; i < eopBlocks.size(); i++)
            markedToRemove.addAll(needsToBeRemoved(i));
        for (Integer eopBlock : eopBlocks) {
            int blockNum = eopBlock;
            for (;
                 blockNum >= 0 && markedToRemove.contains(new Pair<>(blockNum, 0));
                 --blockNum);
            if (blockNum >= 0)
                blocks.get(blockNum).endOfPage = true;
        }
        for (Pair<Integer, Integer> blockLine : markedToRemove) {
            blocks.get(blockLine.getKey()).lines.remove((int)blockLine.getValue());
            if (blocks.get(blockLine.getKey()).lines.isEmpty())
                blocks.remove((int)blockLine.getKey());
        }
        return;
    }

    private float getMedianFirstLinePos(int blockNum, int searchInterval)
    {
        List<Float> positions = new ArrayList<>();
        for (int i = max(blockNum - searchInterval, 0); i <= min(blockNum + searchInterval, blocks.size() - 1); i++) {
            if (i == blockNum)
                continue;
            Block b = blocks.get(i);
            if (b.lines.isEmpty())
                continue;
            positions.add(b.lines.get(0).xMin());
        }
        Float[] posArr = positions.toArray(new Float[]{});
        Sort.quickSelect(posArr, posArr.length / 2);
        return posArr[posArr.length / 2];
    }

    public void mergeFirstLinesWithRest()
    {
        for (int i = 0; i < blocks.size() - 1; i++) {
            if (blocks.get(i).lines.size() != 1)
                continue;
            if (blocks.get(i+1).lines.isEmpty())
                continue;
            if (!blocks.get(i+1).isLeftAligned())
                continue;
            if (blocks.get(i).getFirstLine().GetAlignWith(blocks.get(i+1).getFirstLine()) != Style.AlignType.Unknown)
                continue;
            if (abs(blocks.get(i).lines.get(0).xMin() - getMedianFirstLinePos(i, 5))
                        > max(blocks.get(i).lines.get(0).Width(), blocks.get(i+1).getMaxLineWidth()) / 50)
                continue;
            if (!blocks.get(i).getMedianStyle().almostEquals(blocks.get(i+1).getMedianStyle()))
                continue;
            if (!blocks.get(i).endOfPage && !blocks.get(i).lines.get(0).Near(blocks.get(i+1).lines.get(0)))
                continue;
            blocks.get(i).Append(blocks.get(i+1));
            blocks.remove(i+1);
        }
    }

    public void mergeEopBlocks()
    {
        for (int i = 0; i < blocks.size() - 1; i++) {
            if (!blocks.get(i).endOfPage)
                continue;
            if (!blocks.get(i).getMedianStyle().almostEquals(blocks.get(i+1).getMedianStyle()))
                continue;
            if (       blocks.get(i).alignment != Style.AlignType.Unknown
                    && blocks.get(i).alignment != Style.AlignType.Multiple
                    && blocks.get(i+1).alignment != Style.AlignType.Multiple
                    &&
                    (  blocks.get(i).alignment == blocks.get(i+1).alignment
                    || blocks.get(i).getLastLine().GetAlignWith(blocks.get(i+1).getFirstLine()) == blocks.get(i).alignment)
                    ||
                    ( (blocks.get(i).alignment == Style.AlignType.Full
                    || blocks.get(i).alignment == Style.AlignType.Multiple)
                    && blocks.get(i+1).lines.size() == 1
                    && blocks.get(i+1).getFirstLine().GetAlignWith(blocks.get(i).getLastLine()) == Style.AlignType.Left)
                    ||
                    (blocks.get(i+1).alignment == Style.AlignType.Full
                    || blocks.get(i+1).alignment == Style.AlignType.Multiple)
                    && blocks.get(i).lines.size() == 1
                    && abs(blocks.get(i).getFirstLine().xMin() - getMedianFirstLinePos(i, 5))
                               < max(blocks.get(i).getFirstLine().Width(), blocks.get(i+1).getMaxLineWidth()) / 50
                    && blocks.get(i+1).getFirstLine().GetAlignWith(blocks.get(i).getLastLine()) == Style.AlignType.Right
                    ||
                       blocks.get(i+1).alignment != Style.AlignType.Unknown
                    && blocks.get(i+1).alignment != Style.AlignType.Multiple
                    && blocks.get(i).getLastLine().GetAlignWith(blocks.get(i+1).getFirstLine()) == blocks.get(i+1).alignment) {
                blocks.get(i).Append(blocks.get(i + 1));
                blocks.remove(i + 1);
            }
        }
    }

    public void FillBlocksAlignments()
    {
        for (Block block : blocks)
            block.FillAlignment();
        if (blocks.size() <= 1)
            return;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).alignment != Style.AlignType.Unknown && blocks.get(i).alignment != Style.AlignType.Multiple
                    && !(blocks.get(i).lines.size() <= 3 && blocks.get(i).alignment == Style.AlignType.Full))
                continue;
            float xMin = Float.MAX_VALUE;
            float xMax = Float.MIN_VALUE;
            for (int j = max(0, i - 7); j < min(blocks.size(), i + 7); j++) {
                if (i == j
                    || (j < i && blocks.get(j).getFirstPage() != blocks.get(i).getFirstPage())
                    || (j > i && blocks.get(j).getLastPage() != blocks.get(i).getLastPage()))
                    continue;
                if (blocks.get(j).xMin() < xMin)
                    xMin = blocks.get(j).xMin();
                if (blocks.get(j).xMax() > xMax)
                    xMax = blocks.get(j).xMax();
            }
            float selfXmin = blocks.get(i).xMin();
            float selfXmax = blocks.get(i).xMax();
            float eps = max(abs(selfXmax - selfXmin), abs(xMax - xMin)) / 60;
            if (blocks.get(i).lines.size() == 1 || (blocks.get(i).lines.size() <= 3 && blocks.get(i).alignment == Style.AlignType.Full)) {
                if (abs((selfXmin - xMin) - (xMax - selfXmax)) < eps)
                    blocks.get(i).alignment = Style.AlignType.Center;
                if (abs(xMin - selfXmin) < eps || abs(selfXmin - getMedianFirstLinePos(i, 5)) < eps)
                    if (blocks.get(i).alignment != Style.AlignType.Unknown && abs(xMax - selfXmax) < eps) {
                        blocks.get(i).alignment = Style.AlignType.Multiple;
                        continue;
                    } else {
                        blocks.get(i).alignment = Style.AlignType.Left;
                    }
                if (abs(xMax - selfXmax) < eps)
                    if (blocks.get(i).alignment != Style.AlignType.Unknown) {
                        blocks.get(i).alignment = Style.AlignType.Multiple;
                        continue;
                    } else {
                        blocks.get(i).alignment = Style.AlignType.Right;
                    }
            } else if (blocks.get(i).lines.size() == 2 &&
                    abs(blocks.get(i).lines.get(0).xMin() - getMedianFirstLinePos(i, 5)) < eps &&
                    abs(blocks.get(i).lines.get(0).xMax() - xMax) < eps &&
                    abs(blocks.get(i).lines.get(1).xMin() - xMin) < eps)
                    blocks.get(i).alignment = Style.AlignType.Full;
        }
    }

    public void MergeLinesInsideBlocks()
    {
        for (Block block : blocks)
            block.MergeLines();
    }

    public Map<Style, Set<Block>> GetStylesHist()
    {
        Map<Style, Set<Block>> hist = new TreeMap<>();
        Map<Style, Set<Block>> histMultiAlign = new TreeMap<>();
        for (Block block : blocks) {
            Style style = block.getMedianStyle();
            style.align = block.alignment;
            Map<Style, Set<Block>> h = style.align == Style.AlignType.Multiple ? histMultiAlign : hist;
            if (!h.containsKey(style))
                h.put(style, new TreeSet<Block>(new BlocksComparator()));
            h.get(style).add(block);
        }
        for (Map.Entry<Style, Set<Block>> styleSetEntry : histMultiAlign.entrySet()) {
            TreeMap<Integer, Style> h = new TreeMap<>();
            for (Map.Entry<Style, Set<Block>> setEntry : hist.entrySet()) {
                if (setEntry.getKey().equalsIgnoringAlign(styleSetEntry.getKey()))
                    h.put(setEntry.getValue().size(), setEntry.getKey());
            }
            hist.merge(h.isEmpty() ? styleSetEntry.getKey() : h.lastEntry().getValue(), styleSetEntry.getValue(), (a, b) -> {a.addAll(b); return a;});
        }

        return hist;
    }

    public void FillBlocksTypes()
    {
        for (Block block : blocks)
            block.FillType();
    }
}
