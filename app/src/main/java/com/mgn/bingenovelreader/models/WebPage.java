package com.mgn.bingenovelreader.models;

public class WebPage {

    public WebPage() {
        //Empty Constructor
    }

    public WebPage(String url, String title, String pageData) {
        this.url = url;
        this.title = title;
        this.pageData = pageData;
    }


    private long id;
    private String url;
    private String title;
    private String fileName;
    private long fictionId;
    public String pageData;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFictionId() {
        return this.fictionId;
    }

    public void setFictionId(long fictionId) {
        this.fictionId = fictionId;
    }

}
