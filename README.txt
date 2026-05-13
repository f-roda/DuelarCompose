Duelar Branding Patch

Copiá la carpeta app/src/main/res sobre tu proyecto.

AndroidManifest.xml:
Agregá estas líneas en <application>:

android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
android:label="Duelar"

Logo dentro de la app:
En MainActivity.kt agregá imports:

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

Agregá este composable dentro de MainActivity:

@Composable
private fun DuelarLogoHeader() {
    Image(
        painter = painterResource(id = R.drawable.logo_duelar),
        contentDescription = "Duelar",
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentScale = ContentScale.Fit
    )
}

Luego en WelcomeScreen, antes de Title("Duelar"), agregá:
DuelarLogoHeader()

Opcional: comentá Title("Duelar") para que el texto no quede duplicado.
