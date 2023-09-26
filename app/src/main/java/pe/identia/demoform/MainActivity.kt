package pe.identia.demoform

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import pe.identia.flow.FlowActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val baseUrl = "https://apifacialdev.identia.pe"
    private val httpClient = OkHttpClient()
    private lateinit var accessToken: String
    private lateinit var loadingFrameLayout: FrameLayout

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize FrameLayout for ProgressBar
        loadingFrameLayout = findViewById(R.id.loadingFrameLayout)

        val startProcessButton = findViewById<Button>(R.id.submitButton)
        startProcessButton.setOnClickListener {
            loadingFrameLayout.visibility = View.VISIBLE  // Show the FrameLayout with the ProgressBar
            issueAccessToken()
        }


        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val response = result.data?.getStringExtra("response")
                if (response != null) {
                    Log.d(TAG, "==========================")
                    Log.d(TAG, response)
                    Log.d(TAG, "==========================")
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun issueAccessToken() {
        val authUrl = "$baseUrl/api/auth/login"
        val requestBody = JSONObject().apply {
            put("clientname", "")
            put("username", "")
            put("password", "")
        }

        val request = Request.Builder()
            .url(authUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadingFrameLayout.visibility = View.GONE // Hide the FrameLayout with the ProgressBar
                    Toast.makeText(this@MainActivity, "Error issuing access token: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    accessToken = jsonResponse.getString("access_token")

                    Log.i(TAG, "Access token: $accessToken")
                    createIdProcess()
                } else {
                    runOnUiThread {
                        loadingFrameLayout.visibility = View.GONE // Hide the FrameLayout with the ProgressBar
                        Toast.makeText(this@MainActivity, "Failed to issue access token: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun createIdProcess() {
        val url = "$baseUrl/api/id_process"
        val docId = findViewById<EditText>(R.id.docIdEditText).text.toString()
        val registerPerson = findViewById<CheckBox>(R.id.registerPersonCheckBox).isChecked.toString()

        val formBody = FormBody.Builder()
            .add("doc_id", docId)
            .add("nat", "PER")
            .add("challenges", "[\"match:ocr:id\", \"liveness:selfie\", \"liveness:doc:back\"]")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Authorization", "jwt-bearer $accessToken")
            .put(formBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadingFrameLayout.visibility = View.GONE // Hide the FrameLayout with the ProgressBar
                    Toast.makeText(this@MainActivity, "Error creating ID process: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    Log.d(TAG, "ID process created successfully: $responseBody")
                    val jsonResponse = JSONObject(responseBody)
                    val idIntent = jsonResponse.getString("id")
                    Log.d(TAG, "ID: $idIntent")

                    val intent = Intent(this@MainActivity, FlowActivity::class.java)
                    intent.putExtra("idSession", idIntent);
                    intent.putExtra("endPoint", "https://apifacialdev.identia.pe/");


                    if (intent.resolveActivity(packageManager) != null) {
                        runOnUiThread {
                            loadingFrameLayout.visibility = View.GONE // Hide the FrameLayout with the ProgressBar
                        }
                        resultLauncher.launch(intent)
                    } else {
                        runOnUiThread {
                            loadingFrameLayout.visibility = View.GONE // Hide the FrameLayout with the ProgressBar
                            Toast.makeText(this@MainActivity, "No activity found to handle Intent", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        loadingFrameLayout.visibility = View.GONE // Hide the FrameLayout with the ProgressBar
                        Toast.makeText(this@MainActivity, "Failed to create ID process: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })


    }
}