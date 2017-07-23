package io.github.gmathi.novellibrary.model;

public class NovelGenre {
    private long novelId;
    private long genreId;

    public NovelGenre() {

    }

    public NovelGenre(long novelId, long genreId) {
        this.novelId = novelId;
        this.genreId = genreId;
    }
    
    public long getNovelId() {
        return this.novelId;
    }
    
    public void setNovelId(long novelId) {
        this.novelId = novelId;
    }
    
    public long getGenreId() {
        return this.genreId;
    }
    
    public void setGenreId(long genreId) {
        this.genreId = genreId;
    }
    
}
