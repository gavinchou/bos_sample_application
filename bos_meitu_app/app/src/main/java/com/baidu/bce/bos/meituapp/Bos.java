package com.baidu.bce.bos.meituapp;


import com.baidubce.auth.BceCredentials;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.auth.DefaultBceSessionCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;

import java.io.File;
import java.io.InputStream;

/**
 * Created by gavin on 5/14/16.
 */
public class Bos {
    private String ak = null;
    private String sk = null;
    private String endpoint = null;
    private String stsToken = null;
    private BosClient client = null;

    public Bos(String ak, String sk, String endpoint, String stsToken) {
        this.ak = ak;
        this.sk = sk;
        this.endpoint = endpoint;
        this.stsToken = stsToken;
        client = createClient();
    }

    public BosClient createClient() {
        BosClientConfiguration config = new BosClientConfiguration();
        BceCredentials credentials = null;
        if (stsToken != null && !stsToken.equalsIgnoreCase("")) {
            credentials = new DefaultBceSessionCredentials(ak, sk, stsToken);
        } else {
            credentials = new DefaultBceCredentials(ak, sk);
        }
        config.setEndpoint(endpoint);
        config.setCredentials(credentials);
        return new BosClient(config);
    }

    public void uploadFile(String bucket, String object, File file) {
        client.putObject(bucket, object, file);
    }

    public void uploadFile(String bucket, String object, InputStream inputStream) {
        client.putObject(bucket, object, inputStream);
    }

    public void uploadFile(String bucket, String object, byte[] data) {
        client.putObject(bucket, object, data);
    }

    public byte[] downloadFileContent(String bucket, String object) {
        return client.getObjectContent(bucket, object);
    }

}
