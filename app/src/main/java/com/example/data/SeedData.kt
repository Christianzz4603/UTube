package com.example.data

object SeedData {
    // Public reliable MP4 streams from Google's official GTV sample bucket
    private const val STREAM_BIG_BUCK = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    private const val STREAM_ELEPHANTS = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
    private const val STREAM_BLAZES = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    private const val STREAM_ESCAPES = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
    private const val STREAM_FUN = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
    private const val STREAM_JOYRIDES = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"
    private const val STREAM_MELTDOWNS = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
    private const val STREAM_SINTEL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
    private const val STREAM_TEARS = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"

    val initialChannels = listOf(
        ChannelEntity(
            id = "ch_mkbhd_style",
            name = "Apex Tech Lab",
            handle = "@ApexTechLab",
            avatarColor = 0xFFE53935,
            bannerColor = 0xFF1A237E,
            subscriberCount = 19_400_000,
            videosCount = 1642,
            bio = "Crisp 8K tech reviews, smartphone camera deep dives, futuristic hardware prototypes, and honest engineering breakdowns.",
            isVerified = true,
            isSubscribed = true,
            notificationBellMode = "ALL"
        ),
        ChannelEntity(
            id = "ch_cyber_nexus",
            name = "Nexus Gaming & VFX",
            handle = "@NexusGamingVFX",
            avatarColor = 0xFF8E24AA,
            bannerColor = 0xFF311B92,
            subscriberCount = 8_750_000,
            videosCount = 890,
            bio = "Unreal Engine 5.5 showcases, ray-tracing benchmarks, speedruns, and open-world cyberpunk lore explorations.",
            isVerified = true,
            isSubscribed = true,
            notificationBellMode = "ALL"
        ),
        ChannelEntity(
            id = "ch_umami_craft",
            name = "Umami Kitchen Studio",
            handle = "@UmamiKitchen",
            avatarColor = 0xFFFB8C00,
            bannerColor = 0xFFBF360C,
            subscriberCount = 5_210_000,
            videosCount = 415,
            bio = "48-hour ramen broths, Michelin-technique street food at home, knife skills, and culinary science.",
            isVerified = true,
            isSubscribed = true,
            notificationBellMode = "PERSONALIZED"
        ),
        ChannelEntity(
            id = "ch_terra_cinema",
            name = "Terra 8K Expeditions",
            handle = "@Terra8K",
            avatarColor = 0xFF00897B,
            bannerColor = 0xFF004D40,
            subscriberCount = 12_900_000,
            videosCount = 310,
            bio = "Capturing Earth's wildest frontiers in Dolby Vision & 8K HDR. From Icelandic auroras to deep alpine glaciers.",
            isVerified = true,
            isSubscribed = false,
            notificationBellMode = "ALL"
        ),
        ChannelEntity(
            id = "ch_blender_open",
            name = "Blender Studio Official",
            handle = "@BlenderStudio",
            avatarColor = 0xFF1E88E5,
            bannerColor = 0xFF0D47A1,
            subscriberCount = 3_400_000,
            videosCount = 520,
            bio = "Open-source animated short films, VFX breakdowns, character rigging tutorials, and cinema production logs.",
            isVerified = true,
            isSubscribed = true,
            notificationBellMode = "ALL"
        ),
        ChannelEntity(
            id = "ch_synth_pulse",
            name = "Analog Synth Collective",
            handle = "@AnalogSynthWave",
            avatarColor = 0xFFD81B60,
            bannerColor = 0xFF880E4F,
            subscriberCount = 2_150_000,
            videosCount = 275,
            bio = "Late-night modular synthesizer sessions, lo-fi coding beats, and studio gear teardowns.",
            isVerified = true,
            isSubscribed = false,
            notificationBellMode = "NONE"
        )
    )

