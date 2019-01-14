# jpa 分页的简单版本
error_file.java 是平时用于分页的主要方法
目前年底个人精力有限,暂时没有去除依赖,会慢慢完善

随便放个demo吧
https://github.com/wenhao/jpa-spec 这也是一个分页插件
我方代码优点: 利用Application/json 请求方式直接从前端获取到一个参数的hashMap,直接用hashmap来当做查询条件.
同时为了兼容或查询,特地新加了orCond HashMap 
代码是随便写的,readme就是加上来填充一下.年后完善吧.

```
    @Override
    public RequestResult getItemList(HashMap<String, Object> cond, int pageIndex, int pageSize) {
        Pageable pageable = PageRequest.of(pageIndex - 1, pageSize);
        Page<ProjectItem> page = projectItemDao.findAll(new SimpleSpecification<ProjectItem>(cond, null), pageable);
        RequestResult result = RequestResult.new0();
        result.setMapData("pageInfo", PageInfo.Page2PageInfo(page));
        result.addMapData("list", page.getContent());
        return result;
    }
```