package com.uis.methodinject

import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodInjectPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if(project.hasProperty("android")){
            project.android.registerTransform(new MethodInjectTransform(project))
        }
        project.task("methodInject")<<{

        }
    }
}