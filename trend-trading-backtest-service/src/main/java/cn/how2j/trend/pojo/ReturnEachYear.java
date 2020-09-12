package cn.how2j.trend.pojo;

/**
 * 指数投资和趋势投资在各个年份下的收益率
 */
public class ReturnEachYear {
    private int year;
    private float indexReturn;
    private float trendReturn;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public float getIndexReturn() {
        return indexReturn;
    }

    public void setIndexReturn(float indexReturn) {
        this.indexReturn = indexReturn;
    }

    public float getTrendReturn() {
        return trendReturn;
    }

    public void setTrendReturn(float trendReturn) {
        this.trendReturn = trendReturn;
    }

    @Override
    public String toString() {
        return "ReturnEachYear{" +
                "year=" + year +
                ", indexReturn=" + indexReturn +
                ", trendReturn=" + trendReturn +
                '}';
    }
}
