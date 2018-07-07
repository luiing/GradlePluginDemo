package com.uis.methodinject

import org.gradle.api.NamedDomainObjectContainer

class MethodInjectExtension {
    NamedDomainObjectContainer<MethodInjectInfoExtension> inject


    MethodInjectExtension(NamedDomainObjectContainer<MethodInjectInfoExtension> inject) {
        this.inject = inject
    }

    def inject(Closure closure){
        this.inject.configure(closure)
    }
}