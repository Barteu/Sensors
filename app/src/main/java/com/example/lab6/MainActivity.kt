package com.example.lab6

import android.animation.ObjectAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    private var fusedLocationProviderClient: FusedLocationProviderClient?= null
    private val MY_PERMISSION_FINE_LOCATION =101
    private var locationRequest : LocationRequest?= null
    private var updatesOn = false
    private var locationCallback : LocationCallback? = null

    //temperature

    private var temperature: Sensor? = null
    var temp = 22.0

    lateinit var  notificationChannel: NotificationChannel
    lateinit var builder : Notification.Builder
    private val channelId = "com.example.lab6"
    private val description = "Temp notification"


    //light
    lateinit var sensorManager: SensorManager
    private var light: Sensor?= null
    lateinit var textLight : TextView
    lateinit var layRect : FrameLayout

    //orientacja
    private val accelerometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val magnetometerReading = FloatArray(3)
    lateinit var textPos : TextView


    lateinit var textMagnet : TextView


    lateinit var obj_view : ImageView
    var obj_x :Int = 1
    var obj_y :Int = 0
    var lastAkc = arrayOf(0.0, 9.0, 0.0)

     var lastUpdate : Long =0
     var last_x : Float = 0f
     var last_y : Float = 0f
     var last_z : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btLocUpd = findViewById<ToggleButton>(R.id.tbLocUpdates)
        var image = 0

        obj_view = findViewById<ImageView>(R.id.ivPlanet)






        obj_view.setOnClickListener(){
            if(image==0)
            {
                obj_view.setImageResource(R.drawable.mars)
                image=1
            }
         else
            {
                obj_view.setImageResource(R.drawable.earth)
                image=0
            }

        }

        //########################################################### LOKALIZACJA
        val txtLat = findViewById<TextView>(R.id.textLatitude)
        val txtLon = findViewById<TextView>(R.id.textLongitude)

        locationRequest = LocationRequest()
        locationRequest!!.interval = 120
        locationRequest!!.fastestInterval = 60
        locationRequest!!.priority=LocationRequest.PRIORITY_HIGH_ACCURACY

        btLocUpd.setOnClickListener{
            if(btLocUpd.isChecked)
            {
                btLocUpd.text="Lokalizacja włączona"
                updatesOn = true
                startLocationUpdates()
            }
            else
            {
                btLocUpd.text="Lokalizacja wyłączona"
                updatesOn = false
                stopLocationUpdates()
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if(ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                )==PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationProviderClient!!.lastLocation.addOnSuccessListener { location ->
                if(location!=null){
                    //UPDATE UI
                    txtLat.text = location.latitude.toString()
                    txtLon.text = location.longitude.toString()
                }
            }
        }
        else
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestPermissions(
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSION_FINE_LOCATION
                )
            }
        }

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                for(location in p0!!.locations)
                {
                    //upd UI ;)
                    txtLat.text = location.latitude.toString()
                    txtLon.text = location.longitude.toString()
                }
            }
        }
        //###########  KONIEC  LOKALIZACJI ##################################


        //############## LIGHT SENSOR
        textLight = findViewById<TextView>(R.id.textLight)
        layRect = findViewById<FrameLayout>(R.id.layRect)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val thread = Thread(){
            run{
                light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            }
        }
        thread.run()
        // #### KONIEC LIGHT SENSOR #######

        //####### ORIENTACJA
        textPos = findViewById<TextView>(R.id.textPos)

        //magnetometer
        textMagnet = findViewById<TextView>(R.id.tvMagnet)

        // KONIEC ORIENTACJI ###


        //############temp sensor
        val thread_temp = Thread(){
            run{
                temperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            }
        }
        thread_temp.run()


        // koniec temp sensor ########



        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
            builder = Notification.Builder(this, channelId)
                    .setContentTitle("Temperatura")
                    .setContentText("Aktualna temperatura urządzenia:")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher_round))
                    .setContentIntent(pendingIntent)
        }
        else
        {
            builder = Notification.Builder(this)
                    .setContentTitle("Temperatura")
                    .setContentText("Aktualna temperatura urządzenia:")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher_round))
                    .setContentIntent(pendingIntent)
        }


        val thread_notif = Thread()
        {
            run{

                while(true)
                {


                    Thread.sleep(60000)
                    builder.setContentText("Aktualna temperatura urządzenia: " + temp.toString() + " C")
                    notificationManager.notify(1, builder.build())

                }

            }
        }
        thread_notif.start()
        //## KONIEC alarmManager ##########


    }


    override fun onResume() {
        super.onResume()
        if(updatesOn) {startLocationUpdates()}


        //orientacja;
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_GAME
            )
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                    this,
                    magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
            )
        }

        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, temperature, SensorManager.SENSOR_DELAY_NORMAL)



    }

    override fun onPause() {
        super.onPause()
        startLocationUpdates()

        //sensorManager.unregisterListener(this)
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            MY_PERMISSION_FINE_LOCATION ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted, nice ;-)
                } else {
                    Toast.makeText(
                            this,
                            "Ta aplikacja potrzebuje dostępu do lokalizacji",
                            Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
        }
    }

    private fun startLocationUpdates() {

        if(ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                )==PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationProviderClient!!.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
            )
        }
        else
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestPermissions(
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSION_FINE_LOCATION
                )
            }
        }

    }





    private fun stopLocationUpdates(){
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
    }


