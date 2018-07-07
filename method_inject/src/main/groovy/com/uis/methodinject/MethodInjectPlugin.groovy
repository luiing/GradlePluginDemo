package com.uis.methodinject

import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodInjectPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def inject = project.container(MethodInjectInfoExtension)
        MethodInjectExtension method = new MethodInjectExtension(inject)
        project.extensions.add("methodInject", method)

        if(project.hasProperty("android")){
            project.android.registerTransform(new MethodInjectTransform(project))
        }
    }
}