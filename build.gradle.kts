dependencies {
    // Media3 (ExoPlayer) - Yeh video player aur uski settings ke liye hai
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

    // OkHttp - Cloudflare Worker se connect karne ke liye
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Coroutines - Background me API call karne ke liye
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Default Android dependencies (pehle se honge)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
