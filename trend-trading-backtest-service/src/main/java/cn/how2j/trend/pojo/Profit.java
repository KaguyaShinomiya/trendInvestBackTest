package cn.how2j.trend.pojo;

/**
 * date 日期
 * value 累计收益
 */
public class Profit {
    private String date;
    private float value;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
