# square-maven-plugin
Square框架maven打包插件

# 使用方法
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
