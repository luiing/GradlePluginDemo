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
        InjectUtil inject = new InjectUtil(project,project.methodTimer as MethodTimerExtension)
        DirectoryInput dir
        List<JarInput> jar = new ArrayList<>()
        invocation.inputs.each {input->
            input.directoryInputs.each {
                println("dir="+it.file.absolutePath)
                if(dir == null && it.file != null)
                    dir = it
            }
            input.jarInputs.each {
                println("jar="+it.file.absolutePath)
                jar.add(it)
            }
        }
        while(dir == null){
        }
        inject.initMethodTimer(dir.file.absolutePath)
        //jar inject
        jar.each {
            def s = System.currentTimeMillis()
            def newJarFile = new File(inject.injectJarClass(it.file.absolutePath))
            def newName = it.name.replace(".jar","")+DigestUtils.md5Hex(it.file.absolutePath)
            def des = invocation.outputProvider.getContentLocation(newName,it.contentTypes, it.scopes, Format.JAR)
            FileUtils.copyFile(it.file, des)
            if(!it.file.path.equalsIgnoreCase(newJarFile.path)) {
                newJarFile.delete()
            }
            println("---jarInputs---"+it.file.path+",###"+des.path+"\njarInputs cost="+(System.currentTimeMillis()-s))
        }
        //directory inject
        def st = System.currentTimeMillis()
        inject.injectClass(dir.file.absolutePath)
        def dest = invocation.outputProvider.getContentLocation(dir.name,dir.contentTypes,dir.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(dir.file,dest)
        println("---dirInputs---"+dir.file.absolutePath+",###"+dest.path+"\ndirInputs cost="+(System.currentTimeMillis()-st))

        inject.release()
        project.logger.quiet("------${getName()}costtime-----"+(System.currentTimeMillis()-startTime)+"ms")
    }
}