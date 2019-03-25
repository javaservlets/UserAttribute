package com.example.forgerock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import static com.sun.identity.idm.AMIdentityRepository.debug;

public class Reader {
    static String accessTkn = null;
    static String usr = null;
    static String url = null;
    //todo  if no MsdnNumber exists, don' throw error //rj

    public Reader(String passedUsr, String url) {
        try {
            this.usr = passedUsr; // usr is passed in (w/ """) from OOTB usrname node
            if (usr.startsWith("\"")) {
                usr = usr.substring(1, passedUsr.length() - 1);
            }

            this.url = url;
            accessTkn = getToken(); //
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getAttributes(String attrName) {
        //  todo  replace below Post w/
        //   AMIdentity userIdentity = coreWrapper.getIdentity(context.sharedState.get(USERNAME).asString(),context.sharedState.get(REALM).asString());
        //   Set<String> idAttrs = userIdentity.getAttribute(config.profileAttribute());
        //   if (idAttrs == null || idAttrs.isEmpty() ) {
        //
        //    }

        String attr = "", payload = "";
        if (accessTkn.equals(null)) return payload; // config is wrong somewhere since u don't have an access tkn
        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpGet http = new HttpGet(url + "/openam/json/realms/root/users/" + usr); //todo port num should b configurable as well
            http.setHeader("X-Requested-With", "XMLHttpRequest");
            http.setHeader("Connection", "keep-alive");
            http.setHeader("Content-Type", "application/json");
            http.setHeader("Accept-API-Version", "resource=2.0, protocol=1.0");
            http.setHeader("cache-control", "no-cache");
            http.setHeader("Cookie", "amlbcookie=01; iPlanetDirectoryPro=" + accessTkn); // tkn was init'd during class instantiation
            HttpResponse response = httpclient.execute(http);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                payload = EntityUtils.toString(entity);
                if (payload.contains(attrName)) attr = stripNoise(payload, attrName);
                log("      iot dna  hash retrieved from usr = " + usr + " is " + attr);
            }
        } catch (Exception e) {
            log(e.toString());
        }
        return attr;
    }

    public static String getToken() {
        String cook = "";
        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost http = new HttpPost(url + "/openam/json/realms/root/authenticate");
            http.setHeader("X-OpenAM-Username", "amadmin"); //todo pass these in as well via config
            http.setHeader("X-OpenAM-Password", "password");
            http.setHeader("Content-Type", "application/json");
            http.setHeader("Accept-API-Version", "resource=2.0, protocol=1.0");
            http.setHeader("cache-control", "no-cache");
            HttpResponse response = httpclient.execute(http);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                cook = EntityUtils.toString(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stripNoise(cook, "tokenId");
    }

    private static String stripNoise(String parent, String child) {
        String noise = "";
        try {
            JSONObject jobj = new JSONObject(parent);
            Object idtkn = jobj.getString(child);
            noise = idtkn.toString();

            if (noise.startsWith("[")) { // get only 'value' from "["value"]"
                noise = noise.substring(1, noise.length() - 1);
            }
            if (noise.startsWith("\"")) {
                noise = noise.substring(1, noise.length() - 1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return noise;
        }
    }

    public static void log(String str) {
        debug.message("\r\n           msg:" + str + "\r\n");
    }

}