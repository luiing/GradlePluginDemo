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
import org.gradle.api.Project;

class InjectUtil {
    ClassPool pool = ClassPool.getDefault()
    List<ClassPath> paths = new ArrayList<>()
    String excludes = "android,"//包名
    MethodTimerExtension ext
    String TAG = "com.uis.MethodTimer"

    InjectUtil(Project project,MethodTimerExtension ext) {
        if(ext.exclude != null && !ext.exclude.empty){
            excludes += ext.exclude
        }
        this.ext = ext
        pool.appendClassPath(project.android.bootClasspath[0].toString())
        println("-----exclude-----"+excludes)
    }

    void injectClass(String path){
        paths.add(pool.appendClassPath(path))
        File dir = new File(path)
        String[] excludeClass = excludes.split("\\,")
        int excludeSize = excludeClass.length
        try {
            if (dir.isDirectory()) {
                dir.eachFileRecurse { File file ->
                    if (file.file && file.path.endsWith(".class") && !file.path.contains("R\$") && !file.path.contains("R.class") && !file.path.contains("BuildConfig.class")) {
                        String className = file.path.replace(dir.path+File.separator,"")
                        className = className.replace(".class","")
                        className = className.replace(File.separator,".")
                        boolean  isExclude = false
                        for(int i=0;i<excludeSize;i++){
                            if(className.startsWith(TAG) || className.startsWith(excludeClass[i])) {
                                isExclude = true
                                break
                            }
                        }
                        if(!isExclude) {
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
                                    println(TAG+"###"+ex.message)
                                }
                            }
                            ctClass.writeFile(path)
                            ctClass.detach()
                        }
                    }
                }
            }
        }catch (Throwable ex){
            ex.printStackTrace()
        }
    }

    String injectJarClass(String path){
        if(path.endsWith(".jar")) {
            File jarFile = new File(path)
            String jarDir = jarFile.parent + File.separator + jarFile.name.replace(".jar", "")
            if (!JarZipUtil.unzipJar(path, jarDir,excludes)) {
                injectClass(jarDir)
                def build = new StringBuilder(path.replace(".jar", ""))
                build.append("_").append(System.currentTimeMillis()).append(".jar")
                path = build.toString();
                JarZipUtil.zipJar(jarDir, build.toString())
                FileUtils.deleteDirectory(new File(jarDir))
            }
        }
        return path
    }

    void release(){
        try {
            pool.clearImportedPackages()
            paths.each {
                pool.removeClassPath(it)
            }
        }catch (Exception ex){}
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
