app.controller('searchController',function($scope,searchService,$location){

	//定义查询的map
	$scope.searchMap = {'keyword':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':40,'sortField':'','sort':''};
	//搜索
	$scope.search=function(){
		$scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo) ;
		searchService.search($scope.searchMap).success(
			function(response){
				debugger;
				$scope.resultMap=response;
				//查询之后重新设置页码
				//$scope.searchMap.pageNo =1 ;
				buildPageLabel();
			}
		);		
	}


	//添加搜索项
	$scope.addSearchItem=function (key,value) {
		if(key =='category' || key == 'brand' || key =='price'){ //搜索条件为 分类 或者是品牌的话 或者价格区间的话
			$scope.searchMap[key] = value ;
		}else{ //规格选项
			$scope.searchMap.spec[key] =value ;
		}
		$scope.search();//查询
	}
	//移除搜索信息
	$scope.removeSearchItem=function (key) {
		if(key =='category' || key == 'brand' || key =='price'){
			$scope.searchMap[key] ='';
		}else{
			delete $scope.searchMap.spec[key] ; //移除此属性
		}
		$scope.search();//查询
	}

	//构建分页标签(totalPages为总页数)
	buildPageLabel = function () {
		$scope.pageLableList =[];
		var firstPage = 1 ;
		var lastPage = $scope.resultMap.totalPages ; //截至页码
		 $scope.firstDot = true ; //前面有点
		$scope.lastDot = true ;// 后面有点
		if($scope.resultMap.totalPages > 5 ){
			if($scope.searchMap.pageNo <=3){  //当前页码<=3
				lastPage = 5 ;
				$scope.firstDot= false;
			}else if ($scope.searchMap.pageNo >= $scope.resultMap.totalPages -2){ //当前页>= 总页数-2
				firstPage = $scope.resultMap.totalPages -4 ;
				$scope.lastDot = false ;
			}else{
				firstPage = $scope.searchMap.pageNo -2 ;
				lastPage = $scope.searchMap.pageNo +2 ;
			}

		}else{
			$scope.firstDot = false ;
			$scope.lastDot = false ;
		}

		for(var i=firstPage;i<= lastPage; i++){
			$scope.pageLableList.push(i);
		}

	}


	//分页查询下
	$scope.queryByPage=function (pageNo) {
		if(pageNo<1 || pageNo >$scope.searchMap.totalPages){
			return ;
		}
		$scope.searchMap.pageNo = pageNo;
		$scope.search();
	}

	//判断是否是首页
	$scope.isTopPage= function () {
		if($scope.searchMap.pageNo == 1){
			return true ;
		}else{
			return false;
		}
	}

	//判断是否是尾页
	$scope.isEndPage= function () {
		if($scope.searchMap.pageNo == $scope.resultMap.totalPages){
			return true ;
		}else{
			return false;
		}
	}
	
	
	//排序搜索

	$scope.sortSearch = function (sort,sortField) {
		debugger
		$scope.searchMap.sort = sort ;
		$scope.searchMap.sortField = sortField ;
		$scope.search();

	}

	//判断搜索的关键字中使用有 品牌的子 字符串 ，有隐藏掉品牌
	$scope.keywordIsBrand =function () {
		var brandList = $scope.resultMap.brandList ;

		for(var i =0 ;i<$scope.resultMap.brandList.length;i++){
			var brand =  brandList[i];
			if($scope.searchMap.keyword.indexOf(brand.text)>-1){
				return true ;

			}
		}
		return false ;
	}

	//加载关键字
	$scope.loadkeyWords = function () {
		$scope.searchMap.keyword =	$location.search()['keyword'];
		$scope.search();
	}




});