## Android+AR版跳一跳小游戏

### 效果图

步骤| 图片|步骤|图片
---|---|---|---
步骤一：识别平面 | <img src="https://github.com/91Shenlan/WeAr/raw/master/ar/src/main/assets/1.png" width=230 height=409 />|步骤二：开始游戏 |  <img src="https://github.com/91Shenlan/WeAr/raw/master/ar/src/main/assets/2.png" width=230 height=409 />
步骤三：初始化 |  <img src="https://github.com/91Shenlan/WeAr/raw/master/ar/src/main/assets/3.png" width=230 height=409 />|步骤四：游戏中 |  <img src="https://github.com/91Shenlan/WeAr/raw/master/ar/src/main/assets/5.png" width=230 height=409 />
步骤五：游戏结束 |  <img src="https://github.com/91Shenlan/WeAr/raw/master/ar/src/main/assets/5.png" width=230 height=409 />|渲染说明|<br>截图中的白色菱形为ARCore识别<br/>出来的平面；蓝色的点为特征点，<br/>用于计算位置变换


### Title
基于ARCore开发的AR版跳一跳



### Requirements
1. 首先是手机必须支持AR，具体可用的机型可以参考AR官网（建议使用Pixel 2，因为模型比较复杂，有一些手机即使支持AR也会出现加载失败问题）
[Support Devices](https://developers.google.com/ar/discover/supported-devices)；
1. 其次就是必须在google play上下载ARCore并安装，该软件为AR提供一些基础ARCore；
2. 最后确保系统版本在7及以上。




### Installation
本项目提供一个apk，想直接体验的可以直接点击[WeAr](https://github.com/91Shenlan/WeAr/blob/master/WeAr.apk)下载到本地安装即可；当然也可以用android studio将代码跑起来




### Usage
游戏具体的操作步骤跟微信小程序跳一跳一样，不过区别就是一开始需要等待手机检测到平面之后，才会提示我们可以“点击屏幕任意位置可以开始游戏”，这个时候我们只需要屏幕指定一个初始的渲染位置即可。游戏开始之后，就是长按屏幕让小机器人（游戏中我们控制跳动的模型）跳起来。

### Release Notes
**v1.0.0**

```
完成基本的跳一跳功能
```




### More
这个游戏为我自己一个人开发，基于能力和精力问题，肯定还存在很多问题。比如最基本的代码结构等，所以我在此也希望各位大佬们要是一些好的提议，小弟感激不尽。（不单单局限于项目，也可以是如何设计另外一个更有趣的AR项目）
