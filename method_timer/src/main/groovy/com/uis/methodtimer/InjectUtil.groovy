package com.uis.methodtimer

import javassist.ClassPath
import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtField
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.NotFoundException
import javassist.bytecode.AccessFlag
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class InjectUtil {
    ClassPool pool = new ClassPool(true)
    HashSet<ClassPath> paths = new HashSet()
    HashSet<String> jarChangedPaths = new HashSet()//未插入代码jar不需要重新打包
    String regexDirMatch
    String regexFileMatch = ".*\\.[A-Z]{0,1}R.{0,1}|.*\\.R.{0,1}\\\$.*|.*\\.BuildConfig"//R2,R,BR,R$xx,BuildConfig
    MethodTimerExtension ext
    static String TAG = "com.uis.MethodTimer"

    InjectUtil(Project project,MethodTimerExtension ext) {
        def regexBuilder = new StringBuilder("android.*|${TAG}.*")
        if(ext.exclude != null && !ext.exclude.empty){
            ext.exclude.split("\\,").each {
                regexBuilder.append("|").append(it).append(".*")
            }
        }
        regexDirMatch = regexBuilder.toString()
        this.ext = ext
        pool.appendClassPath(project.android.bootClasspath[0].toString())
    }

    void injectClass(String path){
        paths.add(pool.appendClassPath(path))
        File dir = new File(path)
        if(!dir.directory)return
        loopFile(path,dir)
    }

    void loopFile(final String path,File file){
        def fileName = file.absolutePath as String
        if(file.directory){
            String dirName = fileName.replaceAll("${path}${File.separator}","")
            if(!dirName.matches(regexDirMatch)){
                file.eachFile {
                    loopFile(path,it)
                }
            }
        }else{
            if(fileName.endsWith(".class")){//正则替换成com.tencent.sdk.User
                String className = fileName.replaceAll("${path}${File.separator}|.class","")
                                         .replaceAll("${File.separator}",".")
                if(!className.matches(regexFileMatch) && !className.matches(regexDirMatch)){
                    if(ext.enableLog) println("classname="+className)
                    insertTimerCode(path,className)
                }
                if(!jarChangedPaths.contains(path)) {
                    jarChangedPaths.add(path)
                }
            }
        }
    }

    void insertTimerCode(String path,String className){//format is: android.app.*
        try {
            CtClass ctClass = pool.getCtClass(className)
            if (ctClass.isFrozen()) {
                ctClass.defrost()
            }
            ctClass.getDeclaredMethods().each { method ->
                try {
                    if (method.methodInfo.codeAttribute != null && !method.name.startsWith("access\$")) {
                        insertMehodTimer(ctClass, method)
                    }
                } catch (Exception ex) {
                    println(TAG + "###insertMethodTimer###" + ex.message)
                }
            }
            ctClass.writeFile(path)
            ctClass.detach()
        }catch (Exception ex){
            println(TAG + "===insertTimerCode###" + ex.message)
        }
    }

    void unzipJarClass(String path){
        if(path.endsWith(".jar")) {
            def jarDir = path.replace(".jar", "")
            JarZipUtil.unzipJar(path, jarDir)
            pool.appendClassPath(jarDir)
        }
    }

    void zipJarClass(String path,File desFile){
        def jarDir = path.replace(".jar", "")
        def des = path
        if(ext.enableJar && jarChangedPaths.contains(path)) {
            des = jarDir + "_" + System.currentTimeMillis() + ".jar"
            JarZipUtil.zipJar(jarDir, des)
        }
        FileUtils.deleteDirectory(new File(jarDir))
        def srcFile = new File(des)
        FileUtils.copyFile(srcFile, desFile)
        if(!des.equals(path)) {
            srcFile.delete()
        }
    }

    void injectJarClass(String path){
        if(path.endsWith(".jar")) {
            String jarDir = path.replace(".jar", "")
            injectClass(jarDir)
        }
    }

    void release(){
        try {
            pool.clearImportedPackages()
            paths.each {
                pool.removeClassPath(it)
            }
        }catch (Exception ex){}
        paths.clear()
        jarChangedPaths.clear()
        pool = null
    }

    void insertMehodTimer(CtClass clas, CtMethod method) throws Exception {
        if(0 == AccessFlag.ABSTRACT.and(method.modifiers).intValue()) {
            String className = clas.name
            String methodName = method.name
            String key = DigestUtils.md5Hex(className + methodName + method.hashCode())
            String methodInfo = methodName + "(" + method.methodInfo.getLineNumber(0) + ")"
            method.insertBefore("com.uis.MethodTimer.startCount(\"${key}\",\"${className}\",\"${methodInfo}\");\n")
            method.insertAfter("com.uis.MethodTimer.endCount(\"${key}\");\n")
        }
    }

    void initMethodTimer(String path){//not support fanxing
        try{//pool exists class,then wirte to path
            [TAG,"com.uis.MethodTimerEntity"].each {
                CtClass ctClass = pool.getCtClass(it)
                if (ctClass != null && ctClass.isFrozen()) {//pool exist the file
                    ctClass.defrost()
                }
                ctClass.writeFile(path)
            }
            return
        }catch (NotFoundException ex){
        }
        CtClass ctClass = pool.makeClass("com.uis.MethodTimerEntity")
        CtClass string = pool.get("java.lang.String")
        ctClass.addField(CtField.make("public long start;",ctClass))
        ctClass.addField(CtField.make("public String info;",ctClass))
        CtConstructor constructor = new CtConstructor([CtClass.longType, string, string] as CtClass[],ctClass)
        constructor.setBody("{start = \$1;\ninfo = \$2+\"-\"+\$3+\" cost time = %s ms\";}")
        ctClass.addConstructor(constructor)
        ctClass.writeFile(path)
        ctClass = pool.makeClass(TAG)
        ctClass.addField(CtField.make("private static boolean isMain = ${ext.isMain};",ctClass))
        ctClass.addField(CtField.make("private static int timeout = ${ext.timeout};",ctClass))
        ctClass.addField(CtField.make("private static java.util.HashMap countMap = new java.util.HashMap();",ctClass))
        ctClass.addMethod(CtNewMethod.make("public static void startCount(String key,String calssName,String method){\n" +
                "       if(!isMain || android.os.Looper.myLooper() == android.os.Looper.getMainLooper()){\n" +
                "           countMap.put(key,new com.uis.MethodTimerEntity(System.currentTimeMillis(),calssName,method));\n" +
                "       }\n"+
                "    }",ctClass))
        //fixed java.lang.VerifyError: Verifier rejected class
        //java是强类型，同时javassist不支持范型
        ctClass.addMethod(CtNewMethod.make("public static void endCount(String key){\n" +
                "        Object entity = countMap.remove(key);\n" +
                "        if((!isMain || android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) && entity != null && entity instanceof com.uis.MethodTimerEntity){\n" +
                "           com.uis.MethodTimerEntity v = (com.uis.MethodTimerEntity)entity;\n" +
                "           long costtime = System.currentTimeMillis() - v.start;\n"+
                "           if(costtime > timeout){\n" +
                "               Object[] v1 = new Object[]{String.valueOf(costtime)};\n"+
                "               android.util.Log.e(\"MethodTimer\",String.format(v.info,v1));\n" +
                "           }\n"+
                "       }\n"+
                "    }",ctClass))
        ctClass.writeFile(path)
    }
}
