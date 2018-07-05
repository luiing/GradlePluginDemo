package com.uis.methodinject

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.bytecode.AccessFlag
import org.gradle.api.Project

class MethodInjectTransform extends Transform {
    Project project

    MethodInjectTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "AutoPermissionTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

    }

    static void permission(CtMethod method){
        try {
            CtClass ctClass = null
            CtField ad = ctClass.getField("")
            ad.setModifiers(AccessFlag.PUBLIC)
            CtMethod perMethod = ctClass.getDeclaredMethod("onRequestPermissionsResult")
            permission(perMethod)
        }catch (Exception ex){
            //println("160:"+ex.message)
        }
        if(method != null && method.methodInfo.codeAttribute != null && (0 == AccessFlag.ABSTRACT.and(method.modifiers).intValue())) {
            println(Integer.toHexString(method.modifiers))
            method.insertAfter("interprocess.uis.com.web_demo.PermissionProxy.resultPermission(\$0,\$\$);\n")
        }
    }
}