package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ImageCodec
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.ui.components.Base64ImageBox
import com.example.ui.components.SectionHeader
import com.example.ui.components.parseSafeColor
import com.example.ui.theme.DeepNightBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleGold
import com.example.ui.theme.Turquoise
import com.example.viewmodel.SchoolViewModel

@Composable
fun SchoolBrandingScreen(
    school: School,
    schoolViewModel: SchoolViewModel
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var name by remember(school.id) { mutableStateOf(school.name) }
    var code by remember(school.id) { mutableStateOf(school.code) }
    var schoolType by remember(school.id) { mutableStateOf(school.schoolType) }
    var motto by remember(school.id) { mutableStateOf(school.motto) }
    var city by remember(school.id) { mutableStateOf(school.city) }
    var address by remember(school.id) { mutableStateOf(school.address) }
    var phone by remember(school.id) { mutableStateOf(school.phone) }
    var email by remember(school.id) { mutableStateOf(school.email) }
    var directorName by remember(school.id) { mutableStateOf(school.directorName) }
    var regNumber by remember(school.id) { mutableStateOf(school.registrationNumber) }
    var primaryHex by remember(school.id) { mutableStateOf(school.primaryColorHex) }
    var secondaryHex by remember(school.id) { mutableStateOf(school.secondaryColorHex) }
    var accentHex by remember(school.id) { mutableStateOf(school.accentColorHex) }
    var logoPrimary by remember(school.id) { mutableStateOf(school.logoPrimaryBase64) }
    var logoSecondary by remember(school.id) { mutableStateOf(school.logoSecondaryBase64) }
    var stamp by remember(school.id) { mutableStateOf(school.stampBase64) }
    var signature by remember(school.id) { mutableStateOf(school.signatureBase64) }
    var supportAccess by remember(school.id) { mutableStateOf(school.supportAccessEnabled) }

    var activePickerTarget by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val encoded = ImageCodec.encodeUriToBase64(context, uri, 220) ?: ""
                if (encoded.isNotBlank()) {
                    when (activePickerTarget) {
                        "PRIMARY_LOGO" -> logoPrimary = encoded
                        "SECONDARY_LOGO" -> logoSecondary = encoded
                        "STAMP" -> stamp = encoded
                        "SIGNATURE" -> signature = encoded
                    }
                }
            }
        }
    )

    val previewPrimary = parseSafeColor(primaryHex, DeepNightBlue)
    val previewSecondary = parseSafeColor(secondaryHex, RoyalBlue)
    val previewAccent = parseSafeColor(accentHex, Turquoise)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = strings.previewBeforeSave,
                    subtitle = "Aperçu en direct de l'en-tête officiel et de la page de connexion personnalisée de votre école"
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = previewPrimary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Base64ImageBox(logoPrimary, strings.primaryLogo, Modifier.size(56.dp), Icons.Default.Badge)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name.ifBlank { "NOM ÉTABLISSEMENT" }, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                if (motto.isNotBlank()) {
                                    Text("« $motto »", style = MaterialTheme.typography.bodySmall, color = previewAccent)
                                }
                                Text("$city • $phone • $regNumber", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            }
                            Base64ImageBox(logoSecondary, strings.secondaryLogo, Modifier.size(44.dp), Icons.Default.Image)
                        }
                        Spacer(Modifier.height(12.dp))
                        // Branded school login preview card
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Portail de connexion : ${code.uppercase()}", style = MaterialTheme.typography.labelLarge, color = previewSecondary)
                                    Text("Propulsé par ADRIÈL SCHOOL MANAGER • ADRIÈL TECH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.Lock, contentDescription = null, tint = previewAccent)
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Visuels Officiels (Photo Picker sécurisé)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AssetPickerTile(strings.primaryLogo, logoPrimary, Icons.Default.Badge, Modifier.weight(1f)) {
                                activePickerTarget = "PRIMARY_LOGO"
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            AssetPickerTile(strings.secondaryLogo, logoSecondary, Icons.Default.Image, Modifier.weight(1f)) {
                                activePickerTarget = "SECONDARY_LOGO"
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AssetPickerTile(strings.officialStamp, stamp, Icons.Default.Verified, Modifier.weight(1f)) {
                                activePickerTarget = "STAMP"
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            AssetPickerTile(strings.directorSignature, signature, Icons.Default.Draw, Modifier.weight(1f)) {
                                activePickerTarget = "SIGNATURE"
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Couleurs Institutionnelles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val presets = listOf(
                            Triple("#0B1528", "#1E40AF", "#0D9488"),
                            Triple("#1E293B", "#0F766E", "#D4AF37"),
                            Triple("#31102F", "#7E22CE", "#0EA5E9"),
                            Triple("#14532D", "#15803D", "#CA8A04")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            presets.forEach { (p, s, a) ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(parseSafeColor(p, DeepNightBlue))
                                        .border(2.dp, SubtleGold, CircleShape)
                                        .clickable { primaryHex = p; secondaryHex = s; accentHex = a }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = primaryHex, onValueChange = { primaryHex = it }, label = { Text("Primaire") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = secondaryHex, onValueChange = { secondaryHex = it }, label = { Text("Secondaire") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = accentHex, onValueChange = { accentHex = it }, label = { Text("Accent") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Coordonnées & Informations Administratives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom de l'établissement") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = schoolType, onValueChange = { schoolType = it }, label = { Text("Type") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = motto, onValueChange = { motto = it }, label = { Text("Devise") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Ville") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse / BP") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email officiel") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = directorName, onValueChange = { directorName = it }, label = { Text("Directeur / Proviseur") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = regNumber, onValueChange = { regNumber = it }, label = { Text("N° Agrément / Autorisation") }, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Autoriser l'assistance technique ADRIÈL TECH", style = MaterialTheme.typography.bodyMedium)
                                Text("Journalisation obligatoire de toute intervention", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = supportAccess, onCheckedChange = { supportAccess = it })
                        }
                        Button(
                            onClick = {
                                schoolViewModel.updateSchoolBranding(
                                    school.copy(
                                        name = name,
                                        code = code,
                                        schoolType = schoolType,
                                        motto = motto,
                                        city = city,
                                        address = address,
                                        phone = phone,
                                        email = email,
                                        directorName = directorName,
                                        registrationNumber = regNumber,
                                        primaryColorHex = primaryHex,
                                        secondaryColorHex = secondaryHex,
                                        accentColorHex = accentHex,
                                        logoPrimaryBase64 = logoPrimary,
                                        logoSecondaryBase64 = logoSecondary,
                                        stampBase64 = stamp,
                                        signatureBase64 = signature,
                                        supportAccessEnabled = supportAccess
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_branding_btn")
                        ) {
                            Text(strings.save)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetPickerTile(
    title: String,
    base64: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onPick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Base64ImageBox(base64, title, Modifier.size(64.dp), icon)
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onPick) {
            Text(strings.pickImage, style = MaterialTheme.typography.labelSmall)
        }
    }
}
