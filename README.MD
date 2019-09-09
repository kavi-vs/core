#执行打包
gradle jar
#如果java使用需要增加依赖： compile "org.jetbrains.kotlin:kotlin-reflect:1.3.0"
发布
gradle clean build bintrayUpload -PbintrayUser=kavi -PbintrayKey=fc139da6990809f29b903c327c27cdc9d0890dd5 -PdryRun=false