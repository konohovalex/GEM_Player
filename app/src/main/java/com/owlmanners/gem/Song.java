/* Class, used to storage loaded from device songs' information in POJO */
package com.owlmanners.gem;

import java.util.ArrayList;
import java.util.Objects;

class Song {
    private String            data; // A path to file on device, acts as currentlyPlayingSongName
    private String            artist;
    private String            album;
    private String            title;
    private String            genre;
    private String            year;
    private String            track;
    private long              duration;
    private ArrayList<String> relatedUserPlaylists;
    // Used only for sorting in Playlists menu and Search menu
    private String            fictiveTrack;
    private long              checkpointPosition;

    public Song(String data, String artist, String album, String title, String genre,
                String year, String track, long duration) {
        this.data = data;

        if (artist == null || artist.equals(""))
            this.artist  = CommonStrings.UNKNOWN_ARTIST;
        else this.artist = artist;

        if (album == null || album.equals(""))
            this.album  = CommonStrings.UNKNOWN_ALBUM;
        else this.album = album;

        if (title == null || title.equals(""))
            this.title  = CommonStrings.UNKNOWN_TITLE;
        else this.title = title;

        if (genre == null || genre.equals(""))
            this.genre  = CommonStrings.UNKNOWN_GENRE;
        else this.genre = genre;

        if (year == null || year.length() < 4)
            this.year  = CommonStrings.UNKNOWN_YEAR;
        else this.year = year;

        if (track == null || track.equals(""))
            this.track  = CommonStrings.UNKNOWN_TRACK;
        else this.track = track;

        this.duration        = duration;

        relatedUserPlaylists = new ArrayList<>();

        fictiveTrack         = "";

        checkpointPosition   = 0;
    }

    // Getters
    public String getData() {
        return data;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public String getYear() {
        return year;
    }

    public String getTrack() {
        return track;
    }

    public long getDuration() {
        return duration;
    }

    public ArrayList<String> getRelatedUserPlaylists() {
        return relatedUserPlaylists;
    }

    public String getFictiveTrack() {
        return fictiveTrack;
    }

    // Setters
    public void setData(String data) {
        this.data = data;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setRelatedUserPlaylists(ArrayList<String> relatedUserPlaylists) {
        this.relatedUserPlaylists = relatedUserPlaylists;
    }

    public void setFictiveTrack(String fictiveTrack) {
        this.fictiveTrack = fictiveTrack;
    }

    // Methods to add or remove related user playlist
    public void addRelatedUserPlaylist(String playlist) {
        this.relatedUserPlaylists.add(playlist);
    }

    public void removeRelatedUserPlaylist(String playlist) {
        this.relatedUserPlaylists.remove(playlist);
    }

    // equals(), hashCode() and toString()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(data, song.data);
    }
    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "Song{" +
                "data='" + data + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", year='" + year + '\'' +
                ", track='" + track + '\'' +
                ", duration=" + duration +
                ", relatedUserPlaylists=" + relatedUserPlaylists +
                '}';
    }

    // Addition of {some/some} to songDescription
    public String checkTrack() {
        String result = "";
        if (!track.equals(CommonStrings.UNKNOWN_TRACK)) {
            result += " {" + track + "}";
        }
        return result;
    }

    // Addition of [xxxx] to songDescription
    public String checkYear() {
        String result = "";
        if (!year.equals(CommonStrings.UNKNOWN_YEAR)) {
            result += " [" + year + "]";
        }
        return result;
    }

    // ALL SONGS, UNKNOWN GENRE, SPECIFIC GENRE and USER PLAYLIST song description
    public String getFullSongDescription() {
        String songDescription = title;
        songDescription += checkTrack();
        if (!artist.equals(CommonStrings.UNKNOWN_ARTIST)) {
            songDescription += "\n" + artist;
        }
        if (!album.equals(CommonStrings.UNKNOWN_ALBUM)) {
            songDescription += "\n" + album;
        }
        songDescription += checkYear();
        return songDescription;
    }

    // ALL ARTIST song description
    public String getAllSongsBy_SongDescription() {
        String songDescription = title;
        songDescription += checkTrack();
        if (!album.equals(CommonStrings.UNKNOWN_ALBUM)) {
            songDescription += "\n" + album;
            songDescription += checkYear();
        }
        return songDescription;
    }

    // Complex album info
    public String getComplexAlbumInfo() {
        String complexAlbumInfo = album;
        complexAlbumInfo += checkYear();
        complexAlbumInfo += "\n" + artist;
        return complexAlbumInfo;
    }

    public void setCheckpointPosition(long checkpointPosition) {
        this.checkpointPosition = checkpointPosition;
    }

    public long getCheckpointPosition() {
        return checkpointPosition;
    }
}