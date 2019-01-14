package com.pinyougou.search.service;

import java.util.List;
import java.util.Map;

/**
 * 搜索服务
 */
public interface ItemSearchService {

    public Map search(Map keywords);

    //导入数据到 solr
    public void importData(List list);

    //商品删除同步索引数据
    public void deleteBygoodsIds(List goodsIds);
}
