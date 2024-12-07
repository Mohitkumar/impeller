package com.dtc.impeller.action;
import com.dtc.impeller.flow.ActionParam;
import com.dtc.impeller.flow.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class HttpAction extends AbstractAction {

    private ObjectMapper mapper = new ObjectMapper();

    private HttpClient httpClient;

    private static final Logger logger = LoggerFactory.getLogger(HttpAction.class);

    @ActionParam
    String url;

    @ActionParam
    String method;

    @ActionParam(optional = true)
    Map<String, String> headers;

    @ActionParam(optional = true)
    Map<String, Object> queryParams;

    @ActionParam(optional = true)
    List<Object> pathParams;

    @ActionParam(optional = true)
    Object body;

    @ActionParam(optional = true)
    String bodyStr;

    private StringBuilder urlBuilder;

    public HttpAction(String name, int retryCount) {
        super(name, retryCount);
        httpClient = HttpClient.newBuilder().build();
    }


    @Override
    public Result<?, ? extends Throwable> run() {
        urlBuilder = new StringBuilder();
        urlBuilder.append(url);
        if(pathParams != null){
            for (Object pathParam : pathParams) {
                urlBuilder.append("/").append(pathParam);
            }
        }
        if(queryParams != null){
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if(url.toString().contains("?")){
                    urlBuilder.append("&").append(key).append("=").append(value);
                }else{
                    urlBuilder.append("?").append(key).append("=").append(value);
                }
            }
        }
        try{
            if(body != null){
                if(body instanceof String){
                    bodyStr = (String)body;
                }else{
                    bodyStr = mapper.writeValueAsString(body);
                }
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            if(headers != null){
                headers.forEach(builder::header);
            }
            HttpRequest request = builder.uri(new URI(urlBuilder.toString())).method(method, HttpRequest.BodyPublishers.noBody()).build();
            if(bodyStr != null){
               request = builder.uri(new URI(urlBuilder.toString())).method(method, HttpRequest.BodyPublishers.ofString(bodyStr)).build();
            }
            debugMessage("sending http request url={},body={},headers={}",Map.of("url",urlBuilder,"body",bodyStr != null ? bodyStr : "","headers",headers));
            String responseBody = null;
            @SuppressWarnings("rawtypes")
            HttpResponse response = null;

            if (headers.containsKey("Accept-Encoding") && headers.get("Accept-Encoding").equals("gzip")) {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                logger.info("http response code for uri {} code {}", response.request().uri(), response.statusCode());

                if ("gzip".equals(response.headers().firstValue("Content-Encoding").orElse(""))) {
                    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[]) response.body()); GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream); InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8); BufferedReader reader = new BufferedReader(inputStreamReader)) {

                        StringBuilder responseBodyBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBodyBuilder.append(line);
                        }
                        responseBody = responseBodyBuilder.toString();
                    }
                }
            } else {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                responseBody = (String) response.body();
            }
            logger.info("http response status={},body={}", response.statusCode(), responseBody);
            debugMessage("http response status={},body={}",Map.of("statusCode",response.statusCode(),"responseBody",responseBody != null ? responseBody : ""));
            if(responseBody == null){
                throw new Exception("error making http call body is null");
            }

            if(response.statusCode() != 200 && response.statusCode() != 202){
                throw new Exception(responseBody);
            }

            if (responseBody.startsWith("{")) {
                TypeReference<HashMap<String,Object>> typeRef
                        = new TypeReference<HashMap<String,Object>>() {};
                HashMap<String, Object> responseMap = mapper.readValue(responseBody, typeRef);

                // Check if the response contains a StatusCode and StatusText
                if (responseMap.containsKey("StatusCode") && responseMap.containsKey("StatusText")) {
                    if (!responseMap.get("StatusCode").equals(200)) {
                        throw new Exception(responseMap.get("StatusText").toString());
                    }
                }
                return Result.ok(responseMap);
            } else if (responseBody.startsWith("[")) {
                TypeReference<ArrayList<Object>> typeRef
                        = new TypeReference<ArrayList<Object>>() {};
                ArrayList<Object> list = mapper.readValue(responseBody, typeRef);
                return Result.ok(list);
            }else {
                return Result.ok(responseBody);
            }

        }catch (Exception e){
            return Result.error(e);
        }
    }
}
