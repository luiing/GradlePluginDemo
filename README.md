# GradlePluginDemo
Gradle插件,代码注入demo

# MethodTimer函数执行时间统计(主线程)
        1.gradle setting
            dependencies {
                classpath 'com.uis.methodtimer:MethodTimer:0.0.1'
            }
        2.gradle setting,默认不统计android开头包
       
            apply plugin: 'com.uis.methodtimer'
            methodTimer{
                exclude 'com.facebook,com.google'
            }
# auto_permission权限自动回调onRequestPermissionsResult
#### 1.可以动态设置需要回调的方法，如果是基类方法需要重载才能生效
#### 2.以onRequestPermissionsResult为例子
