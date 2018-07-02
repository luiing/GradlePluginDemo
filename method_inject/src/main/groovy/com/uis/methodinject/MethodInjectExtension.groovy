package com.uis.methodinject

class MethodInjectExtension {
    String methodName = ""//插入函数名
    String methodParam = "\$\$"//函数参数$0表示this,$1第一个参数,$$表示全部
    String exclude = ""//排除包名或类
}