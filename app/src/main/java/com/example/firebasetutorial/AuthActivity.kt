package com.example.firebasetutorial

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.PasswordAuthentication
import kotlinx.android.synthetic.main.activity_auth.*

import android.content.SharedPreferences
import android.view.ViewGroup
import android.widget.Toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import javax.security.auth.callback.Callback

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings


class AuthActivity : AppCompatActivity() {


    private val callBackManager = CallbackManager.Factory.create()
    private val GOOGLE_SIGN_IN = 100

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        //splash
        Thread.sleep(2000)
        setTheme(R.style.AppTheme)
        auth = Firebase.auth

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        //Analitics Event
        firebaseAnalytics = Firebase.analytics
        val bundle = Bundle()
        bundle.putString("message", "integracion de firebase Completa")
        firebaseAnalytics.logEvent("InitScreen", bundle )


        //remote config
        //configuraion principal de firebase remote config nosotros hemos definido valores en un servidor remoto en la nube pero lo recomendable es definir un valor por defecto para esos valores remotos en aso de que nuestra app no se pueda conectar a internet  otro parámetro de configuración muy importante es indicar a nuestra app cada cuánto tiempo va a recargar esos valores, por defecto se va a hacer cada 12 horas pero nosotros podremos bajar esos límites

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 60
        }
        val firebaseConfig = Firebase.remoteConfig
        firebaseConfig.setConfigSettingsAsync(configSettings)
        // otro valor de configuración que que podremos configurar son esos valores por defecto de nuestras variables remotas que por ejemplo podremos hacer llamando a set default de async pudiéndole pasar un xml o un mapa de valores por defecto en nuestro caso más simple lo vamos a hacer con un mapa
        firebaseConfig.setDefaultsAsync(mapOf("show_error_button" to false, "error_button_text" to "Forzar error" ))//Valores por defecto en caso de que tengamos problema recuperando los valores remotos


        //setup
        setup()
        sesion()

        identificador()
        notification()

    }

    override fun onStart(){ //esta operacion se invoca cada vez que se vuelve a mostrar la pantalla, y en este caso lo sobreescribimos para decirle que vuelva a ser visible
        super.onStart()
        authLayout.visibility = View.VISIBLE
    }

    private fun sesion(){// esta funcion sirve para saber si ya se ha iniciado sesion en la app
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)

        val email = prefs.getString("email", null )
        val provider = prefs.getString("provider", null)
        if(email != null && provider != null){
            authLayout.visibility = View.INVISIBLE// volver invisible el authLayout porque ya hay una sesion iniciada, TODO: se debe volver a mostrar al cerrrar la sesion (override onStart)
            showHome(email, ProviderType.valueOf(provider)) // el ProviderType se puede crear apartir de un string usando la funcion valueOf
        }

    }
    private fun setup(){

        title ="Autenticacion"

        signUpButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()){
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(emailEditText.text.toString(),
                        passwordEditText.text.toString()).addOnCompleteListener {
                        if (it.isSuccessful){
                            showHome(it.result?.user?.email ?: "", ProviderType.BASIC)

                        }
                        else{
                            showAlert()
                        }
                    }
            }
        }

        logInButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()){
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(emailEditText.text.toString(),
                        passwordEditText.text.toString()).addOnCompleteListener {
                        if (it.isSuccessful){
                            showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                        }
                        else{
                            showAlert()
                        }
                    }
            }
        }

        googleButton.setOnClickListener {

            //Configuracion Autenticacion Google
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)

        }

        facebookButton.setOnClickListener {

            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))

            LoginManager.getInstance().registerCallback(callBackManager,
            object : FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {

                    result?.let {
                        val token = it.accessToken

                        val credential = FacebookAuthProvider.getCredential(token.token)

                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                            if (it.isSuccessful){
                                showHome(it.result?.user?.email ?: "", ProviderType.FACEBOOK)
                            }
                            else{
                                showAlert()
                            }
                        }
                    }
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException?) {
                    showAlert()

                }
            })
        }

    }


    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("se ha producido un error autenticando al usuario")
        builder.setPositiveButton( "Aceptar", null )
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider : ProviderType){
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        callBackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == GOOGLE_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java )

                if (account != null) {

                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful){
                            showHome(account.email ?: "", ProviderType.GOOGLE)
                        }
                        else{
                            showAlert()
                        }
                    }
                }

            }catch (e: ApiException){
                showAlert()
            }

        }
    }

    private fun identificador(){

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener {
            it.result//IMPORTANTE: ACA ESTOY OPTENIENDO EL identificador unico del dispositivo
            //LO DEBO GUARDAR EN FIREBASE, PARA LUEGO PODER ENVIAR NOTIFICACIONES PERSONALIZADAS Y DEBIDAMENTE SEGMENTADAS

            println("####Este es el token: ${it.result}")
        }
    }


    private fun notification(){
        //Temas(Topics)
        FirebaseMessaging.getInstance().subscribeToTopic("tutorial")

        //intentar recuperar informacion, de notificacion push
       val url = intent.getStringExtra("url")
       url?.let{
            println("Ha llegado informacion en una notificacion push: ${it}")
       }
    }
}