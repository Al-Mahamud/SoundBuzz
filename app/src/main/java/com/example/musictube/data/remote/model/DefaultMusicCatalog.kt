package com.example.musictube.data.remote.model

import com.example.musictube.domain.model.Artist
import com.example.musictube.domain.model.Category
import com.example.musictube.domain.model.Track

object DefaultMusicCatalog {

    val categories: List<Category> = listOf(
        Category("pop", "Pop", "Catchy hits & pop anthems", 0xFFE91E63, 0xFFFF4081),
        Category("rock", "Rock", "Classic, indie & modern rock", 0xFFE65100, 0xFFFF9800),
        Category("hiphop", "Hip-Hop", "Beats, rhythm and lyrical flow", 0xFF673AB7, 0xFF9C27B0),
        Category("rap", "Rap", "Hard-hitting bars & freestyle", 0xFF212121, 0xFF757575),
        Category("classical", "Classical", "Timeless orchestral masterpieces", 0xFF3F51B5, 0xFF7986CB),
        Category("edm", "EDM", "High-energy electronic dance beats", 0xFF00E676, 0xFF00B0FF),
        Category("lofi", "Lo-Fi", "Chill beats to relax & study to", 0xFF795548, 0xFFA1887F),
        Category("jazz", "Jazz", "Smooth saxophone & acoustic jazz", 0xFFFF8F00, 0xFFFFD54F),
        Category("kpop", "K-Pop", "Korean pop phenomena & global hits", 0xFFFF007F, 0xFFFF80AB),
        Category("bollywood", "Bollywood", "Indian cinema blockbusters & melodious tunes", 0xFFFF6D00, 0xFFFFAB40),
        Category("bangla", "Bangla", "Soulful Bengali songs & folk rhythms", 0xFF00897B, 0xFF4DB6AC),
        Category("english", "English", "Top global charting chartbusters", 0xFF1E88E5, 0xFF64B5F6),
        Category("workout", "Workout", "Upbeat fuel for intense training", 0xFFD50000, 0xFFFF5252),
        Category("relax", "Relax", "Calming ambient soundscapes", 0xFF00ACC1, 0xFF80DEEA),
        Category("romantic", "Romantic", "Love songs & heartfelt ballads", 0xFFC2185B, 0xFFF06292),
        Category("sad", "Sad", "Melancholy melodies for emotional days", 0xFF37474F, 0xFF90A4AE),
        Category("party", "Party", "Upbeat bangers for the ultimate celebration", 0xFFF50057, 0xFFFF4081)
    )

    val popularArtists: List<Artist> = listOf(
        Artist(
            id = "the_weeknd",
            name = "The Weeknd",
            imageUrl = "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
            subscriberCount = "35.2M",
            description = "Canadian singer-songwriter, producer, and superstar."
        ),
        Artist(
            id = "dua_lipa",
            name = "Dua Lipa",
            imageUrl = "https://img.youtube.com/vi/TUVcZfQe-Kw/hqdefault.jpg",
            subscriberCount = "23.8M",
            description = "English singer and songwriter known for disco-pop sound."
        ),
        Artist(
            id = "ed_sheeran",
            name = "Ed Sheeran",
            imageUrl = "https://img.youtube.com/vi/JGwWNGJdvx8/hqdefault.jpg",
            subscriberCount = "54.1M",
            description = "English singer-songwriter known worldwide for heartfelt ballads."
        ),
        Artist(
            id = "queen",
            name = "Queen",
            imageUrl = "https://img.youtube.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
            subscriberCount = "17.4M",
            description = "Legendary British rock band formed in London in 1970."
        ),
        Artist(
            id = "bts",
            name = "BTS",
            imageUrl = "https://img.youtube.com/vi/gdZLi9oWNZg/hqdefault.jpg",
            subscriberCount = "77.5M",
            description = "World-famous 21st century pop icons from South Korea."
        ),
        Artist(
            id = "arijit_singh",
            name = "Arijit Singh",
            imageUrl = "https://img.youtube.com/vi/VuG7FT9dx4I/hqdefault.jpg",
            subscriberCount = "41.9M",
            description = "Renowned Indian playback singer and music composer."
        ),
        Artist(
            id = "alan_walker",
            name = "Alan Walker",
            imageUrl = "https://img.youtube.com/vi/60ItHLz5WEA/hqdefault.jpg",
            subscriberCount = "45.7M",
            description = "Norwegian DJ and record producer."
        ),
        Artist(
            id = "eminem",
            name = "Eminem",
            imageUrl = "https://img.youtube.com/vi/_Yhyp-_hX2s/hqdefault.jpg",
            subscriberCount = "59.2M",
            description = "American rapper, songwriter, and record producer."
        )
    )

