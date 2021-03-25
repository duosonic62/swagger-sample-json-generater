package com.example.swagger_mock_json_generater;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.swagger.oas.inflector.examples.ExampleBuilder;
import io.swagger.oas.inflector.examples.models.Example;
import io.swagger.oas.inflector.processors.JsonNodeExampleSerializer;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        // 引数からファイル名を取得
        String swaggerFileName = "swagger.yaml";
        String outputDirName = "mock-json";
        Boolean noEmptyFile = false;
        List<String> arguments = Arrays.asList(args);
        int swaggerIndex = arguments.indexOf("-i");
        if (swaggerIndex >= 0) {
            swaggerFileName = args[swaggerIndex + 1];
        }

        int outputIndex = arguments.indexOf("-o");
        if (outputIndex >= 0) {
            outputDirName = args[outputIndex + 1];
        }

        if (arguments.contains("--noEmptyFile")) {
            noEmptyFile = true;
        }

        OpenAPI swagger = new OpenAPIV3Parser().read(swaggerFileName);
        Components components = swagger.getComponents();
        Map<String, Schema> schemas = components.getSchemas();

        String finalOutputDirName = outputDirName;
        Boolean finalNoEmptyFile = noEmptyFile;
        swagger.getPaths().entrySet()
                .stream()
                .forEach(path -> {
                    PathItem item = path.getValue();

                    Operation get = item.getGet();
                    if (get != null) {
                        write(get.getResponses(), schemas, "get", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                        write(get.getRequestBody(), schemas, "get", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                    }

                    Operation post = item.getPost();
                    if (post != null) {
                        write(post.getResponses(), schemas, "post", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                        write(post.getRequestBody(), schemas, "post", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                    }

                    Operation put = item.getPut();
                    if (put != null) {
                        write(put.getResponses(), schemas, "put", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                        write(put.getRequestBody(), schemas, "put", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                    }

                    Operation delete = item.getDelete();
                    if (delete != null) {
                        write(delete.getResponses(), schemas, "delete", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                        write(delete.getRequestBody(), schemas, "delete", path.getKey(), finalOutputDirName, finalNoEmptyFile);
                    }
                });
    }

    // レスポンスの一覧を書き込み
    private static void write(
            ApiResponses responses,
            Map<String, Schema> schemas,
            String method,
            String path,
            String outputDir,
            Boolean noEmptyFile
    ) {
        // 200系のみJSONの対象にする
        Pattern pattern = Pattern.compile("20.");
        responses.entrySet().stream()
                .filter(response -> pattern.matcher(response.getKey()).find())
                .forEach(response -> {
                    try {
                        write(response.getValue(), schemas, method, path, outputDir, noEmptyFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    // レスポンスを書き込み
    private static void write(
            ApiResponse response,
            Map<String, Schema> schemas,
            String method,
            String path,
            String outputDir,
            Boolean noEmptyFile
    ) throws IOException {
        String fileName = method + path.replace("/", "-").replace("{","_").replace("}","") + ".json";
        // レスポンスのなく、noEmptyFileフラグが立っている場合はファイル作成不要
        if (response.getContent() == null) {
            if (noEmptyFile) return;
            write(fileName, "", outputDir);
            return;
        }

        Schema schema = response.getContent()
                .get("application/json")
                .getSchema();

        Example example = ExampleBuilder.fromSchema(schema, schemas);
        write(fileName, toJsonString(example), outputDir);
    }

    // リクエストを書き込み
    private static void write(
            RequestBody request,
            Map<String, Schema> schemas,
            String method,
            String path,
            String outputDir,
            Boolean noEmptyFile
    ) {
        String fileName = "rqruest-" + method + path.replace("/", "-").replace("{","_").replace("}","") + ".json";
        // レスポンスのなく、noEmptyFileフラグが立っている場合はファイル作成不要
        if (request == null || request.getContent() == null) {
            if (noEmptyFile) return;
            try {
                write(fileName, "", outputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        MediaType type = request.getContent()
                .get("application/json");

        if (type == null) {
            if (noEmptyFile) return;
            try {
                write(fileName, "", outputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Schema schema = type.getSchema();

        Example example = ExampleBuilder.fromSchema(schema, schemas);
        try {
            write(fileName, toJsonString(example), outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String toJsonString(Example example) {
        SimpleModule module = new SimpleModule().addSerializer(new JsonNodeExampleSerializer());
        Json.mapper().registerModule(module);

        return Json.pretty(example);
    }


    private static void write(String name, String json, String outputDir) throws IOException {
        File dir = new File(outputDir);
        // if not exists mock_json directory, create directory.
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new RuntimeException("failed to create directory.");
            }
        }

        File jsonFile = new File(outputDir + "/" + name);
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(json);
        }
    }
}
