package app.innohub.geofence

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.*
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class GeofenceBroadcastReceiver  : BroadcastReceiver() {

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceList: List<Geofence>
    private lateinit var queue: RequestQueue

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val TAG = "location"
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "error")
            return
        }

        //if (queue == null) {
            queue = Volley.newRequestQueue(context)
        //}

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        val location = geofencingEvent.triggeringLocation;
        val locationString = "Lat: ${location.latitude} Long: ${location.longitude}"
        val url = "https://us-central1-fluid-outcome-206504.cloudfunctions.net/location"

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.i(TAG, geofenceTransition.toString())
                Log.i(TAG, locationString)
                val objReq = JsonObjectRequest(Request.Method.POST, url, JSONObject("""{ "lat": ${location.latitude}, "long": ${location.longitude}, "uuid": "KanitPhoneEnter" }"""), Response.Listener {
                    Log.i("location", "POSTED")
                }, Response.ErrorListener {
                    Log.e("location", "POST ERROR")
                })
                queue.add(objReq)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.i(TAG, geofenceTransition.toString())
                Log.i(TAG, locationString)
                val objReq = JsonObjectRequest(Request.Method.POST, url, JSONObject("""{ "lat": ${location.latitude}, "long": ${location.longitude}, "uuid": "KanitPhoneExit" }"""), Response.Listener {
                    Log.i("location", "POSTED")
                }, Response.ErrorListener {
                    Log.e("location", "POST ERROR")
                })
                queue.add(objReq)

                geofenceList = listOf(Geofence.Builder()
                        // Set the request ID of the geofence. This i s a string to identify this
                        // geofence.
                        .setRequestId("mobileBankingLocation")

                        // Set the circular region of this geofence.
                        .setCircularRegion(
                                location.latitude, location.longitude,
                                50.0f
                        )

                        // Set the expiration duration of the geofence. This geofence gets automatically
                        // removed after this period of time.
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)

                        // Set the transition types of interest. Alerts are only generated for these
                        // transition. We track entry and exit transitions in this sample.
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

                        // Create the geofence.
                        .build())

                if (context != null) {
                    geofencingClient = LocationServices.getGeofencingClient(context)
                    val geofencePendingIntent: PendingIntent by lazy {
                        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
                        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
                        // addGeofences() and removeGeofences().
                        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    }
                    try {
                        geofencingClient?.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
                            addOnSuccessListener {
                                // Geofences added
                                // ...
                                Log.i("location", "geofence added")
                            }
                            addOnFailureListener {
                                // Failed to add geofences
                                // ...
                                Log.i("location", "geofence adding failed")
                            }
                        }
                    } catch( ex: SecurityException ) {
                        Log.e("location", "error")
                    }
                }

            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.i(TAG, geofenceTransition.toString())
                Log.i(TAG, locationString)
                val objReq = JsonObjectRequest(Request.Method.POST, url, JSONObject("""{ "lat": ${location.latitude}, "long": ${location.longitude}, "uuid": "KanitPhoneDwell" }"""), Response.Listener {
                    Log.i("location", "POSTED")
                }, Response.ErrorListener {
                    Log.e("location", "POST ERROR")
                })

                queue.add(objReq)
            }
            else -> {
                Log.e(TAG, "error")
            }
        }
    }
}