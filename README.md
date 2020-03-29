# swagger-sample-json-generater
this project generate sample json from swagger 

## usage

### run

```
./gradlew run
```

### create jar

```
./gradlew shadowJar
```

```
java -jar build/libs/swagger-mock-json-generater-1.0-SNAPSHOT-all.jar \
  -i {{input_swagger_file_name}} \
  -o {{output_directory}}
```


If you do not need an empty file, include "--noEmptyFIle" as an argument.

