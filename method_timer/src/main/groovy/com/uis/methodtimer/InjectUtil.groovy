package com.uis.methodtimer

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
    String excludes = "android,"//包名
    boolean isMain;
    String TAG = "com.uis.MethodTimer"

    InjectUtil(Project project,MethodTimerExtension ext) {
        if(ext.exclude != null && !ext.exclude.empty){
            excludes += ext.exclude
        }
        isMain = ext.isMain
        pool.appendClassPath(project.android.bootClasspath[0].toString())
        println("-----exclude-----"+excludes)
    }

    void injectClass(String path){
        pool.appendClassPath(path)
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
        pool.clearImportedPackages()
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
        println("-----dirPath-----"+path)
        try{
            pool.getCtClass(TAG)
            return
        }catch (NotFoundException ex){
        }
        CtClass ctClass = pool.makeClass("com.uis.MethodTimerEntity")
        CtClass string = pool.get("java.lang.String")
        ctClass.addField(CtField.make("public long start;",ctClass))
        ctClass.addField(CtField.make("public String info;",ctClass))
        CtConstructor constructor = new CtConstructor([CtClass.longType, string, string] as CtClass[],ctClass)
        constructor.setBody("{start = \$1;info = \$2+\"-\"+\$3+\" cost time = %s ms\";}")
        ctClass.addConstructor(constructor)
        ctClass.writeFile(path)

        ctClass = pool.makeClass("com.uis.MethodTimer")
        ctClass.addField(CtField.make("private static boolean isMain = ${isMain};",ctClass))
        ctClass.addField(CtField.make("private static java.util.HashMap countMap = new java.util.HashMap();",ctClass))
        ctClass.addMethod(CtNewMethod.make("public static void startCount(String key,String calssName,String method){\n" +
                "       if(!isMain || android.os.Looper.myLooper() == android.os.Looper.getMainLooper()){\n" +
                "           countMap.put(key,new com.uis.MethodTimerEntity(System.currentTimeMillis(),calssName,method));\n" +
                "       }\n"+
                "    }",ctClass))
        ctClass.addMethod(CtNewMethod.make("public static void endCount(String key){\n" +
                "        com.uis.MethodTimerEntity entity = countMap.remove(key);\n" +
                "        if((!isMain || android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) && entity != null){\n" +
                "           Object[] cost = new Object[]{String.valueOf(java.lang.System.currentTimeMillis()-entity.start)};\n" +
                "           android.util.Log.e(\"MethodTimer\",String.format(entity.info,cost));\n" +
                "       }\n"+
                "    }",ctClass))
        ctClass.writeFile(path)
    }
}
