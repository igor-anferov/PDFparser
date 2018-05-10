public class OnPagePosition implements Comparable<OnPagePosition> {
    public int page;
    public float xMin, xMax, yMin, yMax;

    public OnPagePosition clone() {
        OnPagePosition c = new OnPagePosition();
        c.page = page;
        c.xMin = xMin;
        c.xMax = xMax;
        c.yMin = yMin;
        c.yMax = yMax;
        return c;
    }

    public boolean PlacedBefore(OnPagePosition other)
    {
        if (page < other.page)
            return true;
        if (page > other.page)
            return false;
        if (yMax <= other.yMin)
            return true;
        if (other.yMax <= yMin)
            return false;
        if (xMax <= other.xMin)
            return true;
        if (other.xMax <= xMin)
            return false;
        return yMin < other.yMin;
    }

    @Override
    public int compareTo(OnPagePosition o) {
        if (PlacedBefore(o))
            return -1;
        if (o.PlacedBefore(this))
            return 1;
        return 0;
    }

    public void ExtendTo(OnPagePosition other)
    {
        assert (page == other.page);
        if (other.xMin < xMin)
            xMin = other.xMin;
        if (other.xMax > xMax)
            xMax = other.xMax;
        if (other.yMin < yMin)
            yMin = other.yMin;
        if (other.yMax > yMax)
            yMax = other.yMax;
    }
}
