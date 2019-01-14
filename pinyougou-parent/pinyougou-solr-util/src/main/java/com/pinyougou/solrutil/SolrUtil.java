package com.pinyougou.solrutil;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SolrUtil {
    @Autowired
    private TbItemMapper itemMapper ;

    @Autowired
    private SolrTemplate solrTemplate;


    public void importItemData(){
        //查询出审核通过的Item数据 放入solr
        TbItemExample itemExample = new TbItemExample();
        TbItemExample.Criteria criteria = itemExample.createCriteria();
        criteria.andStatusEqualTo("1");
        List<TbItem> tbItems = itemMapper.selectByExample(itemExample);
        //tbItems.forEach(System.out::println);
        for(TbItem item:tbItems){
            //{"机身内存":"16G","网络":"联通3G"}
            String spec = item.getSpec();
            Map map = JSON.parseObject(spec, Map.class);
            item.setMapSpec(map);
        }
        solrTemplate.saveBeans(tbItems);
        solrTemplate.commit();
    }

    public static void main(String[] args) {
        //classpath* 会扫描jar的配置文件
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
        SolrUtil solrUtil = context.getBean(SolrUtil.class);
        solrUtil.importItemData();
    }
}
