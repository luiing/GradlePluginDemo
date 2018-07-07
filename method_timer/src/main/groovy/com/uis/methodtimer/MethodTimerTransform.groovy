package com.uis.methodtimer

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class MethodTimerTransform extends Transform {
    Project project

    MethodTimerTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "MethodTimerTransform"
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
    void transform(TransformInvocation invocation) throws TransformException, InterruptedException, IOException {
        long startTime = System.currentTimeMillis()
        def ext = project.methodTimer as MethodTimerExtension
        InjectUtil inject = new InjectUtil(project,ext)
        DirectoryInput dir
        HashSet<JarInput> jar = new HashSet()
        invocation.inputs.each {input->
            input.directoryInputs.each {
                if(dir == null && it.file != null)
                    dir = it
            }
            input.jarInputs.each {
                jar.add(it)
            }
        }
        inject.initMethodTimer(dir.file.absolutePath)

        //jar unzip all files
        jar.each {
            def s = System.currentTimeMillis()
            inject.unzipJarClass(it.file.absolutePath)
            println("---jarunzip---"+it.file.absolutePath+"\n### cost="+(System.currentTimeMillis()-s))
        }
        //jar inject
        if(ext.enableJar) {
            jar.each {
                def s = System.currentTimeMillis()
                inject.injectJarClass(it.file.absolutePath)
                println("---jarInputs---" + it.file.path + ",### cost=" + (System.currentTimeMillis() - s))
            }
        }
        //directory inject,保证所有jar加入到ClassPool在开始注入
        def st = System.currentTimeMillis()
        inject.injectClass(dir.file.absolutePath)
        def dest = invocation.outputProvider.getContentLocation(dir.name,dir.contentTypes,dir.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(dir.file,dest)
        println("---dirInputs---"+dir.file.absolutePath+",### cost="+(System.currentTimeMillis()-st))

        //jar zip all files
        jar.each {
            def s = System.currentTimeMillis()
            def newName = it.name.replace(".jar","")+DigestUtils.md5Hex(it.name)
            def desFile = invocation.outputProvider.getContentLocation(newName,it.contentTypes, it.scopes, Format.JAR)
            inject.zipJarClass(it.file.absolutePath,desFile)
            println("---jarzip---"+it.file.absolutePath+"\n${desFile.path}"+"\n### cost="+(System.currentTimeMillis()-s))
        }
        inject.release()
        println("------${getName()}costtime-----"+(System.currentTimeMillis()-startTime)+"ms")
    }
}