    val initialVideos = listOf(
        VideoEntity(
            id = "vid_quantum_phone",
            title = "The Transparent Glass Smartphone Is Actually Here! (Full Review)",
            description = "We spent 30 days testing the prototype photonic display smartphone, benchmarking its silicon-carbon battery, periscope telephoto sensor, and neural NPU workload.\n\n0:00 Intro & Unboxing\n0:04 Display & Brightness Test\n0:09 Camera Benchmarks\n0:12 Final Verdict\n\n#Tech #Smartphone #Review #Gadgets",
            videoUrl = STREAM_BLAZES,
            thumbnailResName = "img_thumb_tech_1791326396393",
            channelId = "ch_mkbhd_style",
            channelName = "Apex Tech Lab",
            channelHandle = "@ApexTechLab",
            channelAvatarColor = 0xFFE53935,
            subscriberCountText = "19.4M subscribers",
            viewsCount = 4_820_000,
            likesCount = 248_000,
            publishedTimeText = "5 hours ago",
            durationSeconds = 15,
            category = "Tech",
            isShort = false,
            isLiked = true,
            watchProgressFraction = 0.35f,
            lastWatchedTimestamp = System.currentTimeMillis() - 3_600_000L,
            chaptersJson = "0:00 Intro & Unboxing|0:04 Display & Brightness|0:09 Camera Lab|0:12 Final Verdict"
        ),
        VideoEntity(
            id = "vid_cyber_engine",
            title = "Unreal Engine 5.5 Cyberpunk Metropolis - Path Tracing at 120 FPS!",
            description = "Exploring a dense 64-square-kilometer cyberpunk metropolis rendered in real time with full path-traced reflections, volumetric rain, and dynamic crowd AI.\n\n0:00 Downtown Flyover\n0:05 Neon Market District\n0:10 GPU Frame-Time Analysis\n\n#Gaming #UnrealEngine #Cyberpunk #RTX",
            videoUrl = STREAM_ESCAPES,
            thumbnailResName = "img_thumb_gaming_1791326409204",
            channelId = "ch_cyber_nexus",
            channelName = "Nexus Gaming & VFX",
            channelHandle = "@NexusGamingVFX",
            channelAvatarColor = 0xFF8E24AA,
            subscriberCountText = "8.75M subscribers",
            viewsCount = 2_910_000,
            likesCount = 189_000,
            publishedTimeText = "1 day ago",
            durationSeconds = 15,
            category = "Gaming",
            isShort = false,
            isSavedToWatchLater = true,
            watchProgressFraction = 0.7f,
            lastWatchedTimestamp = System.currentTimeMillis() - 86_400_000L,
            chaptersJson = "0:00 Downtown Flyover|0:05 Neon Market District|0:10 GPU Frame-Time Analysis"
        ),
        VideoEntity(
            id = "vid_ramen_masterclass",
            title = "48-Hour Michelin Tonkotsu Ramen From Scratch (Ultimate Guide)",
            description = "Every step required to craft rich, emulsified 48-hour pork bone Tonkotsu broth, house-made alkaline noodles, 6-minute ajitama soft-boiled eggs, and smoky chashu.\n\n0:00 Broth Emulsification\n0:05 Tare & Aroma Oil\n0:10 Noodle Pull & Plating\n\n#Cooking #Ramen #Foodie #Chef",
            videoUrl = STREAM_JOYRIDES,
            thumbnailResName = "img_thumb_cooking_1791326420737",
            channelId = "ch_umami_craft",
            channelName = "Umami Kitchen Studio",
            channelHandle = "@UmamiKitchen",
            channelAvatarColor = 0xFFFB8C00,
            subscriberCountText = "5.21M subscribers",
            viewsCount = 6_450_000,
            likesCount = 412_000,
            publishedTimeText = "3 days ago",
            durationSeconds = 15,
            category = "Cooking",
            isShort = false,
            isLiked = true,
            isDownloaded = true,
            chaptersJson = "0:00 Broth Emulsification|0:05 Tare & Aroma Oil|0:10 Noodle Pull & Plating"
        ),
        VideoEntity(
            id = "vid_iceland_aurora",
            title = "ICELAND IN 8K 60FPS: Under the Solar Maximum Aurora Borealis",
            description = "Shot over 6 winter expeditions across the Icelandic highlands during the peak solar cycle. Experience volcanic canyons, glacial lagoons, and dancing emerald curtains.\n\n0:00 Highland Fjords\n0:05 Glacial River Delta\n0:10 Solar Storm Finale\n\n#Nature #8K #Iceland #Documentary",
            videoUrl = STREAM_FUN,
            thumbnailResName = "img_thumb_nature_1791326431475",
            channelId = "ch_terra_cinema",
            channelName = "Terra 8K Expeditions",
            channelHandle = "@Terra8K",
            channelAvatarColor = 0xFF00897B,
            subscriberCountText = "12.9M subscribers",
            viewsCount = 14_300_000,
            likesCount = 820_000,
            publishedTimeText = "2 weeks ago",
            durationSeconds = 60,
            category = "Nature",
            isShort = false,
            isSavedToWatchLater = true,
            chaptersJson = "0:00 Highland Fjords|0:20 Glacial River Delta|0:42 Solar Storm Finale"
        ),
        VideoEntity(
            id = "vid_big_buck_bunny",
            title = "Big Buck Bunny - Full 4K Remastered Open Movie (Blender Studio)",
            description = "Watch the legendary open-source animated short film Big Buck Bunny created with Blender. Follow a giant warm-hearted rabbit as he outsmarts three woodland bullies.\n\n0:00 Morning Meadow\n1:45 The Butterfly Encounter\n4:10 Buck's Master Plan\n7:30 Woodland Showdown\n\n#Animation #Blender #Film #Cinema",
            videoUrl = STREAM_BIG_BUCK,
            thumbnailResName = "img_thumb_nature_1791326431475",
            channelId = "ch_blender_open",
            channelName = "Blender Studio Official",
            channelHandle = "@BlenderStudio",
            channelAvatarColor = 0xFF1E88E5,
            subscriberCountText = "3.4M subscribers",
            viewsCount = 28_900_000,
            likesCount = 1_450_000,
            publishedTimeText = "1 month ago",
            durationSeconds = 596,
            category = "Movies",
            isShort = false,
            isDownloaded = true,
            chaptersJson = "0:00 Morning Meadow|1:45 The Butterfly Encounter|4:10 Buck's Master Plan|7:30 Woodland Showdown"
        ),
        VideoEntity(
            id = "vid_sintel_film",
            title = "Sintel - Award-Winning Fantasy Short Film | Official 4K Presentation",
            description = "A lonely young warrior named Sintel searches the frigid peaks and ancient ruins for Scales, a baby dragon she befriended and nursed back to health.\n\n0:00 The Mountain Gate\n2:30 Finding Scales\n6:15 The Desert City\n11:00 Dragon's Lair\n\n#Fantasy #Movies #Blender #Animation",
            videoUrl = STREAM_SINTEL,
            thumbnailResName = "img_thumb_gaming_1791326409204",
            channelId = "ch_blender_open",
            channelName = "Blender Studio Official",
            channelHandle = "@BlenderStudio",
            channelAvatarColor = 0xFF1E88E5,
            subscriberCountText = "3.4M subscribers",
            viewsCount = 19_200_000,
            likesCount = 980_000,
            publishedTimeText = "3 months ago",
            durationSeconds = 888,
            category = "Movies",
            isShort = false,
            chaptersJson = "0:00 The Mountain Gate|2:30 Finding Scales|6:15 The Desert City|11:00 Dragon's Lair"
        ),
        VideoEntity(
            id = "vid_tears_of_steel",
            title = "Tears of Steel - Sci-Fi VFX Breakdown & Full Cyberpunk Short Film",
            description = "Set in a futuristic Amsterdam, a group of resistance warriors and scientists gather at the Oude Kerk to stage a desperate attempt to save the world from destructive robots.\n\n0:00 Bridge Confrontation\n3:10 Lab Briefing\n7:45 Mecha Assault\n10:20 Final Sequence\n\n#SciFi #Tech #VFX #Movies",
            videoUrl = STREAM_TEARS,
            thumbnailResName = "img_thumb_tech_1791326396393",
            channelId = "ch_mkbhd_style",
            channelName = "Apex Tech Lab",
            channelHandle = "@ApexTechLab",
            channelAvatarColor = 0xFFE53935,
            subscriberCountText = "19.4M subscribers",
            viewsCount = 7_600_000,
            likesCount = 410_000,
            publishedTimeText = "4 days ago",
            durationSeconds = 734,
            category = "Tech",
            isShort = false,
            chaptersJson = "0:00 Bridge Confrontation|3:10 Lab Briefing|7:45 Mecha Assault|10:20 Final Sequence"
        ),
        VideoEntity(
            id = "vid_synth_live_session",
            title = "LIVE: 3AM Modular Synthwave & Deep Focus Coding Beats",
            description = "Analog Prophet-6, Moog Subharmonicon, and granular tape loops recorded live in the studio. Perfect for late-night engineering, architecture design, and deep focus.\n\n0:00 Sub-Bass Warmup\n0:05 Arpeggio Horizon\n0:10 Midnight Overdrive\n\n#Music #Synthwave #Coding #Live",
            videoUrl = STREAM_MELTDOWNS,
            thumbnailResName = "img_thumb_tech_1791326396393",
            channelId = "ch_synth_pulse",
            channelName = "Analog Synth Collective",
            channelHandle = "@AnalogSynthWave",
            channelAvatarColor = 0xFFD81B60,
            subscriberCountText = "2.15M subscribers",
            viewsCount = 1_340_000,
            likesCount = 94_000,
            publishedTimeText = "Streamed 9 hours ago",
            durationSeconds = 15,
            category = "Music",
            isShort = false,
            isLive = true,
            chaptersJson = "0:00 Sub-Bass Warmup|0:05 Arpeggio Horizon|0:10 Midnight Overdrive"
        ),
        VideoEntity(
            id = "vid_elephants_dream",
            title = "Elephants Dream - Inside the Infinite Surreal Clockwork Machine",
            description = "Proog and Emo journey deep inside a colossal, self-constructing surrealist machine where thoughts and memories manifest as physical architecture.\n\n0:00 The Wire Room\n3:00 Typewriter Abyss\n6:20 Elevator Freefall\n\n#Art #Animation #Gaming #Design",
            videoUrl = STREAM_ELEPHANTS,
            thumbnailResName = "img_thumb_gaming_1791326409204",
            channelId = "ch_cyber_nexus",
            channelName = "Nexus Gaming & VFX",
            channelHandle = "@NexusGamingVFX",
            channelAvatarColor = 0xFF8E24AA,
            subscriberCountText = "8.75M subscribers",
            viewsCount = 5_120_000,
            likesCount = 310_000,
            publishedTimeText = "1 week ago",
            durationSeconds = 653,
            category = "Gaming",
            isShort = false,
            chaptersJson = "0:00 The Wire Room|3:00 Typewriter Abyss|6:20 Elevator Freefall"
        ),

        // SHORTS (Vertical full-screen feed items with real video streams)
        VideoEntity(
            id = "short_desk_tour",
            title = "Wait for the hidden motorized monitor lift in this 2026 desk setup! 🔥 #Shorts #Tech #Setup",
            description = "Custom walnut desk with integrated MagSafe pads, hidden cable trough, and dual OLED monitors.",
            videoUrl = STREAM_BLAZES,
            thumbnailResName = "img_thumb_tech_1791326396393",
            channelId = "ch_mkbhd_style",
            channelName = "Apex Tech Lab",
            channelHandle = "@ApexTechLab",
            channelAvatarColor = 0xFFE53935,
            subscriberCountText = "19.4M subscribers",
            viewsCount = 9_400_000,
            likesCount = 680_000,
            publishedTimeText = "6 hours ago",
            durationSeconds = 15,
            category = "Tech",
            isShort = true,
            isLiked = true,
            audioTrackTitle = "Original Sound - Apex Tech Lab"
        ),
        VideoEntity(
            id = "short_garlic_chili_oil",
            title = "Sizzling 400°F Chili Crisp Oil over fresh hand-pulled noodles 🌶️🍜 #Shorts #Cooking #Food",
            description = "Nothing beats the sound of hot toasted sesame oil hitting crushed Sichuan peppercorns and garlic.",
            videoUrl = STREAM_JOYRIDES,
            thumbnailResName = "img_thumb_cooking_1791326420737",
            channelId = "ch_umami_craft",
            channelName = "Umami Kitchen Studio",
            channelHandle = "@UmamiKitchen",
            channelAvatarColor = 0xFFFB8C00,
            subscriberCountText = "5.21M subscribers",
            viewsCount = 15_800_000,
            likesCount = 1_240_000,
            publishedTimeText = "1 day ago",
            durationSeconds = 15,
            category = "Cooking",
            isShort = true,
            audioTrackTitle = "Midnight Lo-Fi Kitchen - Umami Beats"
        ),
        VideoEntity(
            id = "short_cyber_drift",
            title = "When you turn on Full Path Tracing at 8K resolution... 🤯🎮 #Shorts #Gaming #RTX",
            description = "Every puddle reflects neon holograms in real-time without screen-space artifacts.",
            videoUrl = STREAM_ESCAPES,
            thumbnailResName = "img_thumb_gaming_1791326409204",
            channelId = "ch_cyber_nexus",
            channelName = "Nexus Gaming & VFX",
            channelHandle = "@NexusGamingVFX",
            channelAvatarColor = 0xFF8E24AA,
            subscriberCountText = "8.75M subscribers",
            viewsCount = 6_200_000,
            likesCount = 520_000,
            publishedTimeText = "2 days ago",
            durationSeconds = 15,
            category = "Gaming",
            isShort = true,
            audioTrackTitle = "Cyberpunk Hyperdrive - Synth Collective"
        ),
        VideoEntity(
            id = "short_glacier_dive",
            title = "FPV Drone diving down a 300-meter Icelandic waterfall into an aurora! 🌌🇮🇸 #Shorts #Nature",
            description = "One-take custom cinewhoop flight over Skógafoss under KP-7 geomagnetic activity.",
            videoUrl = STREAM_MELTDOWNS,
            thumbnailResName = "img_thumb_nature_1791326431475",
            channelId = "ch_terra_cinema",
            channelName = "Terra 8K Expeditions",
            channelHandle = "@Terra8K",
            channelAvatarColor = 0xFF00897B,
            subscriberCountText = "12.9M subscribers",
            viewsCount = 21_500_000,
            likesCount = 1_910_000,
            publishedTimeText = "4 days ago",
            durationSeconds = 15,
            category = "Nature",
            isShort = true,
            audioTrackTitle = "Northern Lights Horizon - Terra Audio"
        )
    )

