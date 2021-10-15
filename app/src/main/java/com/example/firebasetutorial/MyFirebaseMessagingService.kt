package com.example.firebasetutorial

import android.os.Handler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import com.google.firebase.messaging.ktx.remoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService(){
//Las notificaciones por defecto siempre llegan en segundo plano
//es decir si nos llega una notificacion cuando la app esta abierta
//tenemos que manejarla nosotros desde codigo
// Esta clase nos sirve para controlar las notificaciones en primer plano

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

    Looper.prepare()
        Handler().post{

            Toast.makeText(baseContext, remoteMessage.notification?.title, Toast.LENGTH_LONG).show()
        }
        Looper.loop()
    }


}