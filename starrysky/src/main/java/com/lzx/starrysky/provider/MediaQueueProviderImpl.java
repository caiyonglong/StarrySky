package com.lzx.starrysky.provider;

import android.graphics.Bitmap;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import com.lzx.starrysky.BaseMediaInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * 媒体信息提供类
 */
public class MediaQueueProviderImpl implements MediaQueueProvider {

    //使用Map在查找方面会效率高一点
    private LinkedHashMap<String, SongInfo> mSongInfoListById;
    private LinkedHashMap<String, MediaMetadataCompat> mMediaMetadataCompatListById;
    private List<SongInfo> mSongInfos;

    public MediaQueueProviderImpl() {
        mSongInfoListById = new LinkedHashMap<>();
        mMediaMetadataCompatListById = new LinkedHashMap<>();
    }




    /**
     * 获取List#SongInfo
     */
    @Override
    public List<SongInfo> getSongInfos() {
        return mSongInfos == null ? new ArrayList<>() : mSongInfos;
    }

    /**
     * 设置播放列表
     */
    @Override
    public synchronized void setSongInfos(List<SongInfo> songInfos) {
        mSongInfoListById.clear();
        mSongInfos = songInfos;
        for (SongInfo info : songInfos) {
            mSongInfoListById.put(info.getSongId(), info);
        }
        mMediaMetadataCompatListById = toMediaMetadata(songInfos);
    }

    /**
     * 添加一首歌
     */
    @Override
    public synchronized void addSongInfo(SongInfo songInfo) {
        if (!mSongInfos.contains(songInfo)) {
            mSongInfos.add(songInfo);
        }
        mSongInfoListById.put(songInfo.getSongId(), songInfo);
        mMediaMetadataCompatListById.put(songInfo.getSongId(), toMediaMetadata(songInfo));
    }

    /**
     * 根据检查是否有某首音频
     */
    @Override
    public boolean hasSongInfo(String songId) {
        return mSongInfoListById.containsKey(songId);
    }

    /**
     * 根据songId获取SongInfo
     */
    @Override
    public SongInfo getSongInfo(String songId) {
        if (TextUtils.isEmpty(songId)) {
            return null;
        }
        if (mSongInfoListById.containsKey(songId)) {
            return mSongInfoListById.get(songId);
        } else {
            return null;
        }
    }

    /**
     * 根据songId获取索引
     */
    @Override
    public int getIndexBySongInfo(String songId) {
        SongInfo songInfo = getSongInfo(songId);
        return songInfo != null ? getSongInfos().indexOf(songInfo) : -1;
    }

    /**
     * 获取List#MediaMetadataCompat
     */
    @Override
    public List<MediaMetadataCompat> getMusicList() {
        return new ArrayList<>(mMediaMetadataCompatListById.values());
    }

    /**
     * 获取 List#MediaBrowserCompat.MediaItem 用于 onLoadChildren 回调
     */
    @Override
    public List<MediaBrowserCompat.MediaItem> getChildrenResult() {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        List<MediaMetadataCompat> list = new ArrayList<>(mMediaMetadataCompatListById.values());
        for (MediaMetadataCompat metadata : list) {
            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    metadata.getDescription(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    /**
     * 获取乱序列表
     */
    @Override
    public Iterable<MediaMetadataCompat> getShuffledMediaMetadataCompat() {
        List<MediaMetadataCompat> shuffled = new ArrayList<>(mMediaMetadataCompatListById.size());
        shuffled.addAll(mMediaMetadataCompatListById.values());
        Collections.shuffle(shuffled);
        return shuffled;
    }

    @Override
    public Iterable<SongInfo> getShuffledSongInfo() {
        if (mSongInfos != null) {
            Collections.shuffle(mSongInfos);
            return mSongInfos;
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 根据id获取对应的MediaMetadataCompat对象
     */
    @Override
    public MediaMetadataCompat getMusic(String songId) {
        return mMediaMetadataCompatListById.containsKey(songId) ? mMediaMetadataCompatListById.get(songId) : null;
    }

    /**
     * 更新封面art
     */
    @Override
    public synchronized void updateMusicArt(String songId, MediaMetadataCompat changeData, Bitmap albumArt, Bitmap icon) {
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder(changeData)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
                .build();
        mMediaMetadataCompatListById.put(songId, metadata);
    }

    /**
     * List<SongInfo> 转 ConcurrentMap<String, MediaMetadataCompat>
     */
    private synchronized static LinkedHashMap<String, MediaMetadataCompat> toMediaMetadata(List<SongInfo> songInfos) {
        LinkedHashMap<String, MediaMetadataCompat> map = new LinkedHashMap<>();
        for (SongInfo info : songInfos) {
            MediaMetadataCompat metadataCompat = toMediaMetadata(info);
            map.put(info.getSongId(), metadataCompat);
        }
        return map;
    }

    /**
     * SongInfo 转 MediaMetadataCompat
     */
    private synchronized static MediaMetadataCompat toMediaMetadata(SongInfo info) {
        String albumTitle = "";
        if (!TextUtils.isEmpty(info.getAlbumName())) {
            albumTitle = info.getAlbumName();
        } else if (!TextUtils.isEmpty(info.getSongName())) {
            albumTitle = info.getSongName();
        }
        String songCover = "";
        if (!TextUtils.isEmpty(info.getSongCover())) {
            songCover = info.getSongCover();
        } else if (!TextUtils.isEmpty(info.getAlbumCover())) {
            songCover = info.getAlbumCover();
        }
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, info.getSongId());
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, info.getSongUrl());
        if (!TextUtils.isEmpty(albumTitle)) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle);
        }
        if (!TextUtils.isEmpty(info.getArtist())) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, info.getArtist());
        }
        if (info.getDuration() != -1) {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.getDuration());
        }
        if (!TextUtils.isEmpty(info.getGenre())) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, info.getGenre());
        }
        if (!TextUtils.isEmpty(songCover)) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, songCover);
        }
        if (!TextUtils.isEmpty(info.getSongName())) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.getSongName());
        }
        if (info.getTrackNumber() != -1) {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, info.getTrackNumber());
        }
        builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, info.getAlbumSongCount());
        return builder.build();
    }
}
