app.controller('contentController',function($scope,contentService){
	
	$scope.contentList=[];//广告列表
	
	$scope.findByCategoryId=function(categoryId){
		contentService.findByCategoryId(categoryId).success(
			function(response){
				$scope.contentList[categoryId]=response;
			}
		);		
	}

	//首页根据 搜索关键字 跳转到 商品搜索页面
	$scope.search = function () {
		location.href = "http://localhost:9104/search.html#?keyword="+$scope.keyword ;
	}
	
});