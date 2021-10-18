package com.example.firebasetutorial

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firestore.v1.FirestoreGrpc
import kotlinx.android.synthetic.main.activity_home.*
import java.lang.RuntimeException

enum class ProviderType{
    BASIC,
    GOOGLE,
    FACEBOOK

}
class HomeActivity : AppCompatActivity() {

    private val  db = FirebaseFirestore.getInstance()//Referencia como constante privada a nuestra base de datos
    //Instancia  a la base de datos que emos definido en remoto a travez de la consola de fire base

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //setup
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")


        //Remote Config (recuperar datos remotos)
        errorButton.visibility = View.INVISIBLE
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if(task.isSuccessful){
                val showErrorButton = Firebase.remoteConfig.getBoolean("show_error_button")
                val errorButtonText = Firebase.remoteConfig.getString("error_button_text")

                if (showErrorButton){
                    errorButton.visibility = View.VISIBLE
                }
                errorButton.text = errorButtonText
            }


        }

        //Guardado de datos
        val prefs = getSharedPreferences(getString(R.string.prefs_file),Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()


    }

    private fun setup(email: String, provider: String){
        title = "Inicio"
        emailTextView.text = email
        providerTextView.text = provider



        logOutButton.setOnClickListener {
            //Borrado de datos
            val prefs =getSharedPreferences(getString(R.string.prefs_file),Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()


            if (provider == ProviderType.FACEBOOK.name){
                LoginManager.getInstance().logOut()
            }

            FirebaseAuth.getInstance().signOut()
            onBackPressed()//para volver a la pantalla anterior

        }

        errorButton.setOnClickListener {


            // envio del id del usuario
            FirebaseCrashlytics.getInstance().setUserId(email)

            //envio  de claves custom
            FirebaseCrashlytics.getInstance().setCustomKey("provider", provider)

            //envio de log de errores(enviar log de contexto)
            FirebaseCrashlytics.getInstance().log("Se ha pulsado el boton de error")

            //Forzado de error
            //throw RuntimeException("Error forzado")




//            val crashButton = Button(this)
//            crashButton.text = "Test Crash"
//            crashButton.setOnClickListener {
//                throw RuntimeException("Test Crash") // Force a crash
//            }
//
//            addContentView(crashButton, ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        saveButton.setOnClickListener {

           FirebaseMessaging.getInstance().getToken().addOnCompleteListener {
              it.result//IMPORTANTE: ACA ESTOY OPTENIENDO EL identificador unico del dispositivo LO DEBO GUARDAR EN FIREBASE, PARA LUEGO PODER ENVIAR NOTIFICACIONES PERSONALIZADAS Y DEBIDAMENTE SEGMENTADAS
               println("####Este es el token: ${it.result}")
               

                db.collection("users").document(email).set(
                    hashMapOf("provider" to provider,
                        "address" to addressTextView.text.toString(),
                        "phone" to phoneTextView.text.toString(),
                        "tokenCel" to it.result.toString())

                )
           }

        }

        getButton.setOnClickListener {

            db.collection("users").document(email).get().addOnSuccessListener {

                addressTextView.setText(it.get("address") as String?)//it corresponde al documento(la clave del documento es el email) de nuestra coleccion
                phoneTextView.setText(it.get("phone") as String?)
            }



        }

        deleteButton.setOnClickListener {
            db.collection("users").document(email).delete()
        }
    }
}