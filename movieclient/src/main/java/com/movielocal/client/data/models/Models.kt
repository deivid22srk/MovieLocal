package com.movielocal.client.data.models

import com.google.gson.annotations.SerializedName

data class Movie(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("year")
    val year: Int,
    
    @SerializedName("genre")
    val genre: String,
    
    @SerializedName("rating")
    val rating: Float,
    
    @SerializedName("duration")
    val duration: Int,
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String,
    
    @SerializedName("videoUrl")
    val videoUrl: String,
    
    @SerializedName("type")
    val type: String = "movie"
)

data class Series(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("year")
    val year: Int,
    
    @SerializedName("genre")
    val genre: String,
    
    @SerializedName("rating")
    val rating: Float,
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String,
    
    @SerializedName("type")
    val type: String = "series",
    
    @SerializedName("seasons")
    val seasons: List<Season>
)

data class Season(
    @SerializedName("seasonNumber")
    val seasonNumber: Int,
    
    @SerializedName("episodes")
    val episodes: List<Episode>
)

data class Episode(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("episodeNumber")
    val episodeNumber: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("duration")
    val duration: Int,
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String,
    
    @SerializedName("videoUrl")
    val videoUrl: String
)

data class ContentResponse(
    @SerializedName("movies")
    val movies: List<Movie>,
    
    @SerializedName("series")
    val series: List<Series>
)

data class HealthResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("version")
    val version: String,
    
    @SerializedName("serverTime")
    val serverTime: Long
)

data class VideoProgress(
    @SerializedName("videoId")
    val videoId: String,
    
    @SerializedName("position")
    val position: Long,
    
    @SerializedName("duration")
    val duration: Long,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("completed")
    val completed: Boolean = false
)

data class Profile(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("avatarIcon")
    val avatarIcon: String,
    
    @SerializedName("isKidsMode")
    val isKidsMode: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

data class WatchProgress(
    @SerializedName("profileId")
    val profileId: String,
    
    @SerializedName("contentId")
    val contentId: String,
    
    @SerializedName("contentType")
    val contentType: String,
    
    @SerializedName("progress")
    val progress: Long,
    
    @SerializedName("duration")
    val duration: Long,
    
    @SerializedName("lastWatched")
    val lastWatched: Long = System.currentTimeMillis(),
    
    @SerializedName("completed")
    val completed: Boolean = false
)

data class ProfilesResponse(
    @SerializedName("profiles")
    val profiles: List<Profile>
)

data class ContinueWatchingResponse(
    @SerializedName("continueWatching")
    val continueWatching: List<WatchProgress>
)

data class Channel(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String,
    
    @SerializedName("folderPaths")
    val folderPaths: List<String>,
    
    @SerializedName("isActive")
    val isActive: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

data class ChannelState(
    @SerializedName("channelId")
    val channelId: String,
    
    @SerializedName("currentVideoPath")
    val currentVideoPath: String,
    
    @SerializedName("currentVideoUrl")
    val currentVideoUrl: String,
    
    @SerializedName("currentVideoIndex")
    val currentVideoIndex: Int,
    
    @SerializedName("currentPosition")
    val currentPosition: Long,
    
    @SerializedName("totalVideos")
    val totalVideos: Int,
    
    @SerializedName("lastUpdated")
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ChannelsResponse(
    @SerializedName("channels")
    val channels: List<Channel>
)
