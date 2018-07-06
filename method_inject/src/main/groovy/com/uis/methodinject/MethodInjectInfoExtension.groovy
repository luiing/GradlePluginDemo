package com.uis.methodinject

class MethodInjectInfoExtension {
    String name//nameDomain
    String include
    String exclude

    String interfaces//注入实现此接口方法

    String descriptor
    String fields
    String constructor//body
    String methodName
    String methodBefor
    String methodAfter


    //代码支持注入到:构造函数,函数,类子段

    MethodInjectInfoExtension(String name) {
        this.name = name
    }

    def name(String name){
        this.name = name
    }

}