// #################### PONIZEJ SENSORY


    override fun onSensorChanged(event: SensorEvent) {

        if(event.sensor.getType() == Sensor.TYPE_LIGHT)
        {
            textLight.setText(event.values[0].toString() + " lm")

            var max = (if (light!=null) light?.getMaximumRange() else 40000 )

            var color_rev = 255 - (event.values[0].toInt()*255/ (max?.toInt() ?: 40000))

            if(color_rev>60 && color_rev<130)
            {
                color_rev=60
            }
            else if (color_rev>=130 && color_rev<200)
            {
                color_rev=200
            }

            var color_text = java.lang.Integer.toHexString(color_rev)
            var color_sub_text = if(color_text.length>1) color_text else ("0"+color_text)


            var color_bg =java.lang.Integer.toHexString(event.values[0].toInt() * 255 / (max?.toInt()
                    ?: 40000))
            var color_sub = if(color_bg.length>1) color_bg else ("0"+color_bg)

            var color_str = "#" +  color_sub+color_sub+color_sub
            var color_str_text = "#" +  color_sub_text+color_sub_text+color_sub_text
            layRect.setBackgroundColor(Color.parseColor(color_str))
            textLight.setTextColor(Color.parseColor(color_str_text))

        }
        else if(event.sensor.type == Sensor.TYPE_ACCELEROMETER)
        {



            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            textPos.setText(accelerometerReading[0].toString() + " | " + accelerometerReading[1].toString() + " | " + accelerometerReading[2].toString())


            val curTime = System.currentTimeMillis()
            // only allow one update every 100ms.

            if (curTime - lastUpdate > 100) {
                val diffTime: Long = curTime - lastUpdate
                lastUpdate = curTime
                var x = accelerometerReading[0]
                var y = accelerometerReading[1]
               var z = accelerometerReading[2]
                val speed: Float = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
                if (speed > 500) {
                    val rotate = AnimationUtils.loadAnimation(this,R.anim.rotate)
                    obj_view.startAnimation(rotate)

                }
                last_x = x
                last_y = y
                last_z = z
            }


            if(accelerometerReading[1].toDouble()>4.9 && obj_y==0)
            {
                ObjectAnimator.ofFloat(obj_view, "translationY", 300f).apply {
                    duration = 1000
                    start()
                    obj_y=1
                }
            }
            else if(accelerometerReading[1].toDouble()<4.9 && obj_y==1)
            {
                ObjectAnimator.ofFloat(obj_view, "translationY", 0f).apply {
                    duration = 1000
                    start()
                    obj_y=0
                }
            }
            if(accelerometerReading[0].toDouble()>4.9 && obj_x==1)
            {
                ObjectAnimator.ofFloat(obj_view, "translationX", 300f).apply {
                    duration = 1000
                    start()
                    obj_x=2
                }
            }
            else if(accelerometerReading[0].toDouble()<-4.9 && obj_x==1)
            {
                ObjectAnimator.ofFloat(obj_view, "translationX", -300f).apply {
                    duration = 1000
                    start()
                    obj_x=0
                }
            }
            else if(accelerometerReading[0].toDouble()<=4.9 && accelerometerReading[0].toDouble()>=-4.9  && obj_x!=1)
            {
                ObjectAnimator.ofFloat(obj_view, "translationX", 0f).apply {
                    duration = 1000
                    start()
                    obj_x=1
                }
            }


        }
        else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            textMagnet.setText(magnetometerReading[0].toString()+ " | "+magnetometerReading[1].toString()+ " | "+magnetometerReading[2].toString())
        }
        else if (event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            temp = event.values[0].toDouble()
        }


    }

    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
      ;
    }


}