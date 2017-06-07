package com.mgn.bingenovelreader.models;

public class NovelGenre {
    private long id;
    private long fictionId;
    private long genreId;
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getId() {
        return this.id;
    }
    
    public long getFictionId() {
        return this.fictionId;
    }
    
    public void setFictionId(long fictionId) {
        this.fictionId = fictionId;
    }
    
    public long getGenreId() {
        return this.genreId;
    }
    
    public void setGenreId(long genreId) {
        this.genreId = genreId;
    }
    
}
