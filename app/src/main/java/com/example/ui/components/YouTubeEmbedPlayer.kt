package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Lets the app control the embedded YouTube player (seek, play, pause, speed). */
class YouTubePlayerController {
    internal var webView: WebView? = null

    fun seekTo(seconds: Float) = exec("seekTo($seconds)")
    fun play() = exec("playV()")
    fun pause() = exec("pauseV()")
    fun setSpeed(rate: Float) = exec("setRate($rate)")

    private fun exec(js: String) {
        val w = webView ?: return
        w.post { w.evaluateJavascript(js, null) }
    }
}

/** Receives events from the YouTube IFrame player running inside the WebView. */
class YouTubeJsBridge(
    private val post: (() -> Unit) -> Unit,
    private val readyCb: () -> Unit,
    private val stateCb: (Int) -> Unit,
    private val timeCb: (Float, Float) -> Unit,
    private val errorCb: (Int) -> Unit
) {
    @JavascriptInterface
    fun onReady() = post { readyCb() }

    @JavascriptInterface
    fun onState(state: Int) = post { stateCb(state) }

    @JavascriptInterface
    fun onTime(current: Double, duration: Double) = post { timeCb(current.toFloat(), duration.toFloat()) }

    @JavascriptInterface
    fun onError(code: Int) = post { errorCb(code) }
}

private fun buildPlayerHtml(videoId: String, startSeconds: Int, loop: Boolean, controls: Boolean): String {
    val loopVars = if (loop) ",loop:1,playlist:'$videoId'" else ""
    val controlsValue = if (controls) 1 else 0
    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
#player{position:absolute;top:0;left:0;width:100%;height:100%}
</style>
</head>
<body>
<div id="player"></div>
<script>
var tag=document.createElement('script');
tag.src='https://www.youtube.com/iframe_api';
document.head.appendChild(tag);
var player=null;
function onYouTubeIframeAPIReady(){
  player=new YT.Player('player',{
    width:'100%',height:'100%',videoId:'$videoId',
    playerVars:{autoplay:1,controls:$controlsValue,playsinline:1,rel:0,modestbranding:1,fs:0,iv_load_policy:3,start:$startSeconds$loopVars,origin:'https://www.youtube.com'},
    events:{
      onReady:function(e){
        Android.onReady();
        setInterval(function(){
          if(player&&player.getCurrentTime){Android.onTime(player.getCurrentTime(),player.getDuration());}
        },500);
      },
      onStateChange:function(e){Android.onState(e.data);},
      onError:function(e){Android.onError(e.data);}
    }
  });
}
function seekTo(s){if(player)player.seekTo(s,true);}
function playV(){if(player)player.playVideo();}
function pauseV(){if(player)player.pauseVideo();}
function setRate(r){if(player)player.setPlaybackRate(r);}
</script>
</body>
</html>
"""
}

/**
 * Plays a real YouTube video using the official YouTube IFrame player inside a WebView.
 * State codes: 0 ended, 1 playing, 2 paused, 3 buffering.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    startSeconds: Int = 0,
    loop: Boolean = false,
    controls: Boolean = true,
    onControllerReady: (YouTubePlayerController) -> Unit = {},
    onStateChange: (Int) -> Unit = {},
    onTime: (Float, Float) -> Unit = { _, _ -> },
    onError: (Int) -> Unit = {}
) {
    val readyCallback by rememberUpdatedState(onControllerReady)
    val stateCallback by rememberUpdatedState(onStateChange)
    val timeCallback by rememberUpdatedState(onTime)
    val errorCallback by rememberUpdatedState(onError)

    key(videoId) {
        val controller = remember { YouTubePlayerController() }
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    controller.webView = this
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                    }
                    addJavascriptInterface(
                        YouTubeJsBridge(
                            post = { block -> post { block() } },
                            readyCb = { readyCallback(controller) },
                            stateCb = { state -> stateCallback(state) },
                            timeCb = { cur, dur -> timeCallback(cur, dur) },
                            errorCb = { code -> errorCallback(code) }
                        ),
                        "Android"
                    )
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        buildPlayerHtml(videoId, startSeconds, loop, controls),
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            onRelease = { view ->
                view.stopLoading()
                view.removeJavascriptInterface("Android")
                view.destroy()
            }
        )
    }
}
