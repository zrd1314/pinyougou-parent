package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.*;
import java.util.logging.Filter;

//配置超时时间 因为 dubbox去 与solr交互 ，默认服务如果超过1秒没有返回，dubbox会报错
@Service(timeout = 5000)
public class ItemSearchServiceImpl implements ItemSearchService {
    @Autowired
    private SolrTemplate solrTemplate ;

    @Autowired
    private RedisTemplate redisTemplate ;

    @Override
    public Map search(Map searchMap) {
        Map map = new HashMap();
//        Query query = new SimpleQuery("*:*");
//        //使用复制域的字段来查询
//        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keyword"));
//
//        query.addCriteria(criteria);
//        ScoredPage<TbItem> tbItemsPage = solrTemplate.queryForPage(query, TbItem.class);
//        map.put("rows",tbItemsPage.getContent());
//        System.out.println("map=="+map);
        //去除关键字搜索的空格
        String keyword  = (String) searchMap.get("keyword");
        searchMap.put("keyword",keyword.replace(" ",""));
        //1.查询列表
        map.putAll(searchList(searchMap));
        //查询分类列表信息 相当与 select categroed from table group by
        //2.分组查询 商品分类列表
      List<String> categoryList =  searchCategoryList(searchMap);
      map.put("categoryList",categoryList);
      // 3 根据 分类名称 查询 品牌和规格
        String category= (String) searchMap.get("category");
        if(!category.equals("")){ //前台传了分类
            map.putAll(searchBrandAndSpecList(category));
        }else{
            if(categoryList.size()>0){
                map.putAll(searchBrandAndSpecList(categoryList.get(0)));
            }
        }


        return map;
    }

    @Override
    public void importData(List list) {
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }

    @Override
    public void deleteBygoodsIds(List goodsIds) {
        Query query = new SimpleQuery("*:*");
        Criteria criteria =new Criteria("item_goodsid").in(goodsIds);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();

    }

    /**
     * 查询品牌和规格
     * @param
     */
    private Map searchBrandAndSpecList(String categoryName) {
        Map map = new HashMap();
        //根据 分类名称 查询 模板id
        Long templateId =(Long)redisTemplate.boundHashOps("itemCatList").get(categoryName);
        System.out.println("templateId=="+templateId);


        if(templateId!=null){
            //模板id 查询 品牌和规格
            List brandList=(List) redisTemplate.boundHashOps("brandList").get(templateId);
            List specList=(List) redisTemplate.boundHashOps("specList").get(templateId);
            map.put("brandList",brandList);
            map.put("specList",specList);
            System.out.println("品牌列表条数："+brandList.size());
            System.out.println("规格列表："+specList);
        }
        return  map ;
    }

    /**
     * 查询分类信息
     * @param searchMap
     */
    private List<String> searchCategoryList(Map searchMap) {
        List<String> categoryList = new ArrayList<>();
        Query query = new SimpleQuery("*:*");
        //分组信息根据 item_category 域分组
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(groupOptions);
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keyword"));
        query.addCriteria(criteria);
        //获取分组页
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //这个要跟 上面根据哪个域 分组 要一致
        //获取分组结果对象
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        //获取分组入口页
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        //获取分组入口集合
        List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
        for(GroupEntry<TbItem> entry:entryList  ){
            categoryList.add(entry.getGroupValue()	);	//将分组的结果添加到返回值中
        }
        return categoryList ;

    }


    private Map searchList(Map searchMap){
        Map map = new HashMap();
        //高亮选项初始化
        HighlightQuery query = new SimpleHighlightQuery();
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");//高亮域
        highlightOptions.setSimplePrefix("<em style='color:red'>");
        highlightOptions.setSimplePostfix("</em>");
        query.setHighlightOptions(highlightOptions);//为查询对象设置高亮选项
        //关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keyword"));
        query.addCriteria(criteria);

        //根据商品分类进行过滤
        if(!"".equals(searchMap.get("category"))){
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //根据品牌进行过滤
        if(!"".equals(searchMap.get("brand"))){
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //根据规格进行过滤
        if(searchMap.get("spec")!= null){
            Map<String,String>    specMap =  (Map<String,String> )searchMap.get("spec");
            for(Map.Entry<String,String> spec:specMap.entrySet()){
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_spec_"+spec.getKey()).is(spec.getValue());
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }

        }

        //根据价格区间进行搜索

        if(!"".equals(searchMap.get("price"))){ //说明用户选择了 价格搜索
            String[] price = ((String)searchMap.get("price")).split("-");
            if(!"0".equals(price[0])){
                //价格下线
                FilterQuery filterQuery =  new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }

            if(!"*".equals(price[1])){
                //价格上线
                FilterQuery filterQuery =  new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }

        }

        //  排序
        String sortField = (String) searchMap.get("sortField");
        String sort = (String) searchMap.get("sort");

        if(!"".equals(sortField)){ //选择了排序字段
            if("ASC".equals(sort)){
                Sort sorte = new Sort(Sort.Direction.ASC,"item_"+sortField);
                query.addSort(sorte);
            }else if("DESC".equals(sort)){
                Sort sorte = new Sort(Sort.Direction.DESC,"item_"+sortField);
                query.addSort(sorte);
            }
        }



        //分页
        //提取页码 和 每页显示条数
        Integer pageNo = (Integer) searchMap.get("pageNo");
        if(pageNo ==null){
            pageNo = 1 ; //默认第一页
        }
        Integer pageSize=(Integer) searchMap.get("pageSize");//每页记录数
        if(pageSize == null) {
            pageSize = 20 ;// 默认20条
        }
        query.setOffset((pageNo-1)*pageSize);
        query.setRows(pageSize);



        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        //高亮入口集合(每条记录的高亮入口)
        List<HighlightEntry<TbItem>> entryList = page.getHighlighted();
        for(HighlightEntry<TbItem> entry:entryList){
            //获取高亮列表(高亮域的个数)
            List<HighlightEntry.Highlight> highlights = entry.getHighlights();
           // System.out.println("高亮列表个数为"+highlights.size());
            for (HighlightEntry.Highlight highlight : highlights) {
                //snipplet<em style='color:red'>华为</em> G620 精致黑 联通4G手机
                List<String> snipplets = highlight.getSnipplets();
                for (String snipplet : snipplets) {
                  //  System.out.println("snipplet"+snipplet);
                }
            }
            if(highlights.size()>0 && highlights.get(0).getSnipplets().size()>0){
                TbItem item = entry.getEntity();
                item.setTitle(highlights.get(0).getSnipplets().get(0));

            }
        }
        map.put("rows",page.getContent());
        map.put("totalPages",page.getTotalPages()) ;//总页数
        map.put("total",page.getTotalElements()) ;//返回总记录数
        return map;
    }
}
