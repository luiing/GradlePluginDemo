package com.uis.methodinject;

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry

/**
 * @author uis
 * @des jar包分隔符为/
 */
class JarZipUtil {

    static boolean unzipJar(String jarPath, String destDirPath){
        unzipJar(jarPath,destDirPath,null)
    }
    /**
     * 将该jar包解压到指定目录
     * @param jarPath jar包路径
     * @param destDirPath jar包解压后路径
     * @return isExclude
     */
    static boolean unzipJar(String jarPath, String destDirPath,String exclude) {
        if (jarPath.endsWith('.jar')) {
            JarFile jarFile = new JarFile(jarPath)
            if(exclude!=null && !exclude.empty){
                def entries = jarFile.entries()
                if(entries.hasMoreElements()){
                    String[] excludes = exclude.split("\\,")
                    int size = excludes.length
                    JarEntry excludeJar = entries.nextElement()
                    String jarName = excludeJar.name.replace("/",".")
                    for(int i=0;i<size;i++){
                        if(jarName.startsWith(excludes[i])){
                            return true
                        }
                    }
                }
            }
            jarFile.entries().each {jarEntry->
                if(!jarEntry.directory) {
                    def outFile = new File(destDirPath, jarEntry.name)
                    outFile.getParentFile().mkdirs()
                    def inputStream = jarFile.getInputStream(jarEntry)
                    def outputStream = new FileOutputStream(outFile)
                    outputStream << inputStream
                    outputStream.close()
                    inputStream.close()
                }
            }
            jarFile.close()
        }
        return false
    }

    /**
     * 重新打包jar
     * @param packagePath 目录文件
     * @param destPath jar包路径
     */
    static void zipJar(String packagePath, String destPath) {
        File dir = new File(packagePath)
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(destPath))
        outputStream.setLevel(Deflater.BEST_SPEED)
        dir.eachFileRecurse { file ->
            if(!file.directory) {
                def entryName = file.getAbsolutePath().substring(packagePath.length() + 1).replace("\\","/")
                outputStream.putNextEntry(new ZipEntry(entryName))
                InputStream inputStream = new FileInputStream(file)
                outputStream << inputStream
                inputStream.close()
            }
        }
        outputStream.close()
    }
}