    val initialComments = listOf(
        CommentEntity(
            videoId = "vid_quantum_phone",
            authorName = "Apex Tech Lab",
            authorHandle = "@ApexTechLab",
            authorAvatarColor = 0xFFE53935,
            text = "Pinned: Which feature matters more to you in 2026 — a 7,500mAh silicon-carbon battery or under-display periscope optics? Let us know below! 👇",
            timestampText = "5 hours ago",
            likesCount = 14200,
            isHeartedByCreator = true,
            isPinned = true,
            replyCount = 412
        ),
        CommentEntity(
            videoId = "vid_quantum_phone",
            authorName = "Marcus Chen",
            authorHandle = "@marcus_builds",
            authorAvatarColor = 0xFF1E88E5,
            text = "The camera transition at 0:09 is legitimately cinema-grade. Props to the editing team for always raising the bar.",
            timestampText = "4 hours ago",
            likesCount = 3840,
            isHeartedByCreator = true,
            isPinned = false,
            replyCount = 29
        ),
        CommentEntity(
            videoId = "vid_quantum_phone",
            authorName = "Elena Vance",
            authorHandle = "@elenavance_dev",
            authorAvatarColor = 0xFF43A047,
            text = "Battery life every single time! Once you get used to 2 full days without carrying a power bank, you can never go back.",
            timestampText = "3 hours ago",
            likesCount = 1290,
            isHeartedByCreator = false,
            isPinned = false,
            replyCount = 14
        ),
        CommentEntity(
            videoId = "vid_cyber_engine",
            authorName = "Kaelen VFX",
            authorHandle = "@kaelenvfx",
            authorAvatarColor = 0xFF8E24AA,
            text = "The wet asphalt reflections in the Neon Market district at 0:05 look indistinguishable from live-action 70mm film.",
            timestampText = "19 hours ago",
            likesCount = 5620,
            isHeartedByCreator = true,
            isPinned = true,
            replyCount = 87
        ),
        CommentEntity(
            videoId = "vid_ramen_masterclass",
            authorName = "Chef Kenji Sato",
            authorHandle = "@kenjisato_culinary",
            authorAvatarColor = 0xFFFB8C00,
            text = "Pro tip: soaking the kombu at 60°C for 45 minutes before removing it prevents any bitterness in the dashi tare. Incredible video!",
            timestampText = "2 days ago",
            likesCount = 9410,
            isHeartedByCreator = true,
            isPinned = true,
            replyCount = 134
        ),
        CommentEntity(
            videoId = "vid_iceland_aurora",
            authorName = "Aria Lindqvist",
            authorHandle = "@arialindqvist",
            authorAvatarColor = 0xFF00897B,
            text = "Watching this on an OLED screen in a dark room with Ambient Mode turned on is an unreal experience.",
            timestampText = "1 week ago",
            likesCount = 11200,
            isHeartedByCreator = true,
            isPinned = false,
            replyCount = 63
        ),
        CommentEntity(
            videoId = "short_desk_tour",
            authorName = "Devon Hardware",
            authorHandle = "@devon_hw",
            authorAvatarColor = 0xFF3949AB,
            text = "That zero-cable magnetic raceway underneath the desk is pure engineering perfection.",
            timestampText = "4 hours ago",
            likesCount = 8240,
            isHeartedByCreator = true,
            isPinned = true,
            replyCount = 52
        )
    )

