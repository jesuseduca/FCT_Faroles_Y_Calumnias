package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.example.fct_faroles_y_calumnias_app.R

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        val btnCrearLobby = findViewById<Button>(R.id.btnCrearLobby)
        val btnUnirse = findViewById<Button>(R.id.btnUnirse)
        val btnPerfil = findViewById<Button>(R.id.btnPerfil)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController


        val esInvitado = intent.getBooleanExtra("es_invitado", false)
        val perfilId = intent.getStringExtra("perfil_id") ?: ""
        var nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        if (nombreUsuario.isEmpty() && esInvitado) {
            nombreUsuario = "Invitado_" + (1000..9999).random()
        }

        tvBienvenida.text = "¡Bienvenido, $nombreUsuario!"

        if (esInvitado) {
            btnPerfil.visibility = View.GONE
            btnCrearLobby.setTextColor(getColor(R.color.texto_secundario))
            btnCrearLobby.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(android.R.color.darker_gray))
            btnCrearLobby.setOnClickListener {
                Toast.makeText(this, "Regístrate para acceder a esta funcionalidad", Toast.LENGTH_SHORT).show()
            }
        } else {
            btnCrearLobby.setOnClickListener {
                val intent = Intent(this, CrearLobbyActivity::class.java)
                intent.putExtra("nombre_usuario", nombreUsuario)
                intent.putExtra("perfil_id", perfilId)
                startActivity(intent)
            }
        }

        btnUnirse.setOnClickListener {
            val intent = Intent(this, UnirseLobbyActivity::class.java)
            intent.putExtra("nombre_usuario", nombreUsuario)
            intent.putExtra("es_invitado", esInvitado)
            startActivity(intent)
        }

        btnPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("nombre_usuario", nombreUsuario)
            intent.putExtra("perfil_id", perfilId)
            startActivity(intent)
        }

        btnInfo.setOnClickListener {
            findViewById<FragmentContainerView>(R.id.nav_host_fragment).visibility = View.VISIBLE
            navController.navigate(R.id.infoFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.infoFragment) {
                findViewById<FragmentContainerView>(R.id.nav_host_fragment).visibility = View.GONE
            }
        }
    }
}