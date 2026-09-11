package pt.goncalomferreira.quicknote

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)

        // Ajusta o conteúdo da Activity às barras do sistema preservando as margens da aplicação.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val basePaddingHorizontal = (20 * density).toInt()
            val basePaddingVertical = (16 * density).toInt()

            v.setPadding(
                systemBars.left + basePaddingHorizontal,
                systemBars.top + basePaddingVertical,
                systemBars.right + basePaddingHorizontal,
                systemBars.bottom + basePaddingVertical
            )

            insets
        }
    }
}