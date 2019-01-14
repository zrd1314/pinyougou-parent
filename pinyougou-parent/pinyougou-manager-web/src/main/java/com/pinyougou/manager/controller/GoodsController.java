package com.pinyougou.manager.controller;
import java.util.Arrays;
import java.util.List;

import com.pinyougou.page.service.ItemPageService;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojogroup.Goods;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;
import entity.Result;
/**
 * controller
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

	@Reference
	private GoodsService goodsService;
	@Reference(timeout = 100000)
	private ItemSearchService itemSearchService;
	@Reference(timeout = 100000)
	private ItemPageService itemPageService ;
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findAll")
	public List<TbGoods> findAll(){			
		return goodsService.findAll();
	}
	
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findPage")
	public PageResult  findPage(int page,int rows){			
		return goodsService.findPage(page, rows);
	}
	

	
	/**
	 * 修改
	 * @param goods
	 * @return
	 */
	@RequestMapping("/update")
	public Result update(@RequestBody Goods goods){
		try {
			goodsService.update(goods);
			return new Result(true, "修改成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "修改失败");
		}
	}	
	
	/**
	 * 获取实体
	 * @param id
	 * @return
	 */
	@RequestMapping("/findOne")
	public Goods findOne(Long id){
		return goodsService.findOne(id);		
	}
	
	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@RequestMapping("/delete")
	public Result delete(Long [] ids){
		try {
			goodsService.delete(ids);
			//删除索引库数据
			itemSearchService.deleteBygoodsIds(Arrays.asList(ids));
			return new Result(true, "删除成功"); 
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "删除失败");
		}
	}
	
		/**
	 * 查询+分页
	 * @param
	 * @param page
	 * @param rows
	 * @return
	 */
	@RequestMapping("/search")
	public PageResult search(@RequestBody TbGoods goods, int page, int rows  ){
		return goodsService.findPage(goods, page, rows);		
	}
	
	@RequestMapping("/updateStatus")
	public Result updateStatus(Long[] ids, String status){
		try {
			goodsService.updateStatus(ids, status);
			if("1".equals(status)){ //审核已通过
				List<TbItem> itemList = goodsService.findItemListByGoodsIdandStatus(ids,status);
				itemSearchService.importData(itemList);

				//生产商品详情页
				for(Long goodsId:ids){
					itemPageService.genItemHtml(goodsId);
				}
			}
			return new Result(true, "成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "失败");
		}		
	}

	@RequestMapping("/genHtml")
	public void genHtml(Long goodsId){
		itemPageService.genItemHtml(goodsId);
	}
}
