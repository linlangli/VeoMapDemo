**Github地址: https://github.com/linlangli/VeoMapDemo**

# 功能

**1. 地图显示**

**2. 当前坐标显示**

**3. 自定义Icon显示地名**

**4. 地点选择**

**5. 开始导航显示路径**

**6. 实时坐标监控更新**

**7. 抵达目的地结束导航统计数据**

# API

获取路径

```
maps/api/directions/json
```

解析地名

```
maps/api/geocode/json
```

# 架构

项目整体采用**MVVM**架构，使用**Compose**作为UI构建组件，使用**ViewModel**和**Flow**实现生命周期和数据状态管理

![项目包结构.png](%E9%A1%B9%E7%9B%AE%E5%8C%85%E7%BB%93%E6%9E%84.png)

# 代码简介

**MapViewModel**作为**ViewModel**

```kotlin
// 解析后的路径信息
private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())  
val routePoints = _routePoints.asStateFlow()  

// 实际时长
private val _travelDuration = MutableStateFlow<String?>(null)  
val travelDuration = _travelDuration.asStateFlow()  

// 实时位置
private val _currentLocation = MutableStateFlow<LocationInfo?>(null)  
val currentLocation = _currentLocation.asStateFlow()  

// 导航信息
private val _directionLeg = MutableStateFlow<Leg?>(null)  
val directionLeg = _directionLeg.asStateFlow()  

// 起始点
private val _startLocation = MutableStateFlow<LocationInfo?>(null)  
val startLocation = _startLocation.asStateFlow()  

// 终点
private val _endLocation = MutableStateFlow<LocationInfo?>(null)  
val endLocation = _endLocation.asStateFlow()

// 导航状态，包括（空闲，开始，抵达）
private val _navigationState = MutableStateFlow(NavigationState.IDLE)  
val navigationState = _navigationState.asStateFlow()
```

**GoogleMapScreen**做为Map地图组件，也是本APP的核心UI组件

**service**包下的两个接口，分别代表两个不同的API

**MapUtil**中包括地址解析，创建自定义浮层ICON以及API请求服务的初始化对象

**model**包下的model对象作为两个API请求的数据对象

# 注意事项

密钥已经存放到secrets.properties中，在build.gradle中完成注入，这样可以避免直接暴露在代码中，但由于已经作为public项目上传到github库，所以已不安全。

是否已经抵达目的地依赖于实时位置监听，监听时间间隔可配置。

APP本地进行了简单的白盒测试，缺乏充分系统测试，可能存在BUG