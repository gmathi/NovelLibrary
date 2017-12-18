//package io.github.gmathi.novellibrary.util;
//
//import android.util.Log;
//
//import com.eclipsesource.v8.V8;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.CookieHandler;
//import java.net.CookieManager;
//import java.net.CookiePolicy;
//import java.net.CookieStore;
//import java.net.HttpCookie;
//import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * Created by zhkrb on 2017/3/30.
// */
//
//public class CloudFlare {
//
//    private CookieManager cm;
//    private String UA;
//    private String url;
//
//
//    public CloudFlare(String url) {
//        this.url = url;
//    }
//
//    public List<HttpCookie> cookiesMap() {
//        cm = new CookieManager();
//        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL); //接受所有cookies
//        CookieHandler.setDefault(cm);
//
//        try {
//            /**
//             * 第一次链接得到响应头为503的DDOS page；
//             * 其他则直接返回
//             * 正则时没有匹配到也返回
//             */
//            URL ConnUrl = new URL(url);
//            HttpURLConnection conn = (HttpURLConnection) ConnUrl.openConnection();
//            conn.setRequestProperty("User-Agent", getUA());
//            conn.connect();
//            if (conn.getResponseCode() == 503) {
//                InputStream is = conn.getErrorStream();
//                BufferedReader br = new BufferedReader(new InputStreamReader(is));
//                StringBuilder sb = new StringBuilder();
//                String str;
//                while ((str = br.readLine()) != null) {
//                    sb.append(str);
//                }
//                is.close();
//                br.close();
//
//                CookieStore ck = cm.getCookieStore();
//
//
//                conn.disconnect();
//
//
//                str = sb.toString();
//                System.out.println(str);
//                String jschl_vc = regex(str, "name=\"jschl_vc\" value=\"(.+?)\"").get(0);    //正则取值
//                String pass = regex(str, "name=\"pass\" value=\"(.+?)\"").get(0);            //
//                int jschl_answer = get_answer(str);                                         //计算结果
//                System.out.println(jschl_answer);
//
//                Thread.sleep(4000);
//
//                /*
//                  第二次链接得到相应头为302的页面并取得cookies；
//                 */
//                String req = String.valueOf("https://" + ConnUrl.getHost()) + "/cdn-cgi/l/chk_jschl?"
//                        + "jschl_vc=" + jschl_vc + "&pass=" + pass + "&jschl_answer=" + jschl_answer;
//                System.out.println(req);
//                HttpURLConnection.setFollowRedirects(false);
//                HttpURLConnection reqconn = (HttpURLConnection) new URL(req).openConnection();
//
//                reqconn.setRequestProperty("Referer", req);
//                reqconn.setRequestProperty("User-Agent", getUA());
//                reqconn.setRequestProperty("Cookie", ck.getCookiesMap().toString());
//                reqconn.connect();
//                if (reqconn.getResponseCode() == 302) {
//                    CookieStore ck1 = cm.getCookieStore();
//                    reqconn.disconnect();
//                    System.out.println(reqconn.getHeaderFields());
//
//                    /*
//                      同上
//                     */
//                    HttpURLConnection conn302 = (HttpURLConnection) new URL(req).openConnection();
//                    conn302.setRequestProperty("Referer", ConnUrl.getHost());
//                    conn302.setRequestProperty("User-Agent", getUA());
//                    conn302.setRequestProperty("Cookie", ck1.getCookiesMap().toString());
//                    conn302.connect();
//                    if (conn302.getResponseCode() == 302) {
//                        System.out.println("conn302:302");
//                        Log.i("conn302", conn302.getHeaderFields().toString());
//                        CookieStore ck2 = cm.getCookieStore();
//                        Log.i("conn302", ck2.getCookiesMap().toString());
//                        conn302.disconnect();
//                        return ck2.getCookiesMap();
//                    }
//                }
//            } else {
//                CookieStore cookieStore = cm.getCookieStore();
//                return cookieStore.getCookiesMap();
//
//            }
//        } catch (NullPointerException e) {
//            Log.e("Err", "Must set UA");
//        } catch (IndexOutOfBoundsException e) {
//            CookieStore cookieStore = cm.getCookieStore();
//            return cookieStore.getCookiesMap();
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    private int get_answer(String str) {  //取值
//        int a = 0;
//
//        try {
//            List<String> s = regex(str, "var s,t,o,p,b,r,e,a,k,i,n,g,f, (.+?)=\\{\"(.+?)\"");
//            System.out.println(s);
//            String varA = s.get(0);
//            String varB = s.get(1);
//            StringBuilder sb = new StringBuilder();
//            sb.append("a=");
//            sb.append(regex(str, varA + "=\\{\"" + varB + "\":(.+?)\\}").get(0));
//            sb.append(";");
//            List<String> b = regex(str, varA + "\\." + varB + "(.+?)\\;");
//            for (int i = 0; i < b.size() - 1; i++) {
//                sb.append("a");
//                sb.append(b.get(i));
//                sb.append(";");
//            }
//            Log.i("add", sb.toString());
//            V8 v8 = V8.createV8Runtime();
//            a = v8.executeIntegerScript(sb.toString());
//
//            a += new URL(url).getHost().length();
//        } catch (IndexOutOfBoundsException e) {
//            Log.e("answerErr", "get answer error");
//            e.printStackTrace();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//        return a;
//    }
//
//    private String getUA() {
//        return UA;
//    }
//
//    public void setUA(String UA) {
//        this.UA = UA;
//    }
//
//
//    private List<String> regex(String text, String pattern) {    //正则
//        try {
//            Pattern pt = Pattern.compile(pattern);
//            Matcher mt = pt.matcher(text);
//            List<String> group = new ArrayList<>();
//
//            while (mt.find()) {
//                if (mt.groupCount() >= 1) {
//                    if (mt.groupCount() > 1) {
//                        group.add(mt.group(1));
//                        group.add(mt.group(2));
//                    } else group.add(mt.group(1));
//
//
//                }
//            }
//            return group;
//        } catch (NullPointerException e) {
//            Log.i("MATCH", "null");
//        }
//        return null;
//    }
//
//    public Map<String, String> List2Map(List<HttpCookie> list) {  //转换为jsoup可用的map
//        Map<String, String> map = new HashMap<>();
//        try {
//            if (list != null) {
//                for (int i = 0; i < list.size(); i++) {
//                    String[] listStr = list.get(i).toString().split("=");
//                    map.put(listStr[0], listStr[1]);
//                }
//                Log.i("List2Map", map.toString());
//            } else return map;
//
//        } catch (IndexOutOfBoundsException e) {
//            e.printStackTrace();
//        }
//
//
//        return map;
//    }
//
//}
//
//
//
//
