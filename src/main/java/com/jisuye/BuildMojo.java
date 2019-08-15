package com.jisuye;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
    private String finalName;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("square maven plugin exe...");
        getLog().info("outputDir:"+outputDirectory.getPath());
        Artifact source = project.getArtifact();
        try {
            // 取package打出的jar包
            File file = source.getFile();
            getLog().info("file name name ======"+file.getName());
            JarFile jarFile = new JarFile(file);
            // 创建临时jar包
            File tmpFile = new File(outputDirectory.getPath()+"/temp.jar");
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
            List<String> fileList = new ArrayList<String>();
            Enumeration<?> jarEntries = jarFile.entries();
            // 循环复制文件
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = (JarEntry) jarEntries.nextElement();
//                getLog().info("filename:"+entry.getName());
                fileList.add(entry.getName());
                InputStream entryInputStream = jarFile.getInputStream(entry);
                // 如果是清单文件，则追加主类
                if(entry.getName().endsWith("MANIFEST.MF")){
                    BufferedReader br = new BufferedReader(new InputStreamReader(entryInputStream));
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        if(!line.equals("")) {
                            sb.append(line).append("\n");
                        }
                    }
                    String mainClass = findMainClass();
                    getLog().info("mainclass:"+mainClass);
                    if(mainClass.equals("")){
                        getLog().error("No found Main-Class!!  Plase use the @SquareApplication settings");
                    } else {
                        sb.append("Main-Class: ").append(mainClass).append("\n");
                    }
                    jarOutputStream.putNextEntry(new JarEntry(entry.getName()));
                    jarOutputStream.write(sb.toString().getBytes());
                } else {
                    jarOutputStream.putNextEntry(entry);
                    byte[] buffer = new byte[1024];
                    int bytesRead = 0;
                    while ((bytesRead = entryInputStream.read(buffer)) != -1) {
                        jarOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
            addDependenceJar(project.getArtifacts(), jarOutputStream, fileList);
            jarOutputStream.close();
            updateJar(file, tmpFile);
        } catch (IOException e) {
            getLog().error("load jar file error!", e);
        }
    }

    /**
     * 更新jar文件，将原文件保存为**.jar.old
     * @param oldFile 原文件
     * @param newFile 添加清单及依赖后的完整包
     * @throws IOException
     */
    private void updateJar(File oldFile, File newFile) throws IOException {
        String fileName = oldFile.getName();
        File backup = new File(outputDirectory.getPath()+"/"+fileName+".old");
        FileOutputStream fos = new FileOutputStream(backup);
        FileInputStream fis = new FileInputStream(oldFile);
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fis.close();
        fos.close();
        oldFile.delete();
        if(finalName != null && !finalName.equals("")){
            fileName = finalName+".jar";
        }
        File newJar = new File(outputDirectory.getPath()+"/"+fileName);
        fos = new FileOutputStream(newJar);
        fis = new FileInputStream(newFile);
        buffer = new byte[1024];
        while ((bytesRead = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fis.close();
        fos.close();
        newFile.delete();
    }

    /**
     * 添加依赖的jar
     * @param artifacts 依赖Jar列表
     * @param jarOutputStream 当前repackage包输出流
     * @param fileList 已有文件列表（防止重复）
     * @throws IOException
     */
    private void addDependenceJar(Set<Artifact> artifacts, JarOutputStream jarOutputStream, List<String> fileList) throws IOException {
        // save dependence jar
        for (Artifact artifact : artifacts) {
            JarFile jarFile = new JarFile(artifact.getFile());
            Enumeration<?> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = (JarEntry) jarEntries.nextElement();
                if(fileList.contains(entry.getName())){
                    // already added skipping
//                    getLog().info(entry.getName()+" already added, skipping");
                    continue;
                }
                fileList.add(entry.getName());
                InputStream entryInputStream = jarFile.getInputStream(entry);
                jarOutputStream.putNextEntry(entry);
                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                while ((bytesRead = entryInputStream.read(buffer)) != -1) {
                    jarOutputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    /**
     * 遍历目录查找主类
     * @return
     */
    private String findMainClass(){
        Build build = project.getBuild();
        getLog().info("build source dir:"+build.getSourceDirectory());
        File f = new File(build.getSourceDirectory());
        StringBuilder mainClass = new StringBuilder();
        getMainClass(f, mainClass);
        return mainClass.toString();
    }

    /**
     * 查找还@SquareApplication注解的类
     * @param file 文件
     * @param mainClass 要返回的主类
     */
    private void getMainClass(File file, StringBuilder mainClass){
        if(!mainClass.toString().equals("")){
            return;
        }
        File[] fs = file.listFiles();
        for (File f : fs) {
            if(f.isDirectory()){
                // 递归目录
                getMainClass(f, mainClass);
            } else {
                // 处理class
                try {
                    if(!f.getName().endsWith(".java")){
                        return;
                    }
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line, packageStr = "";
                    while((line = br.readLine()) != null){
                        line = line.trim();
                        if(line.startsWith("package ")){
                            packageStr = line.substring(8).replace(";", "");
                        }
                        if(line.equals("@SquareApplication")){
                            mainClass.append(packageStr+"."+f.getName().replace(".java", ""));
                        }
                    }
                } catch (Exception e) {
                    getLog().error("Find Main-Class error!", e);
                }
            }
        }
    }
}