    val initialPlaylists = listOf(
        PlaylistEntity(
            title = "8K Cinema & Visual Benchmarks",
            description = "Reference HDR & 8K videos for testing displays, speakers, and ambient lighting.",
            privacy = "Public",
            videoIdsCsv = "vid_quantum_phone,vid_iceland_aurora,vid_cyber_engine,vid_tears_of_steel"
        ),
        PlaylistEntity(
            title = "Weekend Culinary Projects",
            description = "Deep-dive recipes and fermentation experiments to cook on Sunday afternoons.",
            privacy = "Public",
            videoIdsCsv = "vid_ramen_masterclass"
        ),
        PlaylistEntity(
            title = "Blender Open Movie Anthology",
            description = "Complete collection of open-source animated short films.",
            privacy = "Unlisted",
            videoIdsCsv = "vid_big_buck_bunny,vid_sintel_film,vid_elephants_dream"
        )
    )

    val initialCommunityPosts = listOf(
        CommunityPostEntity(
            channelId = "ch_mkbhd_style",
            channelName = "Apex Tech Lab",
            channelHandle = "@ApexTechLab",
            channelAvatarColor = 0xFFE53935,
            timestampText = "3 hours ago",
            contentText = "We're filming our annual Blind Smartphone Camera Bracket tomorrow! 16 phones, 4 lighting scenarios. Which category should carry the highest weight in the final score?",
            pollOptionsPipe = "Low-Light Night Portraits|5x Telephoto Zoom Clarity|HDR Video Stabilization|Natural Skin Tone Science",
            pollVotesPipe = "18420|14310|9850|24600",
            selectedPollIndex = -1,
            likesCount = 34200,
            commentsCount = 1840
        ),
        CommunityPostEntity(
            channelId = "ch_umami_craft",
            channelName = "Umami Kitchen Studio",
            channelHandle = "@UmamiKitchen",
            channelAvatarColor = 0xFFFB8C00,
            timestampText = "1 day ago",
            contentText = "Batch #14 of our 90-day fermented black garlic miso just finished aging in cedar barrels. Which episode should we drop this Friday?",
            pollOptionsPipe = "Black Garlic Wagyu Mazemen|Crispy Chili Oil Dumplings|36-Hour Croissant Lamination",
            pollVotesPipe = "19200|22400|11300",
            selectedPollIndex = 1,
            likesCount = 19800,
            commentsCount = 920
        ),
        CommunityPostEntity(
            channelId = "ch_cyber_nexus",
            channelName = "Nexus Gaming & VFX",
            channelHandle = "@NexusGamingVFX",
            channelAvatarColor = 0xFF8E24AA,
            timestampText = "2 days ago",
            contentText = "Just compiled our custom path-traced shader pack for the Cyberpunk Metropolis demo! Drop your GPU model in the comments so we can include your settings preset in the benchmark table.",
            pollOptionsPipe = "",
            pollVotesPipe = "",
            selectedPollIndex = -1,
            likesCount = 12400,
            commentsCount = 2310
        )
    )

