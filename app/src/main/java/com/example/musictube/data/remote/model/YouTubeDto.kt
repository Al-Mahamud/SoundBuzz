package com.example.musictube.data.remote.model

import com.google.gson.annotations.SerializedName

data class YouTubeSearchResponse(
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("items") val items: List<YouTubeSearchResultItem>? = null
)

data class YouTubeSearchResultItem(
    @SerializedName("id") val id: YouTubeResourceId? = null,
    @SerializedName("snippet") val snippet: YouTubeSnippet? = null
)

data class YouTubeResourceId(
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("videoId") val videoId: String? = null,
    @SerializedName("channelId") val channelId: String? = null,
    @SerializedName("playlistId") val playlistId: String? = null
)

data class YouTubeVideoListResponse(
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("items") val items: List<YouTubeVideoItem>? = null
)

data class YouTubeVideoItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("snippet") val snippet: YouTubeSnippet? = null,
    @SerializedName("contentDetails") val contentDetails: YouTubeContentDetails? = null,
    @SerializedName("statistics") val statistics: YouTubeStatistics? = null
)

data class YouTubeSnippet(
    @SerializedName("publishedAt") val publishedAt: String? = null,
    @SerializedName("channelId") val channelId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("thumbnails") val thumbnails: YouTubeThumbnails? = null,
    @SerializedName("channelTitle") val channelTitle: String? = null
)

data class YouTubeThumbnails(
    @SerializedName("default") val defaultThumb: YouTubeThumbnailInfo? = null,
    @SerializedName("medium") val medium: YouTubeThumbnailInfo? = null,
    @SerializedName("high") val high: YouTubeThumbnailInfo? = null,
    @SerializedName("standard") val standard: YouTubeThumbnailInfo? = null,
    @SerializedName("maxres") val maxres: YouTubeThumbnailInfo? = null
) {
    fun bestUrl(): String {
        return maxres?.url ?: high?.url ?: medium?.url ?: defaultThumb?.url.orEmpty()
    }
}

data class YouTubeThumbnailInfo(
    @SerializedName("url") val url: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)

data class YouTubeContentDetails(
    @SerializedName("duration") val duration: String? = null
)

data class YouTubeStatistics(
    @SerializedName("viewCount") val viewCount: String? = null,
    @SerializedName("likeCount") val likeCount: String? = null
)

data class YouTubeChannelResponse(
    @SerializedName("items") val items: List<YouTubeChannelItem>? = null
)

data class YouTubeChannelItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("snippet") val snippet: YouTubeSnippet? = null,
    @SerializedName("statistics") val statistics: YouTubeChannelStatistics? = null
)

data class YouTubeChannelStatistics(
    @SerializedName("subscriberCount") val subscriberCount: String? = null,
    @SerializedName("videoCount") val videoCount: String? = null
)
