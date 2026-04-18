package pub.hackers.android.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import pub.hackers.android.R

val UbuntuMonoFontFamily = FontFamily(
    Font(R.font.ubuntu_mono_regular, FontWeight.Normal),
    Font(R.font.ubuntu_mono_bold, FontWeight.Bold),
    Font(R.font.ubuntu_mono_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.ubuntu_mono_bold_italic, FontWeight.Bold, FontStyle.Italic),
)
