package com.example.musictube.data.remote.api

import com.example.musictube.data.remote.model.YouTubeChannelResponse
import com.example.musictube.data.remote.model.YouTubeSearchResponse
import com.example.musictube.data.remote.model.YouTubeVideoListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoCategoryId") videoCategoryId: String = "10", // Music category
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("videoSyndicated") videoSyndicated: String = "true",
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getPopularVideos(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("chart") chart: String = "mostPopular",
        @Query("videoCategoryId") videoCategoryId: String = "10",
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String
    ): YouTubeVideoListResponse

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("id") videoIds: String,
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("key") apiKey: String
    ): YouTubeVideoListResponse

    @GET("channels")
    suspend fun getChannel(
        @Query("part") part: String = "snippet,statistics",
        @Query("id") channelId: String,
        @Query("key") apiKey: String
    ): YouTubeChannelResponse
}
