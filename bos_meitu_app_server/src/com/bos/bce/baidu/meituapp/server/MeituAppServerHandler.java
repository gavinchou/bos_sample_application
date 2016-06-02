package com.bos.bce.baidu.meituapp.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

// baidu bce services
import com.baidubce.BceClientConfiguration;
import com.baidubce.auth.BceCredentials;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.sts.StsClient;
import com.baidubce.services.sts.model.GetSessionTokenRequest;
import com.baidubce.services.sts.model.GetSessionTokenResponse;

// json utils
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// jetty
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MeituAppServerHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        System.out.println(baseRequest.getQueryParameters());
        System.out.println(baseRequest.getQueryString());
        System.out.println(request.getQueryString());
        System.out.println(request.getParameterMap());

        // Inform jetty that this request has now been handled
        baseRequest.setHandled(true);

        if (!request.getMethod().equalsIgnoreCase("GET")) {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }

        // expected url example: localhost:8080/?command=stsToken&type=download
        Map<String, String[]> paramMap = request.getParameterMap();
        if (paramMap.get("command") == null || paramMap.get("type") == null) {
            // invalid request
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!paramMap.get("command")[0].equalsIgnoreCase("stsToken")) {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }

        String responseBody = "";
        responseBody = getBosInfo(paramMap.get("type")[0]);

        // Declare response encoding and types
        response.setContentType("application/json; charset=utf-8");

        // Declare response status code
        response.setStatus(HttpServletResponse.SC_OK);

        // Write back response, utf8 encoded
        response.getWriter().println(responseBody);
    }

    /**
     * Generates bos info needed by app according to requset type(upload, download etc)
     * this is the key part for uploading file to bos with sts token
     * @param bosRequestType 
     * @return utf8 encoded json string
     */
    public String getBosInfo(String bosRequestType) {
        // configuration for getting stsToken
        // bce bos credentials ak sk
        String bosAk = "ak_of_third_party_developer";
        String bosSk = "sk_of_third_party_developer";
        // bce sts service endpoint
        String stsEndpoint = "http://sts.bj.baidubce.com";

        BceCredentials credentials = new DefaultBceCredentials(bosAk, bosSk);
        BceClientConfiguration clientConfig = new BceClientConfiguration();
        clientConfig.setCredentials(credentials);
        clientConfig.setEndpoint(stsEndpoint);
        StsClient stsClient = new StsClient(clientConfig);
        GetSessionTokenRequest stsReq = new GetSessionTokenRequest();
        // request expiration time
        stsReq.setDurationSeconds(1800);
        GetSessionTokenResponse stsToken = stsClient.getSessionToken(stsReq);
        String stsTokenAk = stsToken.getCredentials().getAccessKeyId();
        String stsTokenSk = stsToken.getCredentials().getSecretAccessKey();
        String stsTokenSessionToken = stsToken.getCredentials().getSessionToken();

        // **to simplify this demo there is no difference between "download" and "upload"**
        // parts of bos info
        String bosEndpoint = "http://bos.bj.baidubce.com";
        String bucketName = "bos-android-sdk-app";
        if (bosRequestType.equalsIgnoreCase("download-processed")) {
            // the binded image processing domain set by App developer on bce console
            bosEndpoint = "http://" + bucketName + ".bceimg.com";
        }

        // prefix is the bucket name, and does not specify the object name
        BosInfo bosInfo = new BosInfo(stsTokenAk, stsTokenSk, stsTokenSessionToken, bosEndpoint,
                bucketName, "", bucketName);

        String res = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            res = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bosInfo);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            res = "";
        }
        System.out.println(res);
        try {
            res = new String(res.getBytes(), "utf8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            res = "";
        }
        return res;
    }

    class BosInfo {
        private String sk = "";
        private String stsToken = "";
        private String endpoint = "";
        private String bucketName = "";
        private String objectName = "";
        private String prefix = "";
        public BosInfo(String ak, String sk, String stsToken, String endpoint, String bucketName, 
                String objectName, String prefix) {
            this.ak = ak;
            this.sk = sk;
            this.stsToken = stsToken;
            this.endpoint = endpoint;
            this.bucketName = bucketName;
            this.objectName = objectName;
            this.prefix = prefix;
        }
        
        @SuppressWarnings("unused")
        private BosInfo() {
        }

        private String ak = "";
        public String getAk() {
            return ak;
        }

        public void setAk(String ak) {
            this.ak = ak;
        }

        public String getSk() {
            return sk;
        }

        public void setSk(String sk) {
            this.sk = sk;
        }

        public String getStsToken() {
            return stsToken;
        }

        public void setStsToken(String stsToken) {
            this.stsToken = stsToken;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getObjectName() {
            return objectName;
        }

        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

    }
}
