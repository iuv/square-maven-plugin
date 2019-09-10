# square-maven-plugin
Square框架maven打包插件

# 使用方法

1. 获取代码
2. 执行mvn clean install
3. 在使用square框架的在上中添加build

在使用square框架的项目的pom文件中添加：
```xml
  <build>
        <plugins>
            <plugin>
                <groupId>com.jisuye</groupId>
                <artifactId>square-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

使用 mvn clean package 打包
