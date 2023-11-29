package com.assist.assist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() ,TextToSpeech.OnInitListener{
    private var tts: TextToSpeech? = null
    lateinit  var progress: ProgressBar
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private var isListening = false
    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta3/models/text-bison-001:generateText/"
    private val BASE_URL2 = "https://generativelanguage.googleapis.com/v1beta3/models/text-bison-001:generateText?key=AIzaSyDebzAKzyXVs215WFU90FF4PbFn04cgFvw"

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET
    )
    private val PERMISSIONS_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)

        if (!checkPermissions()) {
            requestPermissions()
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
         recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

        val startButton: Button = findViewById(R.id.startButton)
         progress   = findViewById(R.id.progress)
        startButton.setOnClickListener {
            toggleListening()
        }

        setupSpeechRecognizer()
    }


    private fun checkPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        isListening = true
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.e("TAG", "onReadyForSpeech: "+params )
            }

            override fun onBeginningOfSpeech() {
                Log.e("TAG", "onBeginningOfSpeech: ", )
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.e("TAG", "onRmsChanged: "+rmsdB.toString() )
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.e("TAG", "onBufferReceived: "+buffer.toString() )
            }

            override fun onEndOfSpeech() {
                Log.e("TAG", "onEndOfSpeech: ", )
            }

            override fun onError(error: Int) {
                Toast.makeText(applicationContext, "Error: $error", Toast.LENGTH_SHORT).show()
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                Log.e("TAG", "onResults: "+results.toString() )
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                Log.e("TAG", "onResults: "+matches.toString() )
                searchAndPlay(matches.toString())
                isListening = false

            }

               /* matches?.get(0)?.let { query ->{

                    Log.e("TAG", "queryqueryquery: "+query )
                        searchAndPlay(query)}
                        }
                isListening = false
            }*/

            override fun onPartialResults(partialResults: Bundle?) {
                Log.e("TAG", "onPartialResults: "+partialResults )

            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.e("TAG", "onEvent: "+params )

            }
        })
    }

    private fun searchAndPlay(query: String) {
        progress.visibility = View.VISIBLE
        Log.e("TAG", "searchAndPlay: query"+query )
        val httpClient = OkHttpClient.Builder()
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = httpClient.addInterceptor(interceptor)
            .connectTimeout(50, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
            try {
                val mediaType = "application/json".toMediaType()
                val body = "{ \"prompt\": { \"text\": \"$query\"} }".toRequestBody(mediaType)
                val call: Call<SearchResult> = apiService.search(BASE_URL2,body)
                call.enqueue(object : Callback<SearchResult> {
                    override fun onResponse(
                        call: Call<SearchResult>, response:
                        Response<SearchResult>
                    ) {
                        progress.visibility = View.GONE
                        if (response.isSuccessful) {

                            val searchResults = response.body()
                            if (searchResults != null) {
                                playAudio(searchResults.candidates[0].output)
                            } else {
                                showToast("No audio found for the query")
                            }
                        } else {
                            showToast("Request failed with code: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<SearchResult>, t: Throwable) {
                        progress.visibility = View.GONE
                        showToast("Error during API call: ${t.message}")
                    }
                })
            }catch (e:Exception){
                progress.visibility = View.GONE

                Log.e("TAG", "searchAndPlay: "+e.localizedMessage )
                Log.e("TAG", "searchAndPlay: "+e.message )
            //    showToast("Error during API call: ${e.message}")


        }
    }

    private fun showToast(s: String) {
        Toast.makeText(applicationContext,s,Toast.LENGTH_SHORT).show()

    }

    private fun playAudio(audioUrls: String?) {
          tts!!.speak(audioUrls, TextToSpeech.QUEUE_FLUSH, null,"")

    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            } else {
                Log.e("TTS","The Language!")

            }
        }
    }


}
