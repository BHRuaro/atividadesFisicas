 package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var btnGoogle: Button
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "LoginActivity"
        private const val WEB_CLIENT_ID =
            "449772724679-subi61dcu9evv89qoi3pvn1sp5r23bqa.apps.googleusercontent.com"
    }

    private fun redirectToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        firestore = Firebase.firestore

        auth.currentUser?.let {
            redirectToMain()
            return
        }

        btnGoogle = findViewById(R.id.btnGoogle)
        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption as CredentialOption).build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity,
                )
                val credential = result.credential

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Erro ao obter credencial: ${e.message}")
                Toast.makeText(
                    this@LoginActivity,
                    "Erro no login: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "signInWithCredential:sucess")

                val user = auth.currentUser
                user?.let {
                    criarPerfilUsuario(it)
                    redirectToMain()

                }


                Toast.makeText(this@LoginActivity, "Login bem-sucedido", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.exception)
                Toast.makeText(this@LoginActivity, "Falha na autenticação", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }

    private fun LoginActivity.criarPerfilUsuario(user: FirebaseUser) {
        val docRef = firestore.collection("usuarios").document(user.uid)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "Perfil de usuário já existe, não será recriado.")
                } else {
                    val perfilUsuario = PerfilUsuario(
                        uid = user.uid,
                        nome = user.displayName ?: "",
                        email = user.email ?: "",
                        pontuacao = 0,
                        dataCadastro = Timestamp.now()
                    )

                    docRef.set(perfilUsuario)
                        .addOnSuccessListener {
                            Log.d(TAG, "Perfil de usuário criado com sucesso")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Erro ao criar perfil de usuário", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao verificar existência do perfil", exception)
            }
    }

}
