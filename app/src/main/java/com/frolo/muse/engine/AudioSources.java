package com.frolo.muse.engine;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;


public final class AudioSources {
    private AudioSources() {
    }

    private static final class SimpleAudioMetadata implements AudioMetadata {

        final String title;
        final long albumId;
        final String album;
        final long artistId;
        final String artist;
        final String genre;
        final int duration;
        final int year;
        final int trackNumber;

        SimpleAudioMetadata(
            String title,
            long albumId,
            String album,
            long artistId,
            String artist,
            String genre,
            int duration,
            int year,
            int trackNumber
        ) {
            this.title = title != null ? title : "";
            this.albumId = albumId;
            this.album = album != null ? album : "";
            this.artistId = artistId;
            this.artist = artist != null ? artist : "";
            this.genre = genre != null ? genre : "";
            this.duration = duration;
            this.year = year;
            this.trackNumber = trackNumber;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (obj != null && obj instanceof SimpleAudioMetadata) {
                SimpleAudioMetadata another = (SimpleAudioMetadata) obj;
                return Objects.equals(title, another.title)
                        && albumId == another.albumId
                        && Objects.equals(album, another.album)
                        && artistId == another.artistId
                        && Objects.equals(artist, another.artist)
                        && Objects.equals(genre, another.genre)
                        && duration == another.duration
                        && year == another.year
                        && trackNumber == another.trackNumber;
            } else return false;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public String getAlbum() {
            return album;
        }

        public long getAlbumId() {
            return albumId;
        }

        public int getDuration() {
            return duration;
        }

        public int getYear() {
            return year;
        }

        public String getGenre() {
            return genre;
        }

        public long getArtistId() {
            return artistId;
        }

        @Override
        public int getTrackNumber() {
            return trackNumber;
        }
    }

    private static final class SimpleAudioSource implements AudioSource {

        final long id;
        final String source;
        final AudioMetadata metadata;

        SimpleAudioSource(long id, String source, AudioMetadata metadata) {
            this.id = id;
            this.source = source;
            this.metadata = metadata;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getSource() {
            return source;
        }

        @NotNull
        @Override
        public AudioMetadata getMetadata() {
            return metadata;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (!(o instanceof SimpleAudioSource)) return false;
            SimpleAudioSource that = (SimpleAudioSource) o;
            return id == that.id &&
                    Objects.equals(source, that.source) &&
                    Objects.equals(metadata, that.metadata);
        }

    }

    @NotNull
    public static AudioSource createAudioSource(long id, @NotNull String source, @NotNull AudioMetadata metadata) {
        return new SimpleAudioSource(id, source, metadata);
    }

    @NotNull
    public static AudioSource copyAudioSource(@NotNull AudioSource other) {
        return createAudioSource(other.getId(), other.getSource(), copyMetadata(other.getMetadata()));
    }

    @NotNull
    public static AudioMetadata createMetadata(
        String title,
        long albumId,
        String album,
        long artistId,
        String artist,
        String genre,
        int duration,
        int year,
        int trackNumber
    ) {
        return new SimpleAudioMetadata(title, albumId, album, artistId, artist, genre, duration, year, trackNumber);
    }

    @NotNull
    public static AudioMetadata copyMetadata(@NotNull AudioMetadata metadata) {
        return createMetadata(
            metadata.getTitle(),
            metadata.getAlbumId(),
            metadata.getAlbum(),
            metadata.getArtistId(),
            metadata.getArtist(),
            metadata.getGenre(),
            metadata.getDuration(),
            metadata.getYear(),
            metadata.getTrackNumber()
        );
    }

    public static boolean areSourcesTheSame(@NotNull AudioSource item1, @NotNull AudioSource item2) {
        return Objects.equals(item1.getSource(), item2.getSource());
    }

}