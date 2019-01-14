app.controller('itemController', ['$scope', function ($scope) {

	//增减数字的方法
     $scope.addNum=function(x) {
         $scope.num += x ;

         if($scope.num<1){
         	$scope.num = 1 ;
         }

     }

     $scope.specificationItems={};//记录用户选择的规格

     //选择规格
     $scope.selectSpecification=function(key,value){
$scope.specificationItems[key] =value ;
searchSku() ; //读取sku
     }

     //判断是否被选中加样式
     $scope.isSelect = function (key,value) {
     	if($scope.specificationItems[key] == value){
     		return true ;
     	}else{
     		return false ; 
     	}
     }

     //加载默认的sku
$scope.loadSku = function(){
	$scope.sku =skuList[0];
	//使用深复制
	$scope.specificationItems=JSON.parse(JSON.stringify($scope.sku.spec));
}

   //js判断2个对象是否相等

   matchObject = function(map1,map2){
   	for(var k1 in map1){ //拿到对象的key {"网络":"移动3G","机身内存":"16G"}  key 就是 网络 ,机身内存
   		if(map1[k1] != map2[k1] ){
   			return false;
   		}
   	}

   	for(var k2 in map2){
   		if(map2[k2] != map1[k2] ){
   			return false;
   		}
   	}
   	return true ;
   }

   //查询sku 
   searchSku =function () {
       for( var i=0 ;i<skuList.length;i++){
       	if(matchObject(skuList[i].spec,$scope.specificationItems)){
       		$scope.sku = skuList[i] ;
       		return ;
       	}
       }
       //没有匹配上的话 给默认值
   	$scope.sku={id:0,title:'--------',price:0};//如果没有匹配的	
   }

$scope.addToCart=function(){
		alert('skuid:'+$scope.sku.id);				
	}


}])