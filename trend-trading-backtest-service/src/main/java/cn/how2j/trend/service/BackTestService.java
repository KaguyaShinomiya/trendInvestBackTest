package cn.how2j.trend.service;

import cn.how2j.trend.client.IndexDataClient;
import cn.how2j.trend.pojo.ReturnEachYear;
import cn.how2j.trend.pojo.IndexData;
import cn.how2j.trend.pojo.Profit;
import cn.how2j.trend.pojo.Trade;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BackTestService {

    @Autowired
    IndexDataClient indexDataClient;

    //注入indexDataClient，根据url中的code参数返回该指数的indexData列表
    public List<IndexData> listIndexData(String code) {
        List<IndexData> indexDataList = indexDataClient.getIndexData(code);
        Collections.reverse(indexDataList);

        return indexDataList;
    }


    /**
     * @param period 移动平均周期
     * @param sellThreshold 出售阈值，收盘价/高点低于该阈值则卖出
     * @param buyThreshold 购买阈值，收盘价/MA均价高于该阈值则买入
     * @param serviceCharge 手续费费率
     * @param indexDataList
     * @return 计算趋势投资的收益profit序列，放入列表profits中
     */
    public Map<String, Object> simulate(int period, float buyThreshold, float sellThreshold,
                                        float serviceCharge, List<IndexData> indexDataList) {
        List<Profit> profits = new ArrayList<>();
        List<Trade> trades = new ArrayList<>();
        float initCash = 1000; //初始现金
        float cash = initCash; //现金
        float share = 0; //仓位
        float value = 0; //持仓或持现总额
        float init = 0; //指数初始点位
        int profitCount = 0; //盈利次数
        float totalProfitRate = 0; //总盈利累积幅度
        float avgProfitRate = 0; //平均盈利幅度
        int lossCount = 0; //亏损次数
        float totalLossRate = 0; //总亏损累积幅度
        float avgLossRate = 0; //平均亏损幅度
        
        if (!indexDataList.isEmpty()) init = indexDataList.get(0).getClosePoint();

        for (int i = 0; i < indexDataList.size(); i++) {
            IndexData indexData = indexDataList.get(i);
            float closePoint = indexData.getClosePoint();
            float avg = getMA(i, period, indexDataList);
            float max = getMax(i, period, indexDataList);
            float increaseRate = closePoint / avg;
            float decreaseRate = closePoint / max;


            if (avg != 0) {
                //如果当天收盘价相比于均线的涨幅超过买点，则买入
                if (increaseRate > buyThreshold) {
                    //如果没买,则全仓买入
                    if (0 == share) {
                        share = cash / closePoint;
                        cash = 0;
                        //产生一笔新的交易，构建Trade对象
                        Trade trade = new Trade();
                        trade.setBuyDate(indexData.getDate());
                        trade.setBuyClosePoint(closePoint);
                        trade.setSellClosePoint(0);
                        trade.setSellDate("N/A");
                        trades.add(trade);
                    }
                }
                //如果当天收盘价相比于均价的降幅低于卖点，则卖出
                else if (decreaseRate < sellThreshold) {
                    //如果没卖，则售空
                    if (0 != share) {
                        cash = closePoint * share * (1 - serviceCharge);
                        share = 0;
                        //计算当前指数或现金的价值相比于最初持有的现金的价值的比例
                        float rate = value / initCash;
                        //完成一笔交易，需要从trades列表中取出最新的Trade对象，更新其收益率
                        Trade trade = trades.get(trades.size() - 1);
                        trade.setSellDate(indexData.getDate());
                        trade.setSellClosePoint(closePoint);
                        trade.setRate(rate);

                        //如果这笔交易是盈利的，将盈利幅度计入累积盈利幅度
                        if (trade.getSellClosePoint() >= trade.getBuyClosePoint()) {
                            totalProfitRate += trade.getSellClosePoint() / trade.getBuyClosePoint() - 1;
                            profitCount++;
                        }
                        //如果这笔交易是亏损的，将盈利幅度计入累积亏损幅度
                        else {
                            totalLossRate += trade.getSellClosePoint() / trade.getBuyClosePoint() - 1;
                            lossCount++;
                        }
                    }
                }
            }

            //计算当前持有的指数或现金的价值
            if (share != 0) {
                value = closePoint * share;
            }
            else {
                value = cash;
            }

            //计算当前指数或现金的价值相比于最初持有的现金的价值的比例
            float rate = value / initCash;

            Profit profit = new Profit();
            profit.setDate(indexData.getDate());
            profit.setValue(rate * init);

            //System.out.println("profit.value: " + profit.getValue());
            profits.add(profit);
        }

        //计算指数投资和趋势投资在各年的收益率
        List<ReturnEachYear> returnEachYearList= calcReturnEachYear(indexDataList, profits);

        //计算平均盈利幅度和平均亏损幅度
        avgProfitRate = totalProfitRate / profitCount;
        avgLossRate = totalLossRate / lossCount;

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("profits", profits);
        resultMap.put("trades", trades);
        resultMap.put("returnEachYearList", returnEachYearList);
        resultMap.put("profitCount", profitCount);
        resultMap.put("avgProfitRate", avgProfitRate);
        resultMap.put("lossCount", lossCount);
        resultMap.put("avgLossRate", avgLossRate);
        return resultMap;
    }


    /**
     * @param i indexDataList中的序号
     * @param period 移动平均周期，例如20天均线的ma为20
     * @param indexDataList
     * @return 指数收盘点位的移动平均
     */
    private float getMA(int i, int period, List<IndexData> indexDataList) {
        //注意哦，在第i天计算MA均线时，要从第i-1天的收盘价往前推，因为第i天当天的收盘价是未知的
        int start = i - 1 - period;
        int now = i - 1;

        if (start < 0) return 0;

        float sum = 0;
        //float avg = 0;
        for (int j = start; j < now; j++) {
            IndexData indexData = indexDataList.get(j);
            sum += indexData.getClosePoint();
        }

        float avg = sum / (now - start);
        return avg;

    }


    /**
     * @param i 第i天
     * @param period 移动平均周期
     * @param indexDataList
     * @return 从第i - 1天向前推一个移动平均周期下的收盘价最高点
     */
    private float getMax(int i, int period, List<IndexData> indexDataList) {
        int start = Math.max(0, i - 1 - period);
        int now = i - 1;
        float peak = 0;
        for (int j = start; j < now; j++) {
            IndexData indexData = indexDataList.get(j);
            peak = Math.max(indexData.getClosePoint(), peak);
        }
        return peak;
    }


    /**
     * @param indexDataList 指数数据的时间序列
     * @param profits 趋势投资的累计收益时间序列
     * @return 计算各年份下指数投资和趋势投资分别的收益率，返回相应的ReturnEachYear对象列表
     */
    private List<ReturnEachYear> calcReturnEachYear(List<IndexData> indexDataList,
                                                    List<Profit> profits) {
        List<ReturnEachYear> returnEachYearList = new ArrayList<>();
        String strStartDate = indexDataList.get(0).getDate();
        String strEndDate = indexDataList.get(indexDataList.size() - 1).getDate();

        Date startDate = DateUtil.parse(strStartDate);
        Date endDate = DateUtil.parse(strEndDate);

        int startYear = DateUtil.year(startDate);
        int endYear = DateUtil.year(endDate);

        for (int year = startYear; year <= endYear; year++) {
            ReturnEachYear returnEachYear = new ReturnEachYear();
            returnEachYear.setYear(year);

            float indexReturn = calcIndexReturn(year, indexDataList);
            float trendReturn = calcTrendReturn(year, profits);

            returnEachYear.setIndexReturn(indexReturn);
            returnEachYear.setTrendReturn(trendReturn);
            returnEachYearList.add(returnEachYear);
        }
        return returnEachYearList;
    }


    /**
     * @param year
     * @param indexDataList
     * @return 遍历indexData时间序列，找到时间位于该年份的indexData，计算该年份下的收益率
     */
    private float calcIndexReturn(int year, List<IndexData> indexDataList) {
        IndexData first = null;
        IndexData last = null;

        for (IndexData indexData : indexDataList) {
            String strDate = indexData.getDate();
            int currentYear = getYear(strDate);
            if (currentYear == year) {
                if (null == first) {
                    first = indexData;
                }
                last = indexData;
            }
        }
        return last.getClosePoint() / first.getClosePoint() - 1;
    }


    /**
     * @param year
     * @param profits
     * @return 遍历趋势投资累计收益时间序列，找到时间位于该年份的profit，计算该年份下的收益率
     */
    private float calcTrendReturn(int year, List<Profit> profits) {
        Profit first = null;
        Profit last = null;

        for (Profit profit : profits) {
            String strDate = profit.getDate();
            int currentYear = getYear(strDate);
            if (currentYear == year) {
                if (null == first) {
                    first = profit;
                }
                last = profit;
            }
        }
        return last.getValue() / first.getValue() - 1;
    }


    /**
     * @param date
     * @return 给出日期的字符串，返回年份
     */
    private int getYear(String date) {
        String strYear = StrUtil.subBefore(date, "-", false);
        return Convert.toInt(strYear);
    }

    /**
     * @param indexDataList 经过筛选后的indexData列表
     * @return 根据被筛选的indexData列表计算出以年为时间单位的时间长度
     */
    public float getYears(List<IndexData> indexDataList) {
        String strStartDate = indexDataList.get(0).getDate();
        String strEndDate = indexDataList.get(indexDataList.size() - 1).getDate();

        Date startDate = DateUtil.parse(strStartDate);
        Date endDate = DateUtil.parse(strEndDate);

        long days = DateUtil.between(startDate, endDate, DateUnit.DAY);
        float years = days / 365f;
        return years;
    }


}
