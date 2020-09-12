package cn.how2j.trend.web;

import cn.how2j.trend.pojo.IndexData;
import cn.how2j.trend.pojo.Profit;
import cn.how2j.trend.pojo.Trade;
import cn.how2j.trend.service.BackTestService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class BackTestController {

    @Autowired BackTestService backTestService;

    @GetMapping("/simulate/{code}")
    @CrossOrigin
    public Map<String, Object> backTest(@PathVariable("code") String code) throws Exception {
        List<IndexData> indexDataList = backTestService.listIndexData(code);
        Map<String, Object> result = new HashMap<>();
        result.put("IndexDataList", indexDataList);
        return result;
    }

    /**
     * @param code 指数代码
     * @param strStartDate 起始日期的字符串
     * @param strEndDate 终止日期的字符串
     * @return 将起始日期到终止日期间的indexData放入Map中
     * @throws Exception
     */
    @GetMapping("/simulate/{code}/{maPeriod}/{buyThreshold}/{sellThreshold}/{serviceCharge}/{startDate}/{endDate}")
    @CrossOrigin
    public Map<String, Object> backTest(@PathVariable("code") String code,
                                        @PathVariable("maPeriod") int period,
                                        @PathVariable("buyThreshold") float buyThreshold,
                                        @PathVariable("sellThreshold") float sellThreshold,
                                        @PathVariable("serviceCharge") float serviceCharge,
                                        @PathVariable("startDate") String strStartDate,
                                        @PathVariable("endDate") String strEndDate) throws Exception{
        List<IndexData> indexDataList = backTestService.listIndexData(code);
        String indexStartDate = indexDataList.get(0).getDate();
        String indexEndDate = indexDataList.get(indexDataList.size() - 1).getDate();
        indexDataList = filterByDateRange(indexDataList, strStartDate, strEndDate);

        //int period = 20;
        //float sellRate = 0.95f;
        //float buyRate = 1.05f;
        //float serviceCharge = 0f;

        Map<String, Object> resultMap = backTestService.simulate(period, buyThreshold, sellThreshold,
                serviceCharge, indexDataList);
        List<Profit> profits = (List<Profit>) resultMap.get("profits");
        //List<Trade> trades = (List<Trade>) resultMap.get("trades");

        //计算指数和趋势投资的综合收益率和年化收益率
        float years = backTestService.getYears(indexDataList);
        float indexReturnOverall = indexDataList.get(indexDataList.size() - 1).getClosePoint() /
                indexDataList.get(0).getClosePoint() - 1;
        float indexReturnAnnual = (float) Math.pow(1 + indexReturnOverall, 1 / years) - 1;
        float trendReturnOverall = profits.get(profits.size() - 1).getValue() / profits.get(0).getValue() - 1;
        float trendReturnAnnual = (float) Math.pow(1 + trendReturnOverall, 1 / years) - 1;

        //Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("indexDataList", indexDataList);
        resultMap.put("indexStartDate", indexStartDate);
        resultMap.put("indexEndDate", indexEndDate);
        resultMap.put("years", years);
        resultMap.put("indexReturnOverall", indexReturnOverall);
        resultMap.put("indexReturnAnnual", indexReturnAnnual);
        resultMap.put("trendReturnOverall", trendReturnOverall);
        resultMap.put("trendReturnAnnual", trendReturnAnnual);
        //resultMap.put("profits", profits);
        //resultMap.put("trades", trades);

        return resultMap;
    }


    /**
     * @param fullIndexData 利用backTestService得到的所有的indexData列表
     * @param strStartDate 选中的起始时间
     * @param strEndDate 选中的终止时间
     * @return 当起始日期和终止日期不为空时，返回该区间段内的indexData列表
     */
    public List<IndexData> filterByDateRange(List<IndexData> fullIndexData, String strStartDate, String strEndDate) {
        if (StrUtil.isBlankOrUndefined(strStartDate) || StrUtil.isBlankOrUndefined(strEndDate)) return fullIndexData;
        
        List<IndexData> result = new ArrayList<>();
        Date startDate = DateUtil.parseDate(strStartDate);
        Date endDate = DateUtil.parseDate(strEndDate);

        for (IndexData indexData : fullIndexData) {
            Date indexDataDate = DateUtil.parseDate(indexData.getDate());
            if (indexDataDate.getTime() > startDate.getTime() && indexDataDate.getTime() < endDate.getTime()) {
                result.add(indexData);
            }
        }

        return result;
    }


}
