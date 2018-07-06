# GradlePluginDemo
Gradle插件：代码注入

## 配置要求：gradle:3.0.0以上，buildTool:26.0.2以上

# MethodTimer函数执行耗时统计
        1.gradle setting
            dependencies {
                classpath 'com.uis:method_timer:0.1.1'
            }
        2.gradle setting,默认不统计android开头包
       
            apply plugin: 'com.uis.methodtimer'
            methodTimer{
                exclude 'com.facebook,com.google'//默认排除android.*
                isMain true//默认true表示只统计主线程
                timeout 10//默认100ms,只打印超过此时间函数耗时
                enableJar true//默认开启jar，aar包注入
                enalbeLog true//默认开启日志
            }
# Method方法注入,Field变量初始化
       1. 可以动态设置需要回调的方法，如果是基类方法需要重载才能生效
    
       2. 以onRequestPermissionsResult为例子,auto_permission权限自动回调onRequestPermissionsResult
