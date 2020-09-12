package cn.how2j.trend.service;

import cn.how2j.trend.pojo.Index;
import cn.how2j.trend.util.SpringContextUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
* 提供指数服务，使得访问/codes时能够返回所有指数的名称和代码数据
*/

//表示缓存的名称是 indexes, 保存到 redis 就会以 indexes 命名
@Service
@CacheConfig(cacheNames = "indexes")
public class IndexService {
    private List<Index> indexes;
    @Autowired
    RestTemplate restTemplate;

    //使用工具类RestTemplate 来获取 "http://127.0.0.1:8090/indexes/codes.json"地址
    //RestTemplate是Spring用于同步client端的核心类，简化了与http服务的通信，并满足RestFul原则，程序代码可以给它提供URL，并提取结果
    //更多用法参考https://www.cnblogs.com/javazhiyin/p/9851775.html


    //获取出来的内容是Map类型，所以需要map2Index把Map转换为Index对象
    //@Cacheable(key="'all_codes'") 表示保存到 redis时会使用'all_codes'作为key，注意这里是三引号

    @HystrixCommand(fallbackMethod = "thirdPartyNotConnected")
    public List<Index> fresh() {
        indexes = fetchIndexesFromThirdParty();
        //Cached的方法只会在第一次调用indexService实例的方法时生效，如果直接在这里写remove()、store()则
        //不会生效，所以需要自己手动再生成一个indexService实例，委托该实例完成接下来的两个方法
        IndexService indexService = SpringContextUtil.getBean(IndexService.class);
        indexService.remove();
        return indexService.store();
    }


    @CacheEvict(allEntries = true)
    public void remove() {

    }

    @Cacheable(key = "'all_codes'")
    public List<Index> store() {
        System.out.println(this);
        return indexes;
    }

    @Cacheable(key = "'all_codes'")
    public List<Index> get() {
        return CollUtil.toList();
    }

    public List<Index> fetchIndexesFromThirdParty() {
        List<Map> temp = restTemplate.getForObject("http://127.0.0.1:8090/indexes/codes.json", List.class);
        return map2Index(temp);
    }

    //如果fetch_indexes_from_third_part获取失败了，就自动调用 third_part_not_connected 并返回
    public List<Index> thirdPartyNotConnected() {
        System.out.println("third_part_not_connected()");
        Index index = new Index();
        index.setCode("000000");
        index.setName("无效指数代码");
        return CollectionUtil.toList(index);
    }

    public List<Index> map2Index(List<Map> temp) {
        List<Index> indexes = new ArrayList<>();
        for (Map map : temp) {
            String code = map.get("code").toString();
            String name = map.get("name").toString();
            Index index = new Index();
            index.setCode(code);
            index.setName(name);
            indexes.add(index);
        }
        return indexes;
    }
}
