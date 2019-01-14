// 定义模块:
var app = angular.module("pinyougou",[]);
//由于angular的安全校验，防止html注入，js注入，
//定义过滤器  $sce  angular的 安全信任模块 trustHtml 过滤器的名字
app.filter('trustHtml',['$sce',function ($sce) {
    return function (data) { //传入参数是被过滤的内容
        return $sce.trustAsHtml(data); //返回的是过滤后的内容（信任html的转换）
    }
}])