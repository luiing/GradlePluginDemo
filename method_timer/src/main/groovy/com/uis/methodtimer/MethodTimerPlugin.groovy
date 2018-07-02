package com.uis.methodtimer

import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodTimerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create("methodTimer",MethodTimerExtension)
        if(project.hasProperty("android")){
            project.android.registerTransform(new MethodTimerTransform(project))
        }
    }
}