    val initialSearchHistory = listOf(
        SearchQueryEntity("8k hdr oled test", System.currentTimeMillis() - 10000),
        SearchQueryEntity("tonkotsu ramen recipe", System.currentTimeMillis() - 20000),
        SearchQueryEntity("unreal engine 5.5 cyberpunk", System.currentTimeMillis() - 30000),
        SearchQueryEntity("transparent smartphone review", System.currentTimeMillis() - 40000),
        SearchQueryEntity("modular synthwave live", System.currentTimeMillis() - 50000),
        SearchQueryEntity("big buck bunny 4k", System.currentTimeMillis() - 60000)
    )

    val initialNotifications = listOf(
        NotificationEntity(
            channelName = "Apex Tech Lab",
            channelAvatarColor = 0xFFE53935,
            title = "Uploaded: The Transparent Glass Smartphone Is Actually Here! (Full Review)",
            timeAgoText = "5 hours ago",
            videoId = "vid_quantum_phone",
            thumbnailResName = "img_thumb_tech_1791326396393",
            isRead = false
        ),
        NotificationEntity(
            channelName = "Analog Synth Collective",
            channelAvatarColor = 0xFFD81B60,
            title = "🔴 LIVE: 3AM Modular Synthwave & Deep Focus Coding Beats",
            timeAgoText = "9 hours ago",
            videoId = "vid_synth_live_session",
            thumbnailResName = "img_thumb_tech_1791326396393",
            isRead = false
        ),
        NotificationEntity(
            channelName = "Nexus Gaming & VFX",
            channelAvatarColor = 0xFF8E24AA,
            title = "Uploaded: Unreal Engine 5.5 Cyberpunk Metropolis - Path Tracing at 120 FPS!",
            timeAgoText = "1 day ago",
            videoId = "vid_cyber_engine",
            thumbnailResName = "img_thumb_gaming_1791326409204",
            isRead = true
        ),
        NotificationEntity(
            channelName = "Umami Kitchen Studio",
            channelAvatarColor = 0xFFFB8C00,
            title = "Uploaded: 48-Hour Michelin Tonkotsu Ramen From Scratch (Ultimate Guide)",
            timeAgoText = "3 days ago",
            videoId = "vid_ramen_masterclass",
            thumbnailResName = "img_thumb_cooking_1791326420737",
            isRead = true
        )
    )
}
