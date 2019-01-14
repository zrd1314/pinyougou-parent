package com.pinyougou.sellergoods.service.impl;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbSpecificationOptionMapper;
import com.pinyougou.mapper.TbTypeTemplateMapper;
import com.pinyougou.pojo.TbSpecificationOption;
import com.pinyougou.pojo.TbSpecificationOptionExample;
import com.pinyougou.pojo.TbTypeTemplate;
import com.pinyougou.pojo.TbTypeTemplateExample;
import com.pinyougou.pojo.TbTypeTemplateExample.Criteria;
import com.pinyougou.sellergoods.service.TypeTemplateService;

import entity.PageResult;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
@Transactional
public class TypeTemplateServiceImpl implements TypeTemplateService {

	@Autowired
	private TbTypeTemplateMapper typeTemplateMapper;

	@Autowired
	private RedisTemplate redisTemplate;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbTypeTemplate> findAll() {
		return typeTemplateMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbTypeTemplate> page=   (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbTypeTemplate typeTemplate) {
		typeTemplateMapper.insert(typeTemplate);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbTypeTemplate typeTemplate){
		typeTemplateMapper.updateByPrimaryKey(typeTemplate);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbTypeTemplate findOne(Long id){
		return typeTemplateMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			typeTemplateMapper.deleteByPrimaryKey(id);
		}		
	}

	/**
	 * 增删 改 都会调用 所有 使用这个方法来 缓存 规格（模板id（key）），规格选项集合（value） 和 品牌信息（模板id（key）,品牌集合为value）
	 * @param typeTemplate
	 * @param pageNum 当前页 码
	 * @param pageSize 每页记录数
	 * @return
	 */
		@Override
	public PageResult findPage(TbTypeTemplate typeTemplate, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbTypeTemplateExample example=new TbTypeTemplateExample();
		Criteria criteria = example.createCriteria();
		
		if(typeTemplate!=null){			
						if(typeTemplate.getName()!=null && typeTemplate.getName().length()>0){
				criteria.andNameLike("%"+typeTemplate.getName()+"%");
			}
			if(typeTemplate.getSpecIds()!=null && typeTemplate.getSpecIds().length()>0){
				criteria.andSpecIdsLike("%"+typeTemplate.getSpecIds()+"%");
			}
			if(typeTemplate.getBrandIds()!=null && typeTemplate.getBrandIds().length()>0){
				criteria.andBrandIdsLike("%"+typeTemplate.getBrandIds()+"%");
			}
			if(typeTemplate.getCustomAttributeItems()!=null && typeTemplate.getCustomAttributeItems().length()>0){
				criteria.andCustomAttributeItemsLike("%"+typeTemplate.getCustomAttributeItems()+"%");
			}
	
		}
		
		Page<TbTypeTemplate> page= (Page<TbTypeTemplate>)typeTemplateMapper.selectByExample(example);
		saveToRedis();
		return new PageResult(page.getTotal(), page.getResult());
	}

	private void saveToRedis() {
			//缓存所有规格信息 和 品牌信息
		List<TbTypeTemplate> typeTemplateList = findAll();
		typeTemplateList.forEach(typeTemplate ->{
      //brand_ids [{"id":3,"text":"三星"},{"id":5,"text":"OPPO"}]
			//缓存 品牌数据
			redisTemplate.boundHashOps("brandList").put(typeTemplate.getId(),JSON.parseArray(typeTemplate.getBrandIds(),Map.class));
			//得到规格列表
			List<Map> specList = findSpecList(typeTemplate.getId());
			//缓存规格数据
			redisTemplate.boundHashOps("specList").put(typeTemplate.getId(),specList);
		});
		System.out.println("缓存品牌列表");
		System.out.println("缓存规格列表");
	}

	@Autowired
	private TbSpecificationOptionMapper specificationOptionMapper;
		
	@Override
	public List<Map> findSpecList(Long id) {
		//查询模板
		TbTypeTemplate typeTemplate = typeTemplateMapper.selectByPrimaryKey(id);
		
		List<Map> list = JSON.parseArray(typeTemplate.getSpecIds(), Map.class)  ;
		
		for(Map map:list){
			
			//查询规格选项列表
			TbSpecificationOptionExample example=new TbSpecificationOptionExample();
			com.pinyougou.pojo.TbSpecificationOptionExample.Criteria criteria = example.createCriteria();
			criteria.andSpecIdEqualTo( new Long( (Integer)map.get("id") ) );
			List<TbSpecificationOption> options = specificationOptionMapper.selectByExample(example);
			
			map.put("options", options);
		}
		
		return list;
	}


}