    val catalogTracks: List<Track> = listOf(
        // Pop
        Track(
            id = "4NRXx6U8ABQ",
            youtubeVideoId = "4NRXx6U8ABQ",
            title = "Blinding Lights",
            artist = "The Weeknd",
            channel = "The Weeknd",
            thumbnailUrl = "https://img.youtube.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
            duration = "3:20",
            durationSeconds = 200,
            category = "Pop",
            viewCount = 780000000L
        ),
        Track(
            id = "TUVcZfQe-Kw",
            youtubeVideoId = "TUVcZfQe-Kw",
            title = "Levitating",
            artist = "Dua Lipa",
            channel = "Dua Lipa",
            thumbnailUrl = "https://img.youtube.com/vi/TUVcZfQe-Kw/hqdefault.jpg",
            duration = "3:23",
            durationSeconds = 203,
            category = "Pop",
            viewCount = 920000000L
        ),
        Track(
            id = "JGwWNGJdvx8",
            youtubeVideoId = "JGwWNGJdvx8",
            title = "Shape of You",
            artist = "Ed Sheeran",
            channel = "Ed Sheeran",
            thumbnailUrl = "https://img.youtube.com/vi/JGwWNGJdvx8/hqdefault.jpg",
            duration = "3:53",
            durationSeconds = 233,
            category = "Pop",
            viewCount = 6100000000L
        ),
        Track(
            id = "kffacxfA7G4",
            youtubeVideoId = "kffacxfA7G4",
            title = "Baby",
            artist = "Justin Bieber",
            channel = "Justin Bieber",
            thumbnailUrl = "https://img.youtube.com/vi/kffacxfA7G4/hqdefault.jpg",
            duration = "3:45",
            durationSeconds = 225,
            category = "Pop",
            viewCount = 3000000000L
        ),

        // Rock
        Track(
            id = "fJ9rUzIMcZQ",
            youtubeVideoId = "fJ9rUzIMcZQ",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            channel = "Queen Official",
            thumbnailUrl = "https://img.youtube.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
            duration = "5:55",
            durationSeconds = 355,
            category = "Rock",
            viewCount = 1700000000L
        ),
        Track(
            id = "kXYiU_JCYtU",
            youtubeVideoId = "kXYiU_JCYtU",
            title = "Numb",
            artist = "Linkin Park",
            channel = "Linkin Park",
            thumbnailUrl = "https://img.youtube.com/vi/kXYiU_JCYtU/hqdefault.jpg",
            duration = "3:07",
            durationSeconds = 187,
            category = "Rock",
            viewCount = 2200000000L
        ),
        Track(
            id = "7wtfhZwyrcc",
            youtubeVideoId = "7wtfhZwyrcc",
            title = "Believer",
            artist = "Imagine Dragons",
            channel = "Imagine Dragons",
            thumbnailUrl = "https://img.youtube.com/vi/7wtfhZwyrcc/hqdefault.jpg",
            duration = "3:24",
            durationSeconds = 204,
            category = "Rock",
            viewCount = 2600000000L
        ),

        // Hip-Hop / Rap
        Track(
            id = "_Yhyp-_hX2s",
            youtubeVideoId = "_Yhyp-_hX2s",
            title = "Lose Yourself",
            artist = "Eminem",
            channel = "EminemMusic",
            thumbnailUrl = "https://img.youtube.com/vi/_Yhyp-_hX2s/hqdefault.jpg",
            duration = "5:26",
            durationSeconds = 326,
            category = "Hip-Hop",
            viewCount = 1800000000L
        ),
        Track(
            id = "tvTRZJ-4EyI",
            youtubeVideoId = "tvTRZJ-4EyI",
            title = "HUMBLE.",
            artist = "Kendrick Lamar",
            channel = "Kendrick Lamar",
            thumbnailUrl = "https://img.youtube.com/vi/tvTRZJ-4EyI/hqdefault.jpg",
            duration = "3:03",
            durationSeconds = 183,
            category = "Rap",
            viewCount = 980000000L
        ),
        Track(
            id = "uxpDa-c-4Mc",
            youtubeVideoId = "uxpDa-c-4Mc",
            title = "Hotline Bling",
            artist = "Drake",
            channel = "Drake",
            thumbnailUrl = "https://img.youtube.com/vi/uxpDa-c-4Mc/hqdefault.jpg",
            duration = "4:27",
            durationSeconds = 267,
            category = "Hip-Hop",
            viewCount = 1900000000L
        ),

        // EDM
        Track(
            id = "60ItHLz5WEA",
            youtubeVideoId = "60ItHLz5WEA",
            title = "Faded",
            artist = "Alan Walker",
            channel = "Alan Walker",
            thumbnailUrl = "https://img.youtube.com/vi/60ItHLz5WEA/hqdefault.jpg",
            duration = "3:32",
            durationSeconds = 212,
            category = "EDM",
            viewCount = 3500000000L
        ),
        Track(
            id = "_ovdm2yX4MA",
            youtubeVideoId = "_ovdm2yX4MA",
            title = "Wake Me Up",
            artist = "Avicii",
            channel = "Avicii",
            thumbnailUrl = "https://img.youtube.com/vi/_ovdm2yX4MA/hqdefault.jpg",
            duration = "4:32",
            durationSeconds = 272,
            category = "EDM",
            viewCount = 2400000000L
        ),
        Track(
            id = "gCYcTMt96nM",
            youtubeVideoId = "gCYcTMt96nM",
            title = "Animals",
            artist = "Martin Garrix",
            channel = "Martin Garrix",
            thumbnailUrl = "https://img.youtube.com/vi/gCYcTMt96nM/hqdefault.jpg",
            duration = "3:12",
            durationSeconds = 192,
            category = "EDM",
            viewCount = 1650000000L
        ),

        // Lo-Fi & Relax
        Track(
            id = "jfKfPfyJRdk",
            youtubeVideoId = "jfKfPfyJRdk",
            title = "lofi hip hop radio - beats to relax/study to",
            artist = "Lofi Girl",
            channel = "Lofi Girl",
            thumbnailUrl = "https://img.youtube.com/vi/jfKfPfyJRdk/hqdefault.jpg",
            duration = "3:40",
            durationSeconds = 220,
            category = "Lo-Fi",
            viewCount = 120000000L
        ),
        Track(
            id = "5qap5aO4i9A",
            youtubeVideoId = "5qap5aO4i9A",
            title = "ChilledCow Midnight Reverie",
            artist = "Lofi Girl",
            channel = "Lofi Girl",
            thumbnailUrl = "https://img.youtube.com/vi/5qap5aO4i9A/hqdefault.jpg",
            duration = "4:15",
            durationSeconds = 255,
            category = "Relax",
            viewCount = 85000000L
        ),

        // K-Pop
        Track(
            id = "gdZLi9oWNZg",
            youtubeVideoId = "gdZLi9oWNZg",
            title = "Dynamite",
            artist = "BTS",
            channel = "HYBE LABELS",
            thumbnailUrl = "https://img.youtube.com/vi/gdZLi9oWNZg/hqdefault.jpg",
            duration = "3:43",
            durationSeconds = 223,
            category = "K-Pop",
            viewCount = 1800000000L
        ),
        Track(
            id = "2S24X_cAkdE",
            youtubeVideoId = "2S24X_cAkdE",
            title = "Kill This Love",
            artist = "BLACKPINK",
            channel = "BLACKPINK",
            thumbnailUrl = "https://img.youtube.com/vi/2S24X_cAkdE/hqdefault.jpg",
            duration = "3:13",
            durationSeconds = 193,
            category = "K-Pop",
            viewCount = 1900000000L
        ),

        // Bollywood
        Track(
            id = "VuG7FT9dx4I",
            youtubeVideoId = "VuG7FT9dx4I",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            channel = "T-Series",
            thumbnailUrl = "https://img.youtube.com/vi/VuG7FT9dx4I/hqdefault.jpg",
            duration = "4:22",
            durationSeconds = 262,
            category = "Bollywood",
            viewCount = 800000000L
        ),
        Track(
            id = "kJQP7kiw5Fk",
            youtubeVideoId = "kJQP7kiw5Fk",
            title = "Kesariya",
            artist = "Arijit Singh",
            channel = "Sony Music India",
            thumbnailUrl = "https://img.youtube.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
            duration = "4:28",
            durationSeconds = 268,
            category = "Bollywood",
            viewCount = 650000000L
        ),

        // Bangla
        Track(
            id = "w4ClQO03S60",
            youtubeVideoId = "w4ClQO03S60",
            title = "Deora (Coke Studio Bangla)",
            artist = "Pritom Hasan",
            channel = "Coke Studio Bangla",
            thumbnailUrl = "https://img.youtube.com/vi/w4ClQO03S60/hqdefault.jpg",
            duration = "4:07",
            durationSeconds = 247,
            category = "Bangla",
            viewCount = 110000000L
        ),
        Track(
            id = "oWdD2J37nC0",
            youtubeVideoId = "oWdD2J37nC0",
            title = "Bulbuli (Coke Studio Bangla)",
            artist = "Rituraj Sen & Nandita",
            channel = "Coke Studio Bangla",
            thumbnailUrl = "https://img.youtube.com/vi/oWdD2J37nC0/hqdefault.jpg",
            duration = "3:58",
            durationSeconds = 238,
            category = "Bangla",
            viewCount = 45000000L
        ),

        // Classical & Jazz
        Track(
            id = "GRxofEmo3HA",
            youtubeVideoId = "GRxofEmo3HA",
            title = "Für Elise",
            artist = "Ludwig van Beethoven",
            channel = "Classical Masters",
            thumbnailUrl = "https://img.youtube.com/vi/GRxofEmo3HA/hqdefault.jpg",
            duration = "2:56",
            durationSeconds = 176,
            category = "Classical",
            viewCount = 210000000L
        ),
        Track(
            id = "ylXk1LBvIqU",
            youtubeVideoId = "ylXk1LBvIqU",
            title = "So What",
            artist = "Miles Davis",
            channel = "Miles Davis Official",
            thumbnailUrl = "https://img.youtube.com/vi/ylXk1LBvIqU/hqdefault.jpg",
            duration = "9:22",
            durationSeconds = 562,
            category = "Jazz",
            viewCount = 35000000L
        ),

        // Workout & Party
        Track(
            id = "OPf0YbXqDm0",
            youtubeVideoId = "OPf0YbXqDm0",
            title = "Uptown Funk",
            artist = "Mark Ronson ft. Bruno Mars",
            channel = "Mark Ronson",
            thumbnailUrl = "https://img.youtube.com/vi/OPf0YbXqDm0/hqdefault.jpg",
            duration = "4:30",
            durationSeconds = 270,
            category = "Party",
            viewCount = 5000000000L
        ),
        Track(
            id = "btPJPFnesV4",
            youtubeVideoId = "btPJPFnesV4",
            title = "Eye of the Tiger",
            artist = "Survivor",
            channel = "Survivor Band",
            thumbnailUrl = "https://img.youtube.com/vi/btPJPFnesV4/hqdefault.jpg",
            duration = "4:05",
            durationSeconds = 245,
            category = "Workout",
            viewCount = 1100000000L
        ),

        // Romantic & Sad
        Track(
            id = "lp-EO5I60KA",
            youtubeVideoId = "lp-EO5I60KA",
            title = "Thinking Out Loud",
            artist = "Ed Sheeran",
            channel = "Ed Sheeran",
            thumbnailUrl = "https://img.youtube.com/vi/lp-EO5I60KA/hqdefault.jpg",
            duration = "4:56",
            durationSeconds = 296,
            category = "Romantic",
            viewCount = 3700000000L
        ),
        Track(
            id = "hLQl3WQQoQ0",
            youtubeVideoId = "hLQl3WQQoQ0",
            title = "Someone Like You",
            artist = "Adele",
            channel = "Adele",
            thumbnailUrl = "https://img.youtube.com/vi/hLQl3WQQoQ0/hqdefault.jpg",
            duration = "4:44",
            durationSeconds = 284,
            category = "Sad",
            viewCount = 2100000000L
        )
    )
}
