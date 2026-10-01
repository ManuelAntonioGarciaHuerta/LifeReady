package com.example.lifeready

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PantallaLifeReady(onSalir = { finish() })
        }
    }
}

// ==================== PALETA DE COLORES ====================
object EleganceTheme {
    val FondoProfundo    = Color(0xFF0D0D0D)
    val SuperficieOscura = Color(0xFF16161A)
    val SuperficieMedia  = Color(0xFF22222B)
    val GrisBorde        = Color(0xFF2E2E3A)
    val TextoBlanco      = Color(0xFFF5F5F7)
    val TextoGris        = Color(0xFF8E8E93)
}

// ==================== MODELOS DE DATOS ====================
data class ItemPrimerosAuxilios(val imagen: Int, val titulo: String, val descripcion: String)
data class PasoModulo(val numero: Int, val titulo: String, val descripcion: String)
data class BotonEmergencia(val titulo: String, val imagenRes: Int, val uri: String)
data class DetalleCurso(
    val introduccion: String,
    val puntosClave: List<String>,
    val pasos: List<PasoModulo>
)

// =====================================================================
// ====================  LIFEREADY COMPLETO  ==========================
// =====================================================================

// ---- Perfil de edad: cambia fuerza de RCP, Heimlich, dosis, etc. ----
enum class PerfilEdad(val etiqueta: String, val emoji: String) {
    BEBE("Bebé (0-1 año)", "👶"),
    NINO("Niño (1-8 años)", "🧒"),
    ADULTO("Adulto (9-64 años)", "🧑"),
    ADULTO_MAYOR("Adulto mayor (65+)", "🧓");

    companion object {
        fun desdeEdad(edad: Int): PerfilEdad = when {
            edad <= 1  -> BEBE
            edad <= 8  -> NINO
            edad <= 64 -> ADULTO
            else       -> ADULTO_MAYOR
        }
    }
}

// ---- Perfil médico: condiciones que cambian las recomendaciones ----
data class PerfilMedico(
    val alergias: Boolean = false,
    val epilepsia: Boolean = false,
    val diabetes: Boolean = false
)

// Genera advertencias médicas personalizadas según el perfil
fun advertenciasPerfil(perfil: PerfilMedico): List<String> {
    val avisos = mutableListOf<String>()
    if (perfil.alergias) avisos.add("⚠️ Alergias registradas: evita administrar medicamentos nuevos sin saber el alérgeno. Si hay hinchazón de cara/garganta, sospecha shock anafiláctico y llama al 911 de inmediato.")
    if (perfil.epilepsia) avisos.add("⚠️ Epilepsia registrada: durante una convulsión NO sujetes a la persona ni le pongas nada en la boca. Protege su cabeza y espera a que pase, máx. 5 min antes de llamar al 911.")
    if (perfil.diabetes) avisos.add("⚠️ Diabetes registrada: si hay confusión o sudoración súbita, sospecha hipoglucemia antes que un derrame. Si está consciente, dale algo dulce; si no responde, llama al 911.")
    avisos.add("🚫 No le des aspirina si sospechas un derrame cerebral (boca caída, debilidad de un lado, habla confusa): podría empeorar una hemorragia.")
    return avisos
}

// Devuelve los pasos de RCP/Heimlich/dosis adaptados según la edad
fun pasosAdaptadosPorEdad(perfilEdad: PerfilEdad): List<PasoModulo> {
    return when (perfilEdad) {
        PerfilEdad.BEBE -> listOf(
            PasoModulo(1, "Mantén la calma", "Respira profundo. Pide ayuda en voz alta o llama al 911 mientras actúas."),
            PasoModulo(2, "RCP en bebés", "Usa solo 2 dedos (no toda la mano) en el centro del pecho. Compresiones de 4 cm de profundidad, 100-120 por minuto. 30 compresiones por 2 ventilaciones suaves cubriendo boca y nariz."),
            PasoModulo(3, "Asfixia en bebés (NO Heimlich)", "Nunca uses la maniobra de Heimlich en un bebé. Sujétalo boca abajo sobre tu antebrazo, cabeza más baja que el cuerpo, y da 5 golpes firmes entre los omóplatos. Si no sale, voltéalo y da 5 compresiones en el pecho con 2 dedos."),
            PasoModulo(4, "Fiebre y medicamentos", "Nunca le das aspirina a un bebé. Solo paracetamol infantil en la dosis exacta que indique el pediatra o el empaque según su peso, nunca a ojo."),
            PasoModulo(5, "Llama al 911", "Da tu ubicación, edad aproximada del bebé y lo que observas: color de piel, respiración, si responde a estímulos."),
            PasoModulo(6, "Mientras llega ayuda", "No lo dejes solo. Mantenlo calentito, vigila su respiración y color de piel constantemente.")
        )
        PerfilEdad.NINO -> listOf(
            PasoModulo(1, "Mantén la calma", "Respira profundo y pide a un adulto que llame al 911 mientras tú actúas."),
            PasoModulo(2, "RCP en niños", "Usa una mano (o ambas si el niño es grande) en el centro del pecho. Compresiones de 5 cm de profundidad, 100-120 por minuto. 30 compresiones por 2 ventilaciones."),
            PasoModulo(3, "Heimlich en niños", "Párate detrás, rodea su cintura, puño arriba del ombligo, jala hacia adentro y arriba con fuerza moderada (menos fuerte que con un adulto). Repite hasta que salga el objeto."),
            PasoModulo(4, "Medicamentos", "No le des aspirina. Usa solo medicamentos pediátricos en la dosis indicada para su peso y edad; si tienes duda, no le des nada y espera a un adulto o paramédico."),
            PasoModulo(5, "Llama al 911", "Indica la edad del niño, qué pasó y su estado actual."),
            PasoModulo(6, "Tranquilízalo", "Los niños se asustan más fácil. Habla con voz calmada y constante hasta que llegue la ayuda.")
        )
        PerfilEdad.ADULTO -> listOf(
            PasoModulo(1, "Mantén la calma", "Antes de actuar, respira profundo. Llama al 911 o pide a alguien que lo haga."),
            PasoModulo(2, "RCP en adultos", "Coloca el talón de tu mano dominante en el centro del pecho, la otra mano encima. Compresiones de al menos 5 cm de profundidad, 100-120 por minuto. 30 compresiones por 2 ventilaciones."),
            PasoModulo(3, "Heimlich en adultos", "Párate detrás, rodea la cintura, puño arriba del ombligo y debajo del esternón, jala con fuerza hacia adentro y arriba. Repite hasta que salga el objeto."),
            PasoModulo(4, "Medicamentos", "No le des aspirina si sospechas un derrame. Si es un episodio cardíaco conocido y tiene su medicamento recetado, ayúdale a tomarlo según su indicación médica."),
            PasoModulo(5, "Llama al 911", "Ubicación exacta, qué ocurrió, estado de la persona."),
            PasoModulo(6, "Permanece con la víctima", "Monitorea su respiración y nivel de conciencia hasta que llegue ayuda.")
        )
        PerfilEdad.ADULTO_MAYOR -> listOf(
            PasoModulo(1, "Mantén la calma", "Respira profundo. Llama al 911 de inmediato; en adultos mayores cada minuto cuenta más."),
            PasoModulo(2, "RCP en adultos mayores", "Misma técnica que en adultos (talón de la mano en el pecho, 100-120 compresiones/min), pero con cuidado: la fragilidad ósea aumenta el riesgo de fracturas de costillas; no dejes de comprimir por eso, es preferible una costilla rota a perder la vida."),
            PasoModulo(3, "Heimlich en adultos mayores", "Aplica con fuerza moderada-firme; si la persona usa silla de ruedas o no puedes rodearla de pie, hazlo sentada empujando hacia adentro y arriba desde atrás de la silla."),
            PasoModulo(4, "Medicamentos y dosis", "No le des aspirina si sospechas un derrame. Revisa si trae un brazalete o tarjeta de condiciones médicas y medicamentos antes de administrar cualquier cosa; muchos adultos mayores toman anticoagulantes que cambian el manejo de hemorragias."),
            PasoModulo(5, "Llama al 911", "Menciona la edad aproximada y cualquier condición crónica visible (diabetes, cardiopatía, etc.)."),
            PasoModulo(6, "Acompaña y observa", "Vigila confusión repentina, caídas o dificultad para hablar; son señales de alerta frecuentes en este grupo de edad.")
        )
    }
}

// ---- Microaprendizaje diario: reto de 60 segundos ----
data class OpcionReto(val texto: String, val esCorrecta: Boolean)
data class RetoDiario(val pregunta: String, val opciones: List<OpcionReto>, val explicacion: String, val insigniaId: String)

val bancoRetos = listOf(
    RetoDiario(
        pregunta = "Si alguien se está ahogando con una uva ahora mismo, ¿qué haces primero?",
        opciones = listOf(
            OpcionReto("Le doy agua para que la pase", false),
            OpcionReto("Aplico la maniobra de Heimlich", true),
            OpcionReto("Espero a que la tosa sola sin hacer nada", false)
        ),
        explicacion = "Si no puede toser, hablar o respirar, actúa de inmediato con Heimlich. Dar agua puede empeorar la obstrucción.",
        insigniaId = "atragantamiento"
    ),
    RetoDiario(
        pregunta = "Alguien se desmaya y no respira con normalidad. ¿Qué haces primero?",
        opciones = listOf(
            OpcionReto("Le doy de beber agua", false),
            OpcionReto("Llamo al 911 e inicio RCP", true),
            OpcionReto("Espero a que despierte solo", false)
        ),
        explicacion = "Sin respiración normal, cada segundo cuenta: llama al 911 e inicia compresiones de inmediato.",
        insigniaId = "rcp"
    ),
    RetoDiario(
        pregunta = "Una persona tiene una herida que sangra mucho. ¿Qué haces?",
        opciones = listOf(
            OpcionReto("Aplico presión directa firme con tela limpia", true),
            OpcionReto("Le pongo alcohol y la dejo destapada", false),
            OpcionReto("Espero a ver si para sola", false)
        ),
        explicacion = "La presión directa y sostenida es la forma más efectiva de controlar una hemorragia mientras llega ayuda.",
        insigniaId = "hemorragias"
    ),
    RetoDiario(
        pregunta = "Sospechas que alguien tiene un derrame cerebral (boca caída, habla confusa). ¿Qué NO debes hacer?",
        opciones = listOf(
            OpcionReto("Llamar al 911 de inmediato", false),
            OpcionReto("Anotar la hora en que empezaron los síntomas", false),
            OpcionReto("Darle aspirina", true)
        ),
        explicacion = "Nunca le des aspirina si sospechas un derrame: en una hemorragia cerebral puede empeorar el sangrado.",
        insigniaId = "derrame"
    ),
    RetoDiario(
        pregunta = "Una persona con epilepsia conocida empieza a convulsionar. ¿Qué haces?",
        opciones = listOf(
            OpcionReto("La sujeto con fuerza para que no se mueva", false),
            OpcionReto("Le pongo algo en la boca para que no se muerda la lengua", false),
            OpcionReto("Protejo su cabeza y espero, sin sujetarla", true)
        ),
        explicacion = "Nunca sujetes a alguien con convulsiones ni le pongas nada en la boca; solo protege su cabeza y cronometra.",
        insigniaId = "epilepsia"
    )
)

data class Insignia(val id: String, val nombre: String, val emoji: String, val descripcionReal: String)

val catalogoInsignias = listOf(
    Insignia("atragantamiento", "Insignia de Atragantamiento", "🫁", "Dominas la maniobra de Heimlich"),
    Insignia("rcp",            "Insignia de RCP",              "❤️", "Sabes iniciar RCP sin dudar"),
    Insignia("hemorragias",     "Insignia de Hemorragias",      "🩹", "Dominas el control de hemorragias y torniquetes"),
    Insignia("derrame",         "Insignia de Derrames",         "🧠", "Reconoces señales de un derrame y qué evitar"),
    Insignia("epilepsia",       "Insignia de Convulsiones",     "🛡️", "Sabes proteger a alguien durante una crisis convulsiva")
)

// ==================== LIFEREADY: NAV INTERNO ====================
@Composable
fun PantallaLifeReady(onSalir: () -> Unit) {
    var pantallaActual by remember { mutableStateOf("principal") } // principal | curso | perfil | reto
    var cursoSeleccionadoIndex by remember { mutableIntStateOf(-1) }

    // ---- Estado de personalización (perfil edad + perfil médico) ----
    var edadTexto    by remember { mutableStateOf("") }
    var perfilEdad    by remember { mutableStateOf(PerfilEdad.ADULTO) }
    var perfilMedico  by remember { mutableStateOf(PerfilMedico()) }

    // ---- Estado de gamificación ----
    val insigniasDesbloqueadas = remember { mutableStateListOf<String>() }
    var retoDelDiaIndex by remember { mutableIntStateOf((0 until bancoRetos.size).random()) }
    var retoCompletadoHoy by remember { mutableStateOf(false) }

    when (pantallaActual) {
        "perfil" -> PantallaPerfilPersonalizacion(
            edadTexto = edadTexto,
            onEdadChange = {
                edadTexto = it
                it.toIntOrNull()?.let { e -> perfilEdad = PerfilEdad.desdeEdad(e) }
            },
            perfilEdadManual = perfilEdad,
            onPerfilEdadManual = { perfilEdad = it },
            perfilMedico = perfilMedico,
            onPerfilMedicoChange = { perfilMedico = it },
            onVolver = { pantallaActual = "principal" }
        )
        "reto" -> PantallaRetoDiario(
            reto = bancoRetos[retoDelDiaIndex],
            yaCompletado = retoCompletadoHoy,
            onRespuestaCorrecta = { insigniaId ->
                if (!insigniasDesbloqueadas.contains(insigniaId)) insigniasDesbloqueadas.add(insigniaId)
                retoCompletadoHoy = true
            },
            onVolver = { pantallaActual = "principal" }
        )
        "curso" -> LifeReadyPantallaIntroduccion(
            indexCurso = cursoSeleccionadoIndex,
            perfilEdad = perfilEdad,
            perfilMedico = perfilMedico,
            onVolver = { pantallaActual = "principal" }
        )
        else -> LifeReadyPantallaPrincipal(
            onSalir = onSalir,
            perfilEdad = perfilEdad,
            perfilMedico = perfilMedico,
            insigniasDesbloqueadas = insigniasDesbloqueadas,
            retoCompletadoHoy = retoCompletadoHoy,
            onCursoClick = { index -> cursoSeleccionadoIndex = index; pantallaActual = "curso" },
            onAbrirPerfil = { pantallaActual = "perfil" },
            onAbrirReto = { pantallaActual = "reto" }
        )
    }
}

// ==================== LIFEREADY: PANTALLA DE PERFIL ====================
@Composable
fun PantallaPerfilPersonalizacion(
    edadTexto: String,
    onEdadChange: (String) -> Unit,
    perfilEdadManual: PerfilEdad,
    onPerfilEdadManual: (PerfilEdad) -> Unit,
    perfilMedico: PerfilMedico,
    onPerfilMedicoChange: (PerfilMedico) -> Unit,
    onVolver: () -> Unit
) {
    BackHandler { onVolver() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EleganceTheme.FondoProfundo)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Atrás", tint = EleganceTheme.TextoBlanco, modifier = Modifier.size(24.dp).clickable { onVolver() })
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Personalización", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoBlanco)
            }
        }

        item {
            Text(text = "EDAD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoGris, letterSpacing = 1.5.sp)
        }
        item {
            OutlinedTextField(
                value = edadTexto,
                onValueChange = onEdadChange,
                label = { Text("Edad (años)", color = EleganceTheme.TextoGris) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = EleganceTheme.TextoBlanco,
                    unfocusedTextColor = EleganceTheme.TextoBlanco,
                    focusedContainerColor = EleganceTheme.SuperficieOscura,
                    unfocusedContainerColor = EleganceTheme.SuperficieOscura,
                    focusedBorderColor = Color(0xFF6DE19D),
                    unfocusedBorderColor = EleganceTheme.GrisBorde
                )
            )
        }
        item {
            Text(text = "El modo se ajusta automáticamente con tu edad. También puedes elegirlo manualmente:", fontSize = 12.sp, color = EleganceTheme.TextoGris)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PerfilEdad.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (perfilEdadManual == opcion) Color(0xFF6DE19D) else EleganceTheme.GrisBorde, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(EleganceTheme.SuperficieOscura)
                            .clickable { onPerfilEdadManual(opcion) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = perfilEdadManual == opcion,
                            onClick = { onPerfilEdadManual(opcion) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6DE19D), unselectedColor = EleganceTheme.TextoGris)
                        )
                        Text(text = "${opcion.emoji}  ${opcion.etiqueta}", fontSize = 14.sp, color = EleganceTheme.TextoBlanco)
                    }
                }
            }
        }

        item { HorizontalDivider(color = EleganceTheme.GrisBorde) }

        item {
            Text(text = "PERFIL MÉDICO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoGris, letterSpacing = 1.5.sp)
        }
        item {
            Text(text = "Marca tus condiciones para que LifeReady adapte los pasos (ej. \"No le des aspirina si sospechas derrame\").", fontSize = 12.sp, color = EleganceTheme.TextoGris)
        }
        item {
            FilaCheckMedico(titulo = "Alergias", checked = perfilMedico.alergias) { onPerfilMedicoChange(perfilMedico.copy(alergias = it)) }
        }
        item {
            FilaCheckMedico(titulo = "Epilepsia", checked = perfilMedico.epilepsia) { onPerfilMedicoChange(perfilMedico.copy(epilepsia = it)) }
        }
        item {
            FilaCheckMedico(titulo = "Diabetes", checked = perfilMedico.diabetes) { onPerfilMedicoChange(perfilMedico.copy(diabetes = it)) }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun FilaCheckMedico(titulo: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EleganceTheme.GrisBorde, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(EleganceTheme.SuperficieOscura)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6DE19D)))
        Text(text = titulo, fontSize = 14.sp, color = EleganceTheme.TextoBlanco)
    }
}

// ==================== LIFEREADY: RETO DIARIO (MICROAPRENDIZAJE) ====================
@Composable
fun PantallaRetoDiario(reto: RetoDiario, yaCompletado: Boolean, onRespuestaCorrecta: (String) -> Unit, onVolver: () -> Unit) {
    BackHandler { onVolver() }
    var opcionElegida by remember { mutableStateOf<Int?>(null) }
    var mostrarResultado by remember { mutableStateOf(yaCompletado) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EleganceTheme.FondoProfundo)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Atrás", tint = EleganceTheme.TextoBlanco, modifier = Modifier.size(24.dp).clickable { onVolver() })
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Reto del día · 60 segundos", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoBlanco)
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = "🚨", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = reto.pregunta, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = EleganceTheme.TextoBlanco, lineHeight = 26.sp)
        Spacer(modifier = Modifier.height(24.dp))

        reto.opciones.forEachIndexed { index, opcion ->
            val seleccionada = opcionElegida == index
            val colorBorde = when {
                !mostrarResultado -> if (seleccionada) Color(0xFF6DE19D) else EleganceTheme.GrisBorde
                opcion.esCorrecta -> Color(0xFF6DE19D)
                seleccionada -> Color(0xFFFF4444)
                else -> EleganceTheme.GrisBorde
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.5.dp, colorBorde, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(EleganceTheme.SuperficieOscura)
                    .clickable(enabled = !mostrarResultado) {
                        opcionElegida = index
                        mostrarResultado = true
                        if (opcion.esCorrecta) onRespuestaCorrecta(reto.insigniaId)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = opcion.texto, fontSize = 14.sp, color = EleganceTheme.TextoBlanco, modifier = Modifier.weight(1f))
                if (mostrarResultado && opcion.esCorrecta) Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF6DE19D), modifier = Modifier.size(18.dp))
            }
        }

        if (mostrarResultado) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = EleganceTheme.SuperficieMedia),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = reto.explicacion, fontSize = 13.sp, color = EleganceTheme.TextoGris, modifier = Modifier.padding(14.dp), lineHeight = 19.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            val insignia = catalogoInsignias.first { it.id == reto.insigniaId }
            Row(
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF6DE19D), RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(Color(0xFF6DE19D).copy(alpha = 0.1f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = insignia.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = insignia.nombre, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6DE19D))
                    Text(text = "En la vida real: ${insignia.descripcionReal}", fontSize = 11.sp, color = EleganceTheme.TextoGris)
                }
            }
        }
    }
}

// ==================== LIFEREADY: PANTALLA PRINCIPAL ====================
@Composable
fun LifeReadyPantallaPrincipal(
    onSalir: () -> Unit,
    perfilEdad: PerfilEdad,
    perfilMedico: PerfilMedico,
    insigniasDesbloqueadas: List<String>,
    retoCompletadoHoy: Boolean,
    onCursoClick: (Int) -> Unit,
    onAbrirPerfil: () -> Unit,
    onAbrirReto: () -> Unit
) {
    val context = LocalContext.current

    val listaColumn = listOf(
        ItemPrimerosAuxilios(R.drawable.imagen1, "Reconocimiento de Crisis",
            "Aprende a identificar de manera clara y precisa cuando alguien está en medio de una crisis."),
        ItemPrimerosAuxilios(R.drawable.imagen2, "Toma de Signos Vitales",
            "Conoce cómo medir la frecuencia cardíaca, respiratoria, temperatura y presión arterial."),
        ItemPrimerosAuxilios(R.drawable.imagen3, "Primeros Auxilios Básicos",
            "Aprende a evaluar al paciente, identificar posibles padecimientos y aplicar los primeros cuidados."),
        ItemPrimerosAuxilios(R.drawable.imagen4, "Seguimiento de Síntomas",
            "Aprende a monitorear la evolución de los síntomas mientras llega la ayuda profesional.")
    )

    // ── BOTONES DE EMERGENCIA CON IMAGEN PROPIA ────────────────────────
    val botonesEmergencia = listOf(
        BotonEmergencia("Llamar al 911",        R.drawable.img_911,      "tel:911"),
        BotonEmergencia("Llamar a Cruz Roja",   R.drawable.img_cruzroja, "tel:065"),
        BotonEmergencia("Hospital más cercano", R.drawable.img_hospi,    "geo:0,0?q=Urgencias")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EleganceTheme.FondoProfundo)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Atrás",
                tint = EleganceTheme.TextoBlanco,
                modifier = Modifier.size(24.dp).clickable { onSalir() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "LifeReady", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoBlanco, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.weight(1f))
            // Chip de perfil activo -> abre personalización
            Row(
                modifier = Modifier
                    .border(1.dp, EleganceTheme.GrisBorde, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(EleganceTheme.SuperficieOscura)
                    .clickable { onAbrirPerfil() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = perfilEdad.emoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Perfil", fontSize = 12.sp, color = EleganceTheme.TextoBlanco, fontWeight = FontWeight.Medium)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Image(
                    painter = painterResource(id = R.drawable.imagen),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            item {
                Text(text = "Aprende a actuar correctamente ante una emergencia.", fontSize = 16.sp, color = EleganceTheme.TextoGris, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            // ── TARJETA DE PERSONALIZACIÓN ACTIVA ──────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF4A90D9), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF003466).copy(alpha = 0.35f))
                        .clickable { onAbrirPerfil() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = perfilEdad.emoji, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Modo activo: ${perfilEdad.etiqueta}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EleganceTheme.TextoBlanco)
                        val condiciones = listOfNotNull(
                            if (perfilMedico.alergias) "Alergias" else null,
                            if (perfilMedico.epilepsia) "Epilepsia" else null,
                            if (perfilMedico.diabetes) "Diabetes" else null
                        )
                        Text(
                            text = if (condiciones.isEmpty()) "Sin condiciones médicas registradas · Toca para editar" else "${condiciones.joinToString(", ")} · Toca para editar",
                            fontSize = 11.sp,
                            color = EleganceTheme.TextoGris
                        )
                    }
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = EleganceTheme.TextoGris, modifier = Modifier.size(16.dp))
                }
            }

            // ── RETO DEL DÍA (MICROAPRENDIZAJE) ────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (retoCompletadoHoy) Color(0xFF6DE19D) else Color(0xFFFFC107), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (retoCompletadoHoy) Color(0xFF6DE19D).copy(alpha = 0.1f) else Color(0xFFFFC107).copy(alpha = 0.1f))
                        .clickable { onAbrirReto() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (retoCompletadoHoy) "✅" else "🚨", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (retoCompletadoHoy) "Reto del día completado" else "Reto del día · 60 segundos",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (retoCompletadoHoy) Color(0xFF6DE19D) else Color(0xFFFFC107)
                        )
                        Text(
                            text = if (retoCompletadoHoy) "Vuelve mañana por una nueva insignia" else "Una emergencia, 3 opciones, te corregimos al instante",
                            fontSize = 11.sp, color = EleganceTheme.TextoGris
                        )
                    }
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = EleganceTheme.TextoGris, modifier = Modifier.size(16.dp))
                }
            }

            // ── INSIGNIAS (GAMIFICACIÓN ÚTIL) ──────────────────────────
            item {
                Text(text = "MIS INSIGNIAS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoBlanco, letterSpacing = 1.5.sp)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(catalogoInsignias) { insignia ->
                        val desbloqueada = insigniasDesbloqueadas.contains(insignia.id)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(96.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, if (desbloqueada) Color(0xFF6DE19D) else EleganceTheme.GrisBorde, CircleShape)
                                    .background(if (desbloqueada) Color(0xFF6DE19D).copy(alpha = 0.15f) else EleganceTheme.SuperficieOscura),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = if (desbloqueada) insignia.emoji else "🔒", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = insignia.nombre.removePrefix("Insignia de "),
                                fontSize = 10.sp,
                                color = if (desbloqueada) EleganceTheme.TextoBlanco else EleganceTheme.TextoGris,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }

            // ── SECCIÓN EMERGENCIAS ────────────────────────────────────
            item {
                Text(text = "EMERGENCIAS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoBlanco, letterSpacing = 1.5.sp)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    botonesEmergencia.forEach { boton ->
                        val esLlamada  = boton.uri.startsWith("tel:")
                        val colorFondo = if (esLlamada) Color(0xFF8B0000) else Color(0xFF003466)
                        val colorBorde = if (esLlamada) Color(0xFFFF4444) else Color(0xFF4A90D9)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .border(1.dp, colorBorde, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorFondo)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(boton.uri))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Imagen personalizada en círculo
                            Image(
                                painter = painterResource(id = boton.imagenRes),
                                contentDescription = boton.titulo,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = boton.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EleganceTheme.TextoBlanco)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = EleganceTheme.TextoGris, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ── MÓDULOS DE APRENDIZAJE ─────────────────────────────────
            item {
                Text(text = "MÓDULOS DE APRENDIZAJE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoBlanco, letterSpacing = 1.5.sp)
            }
            itemsIndexed(listaColumn) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onCursoClick(index) },
                    colors = CardDefaults.cardColors(containerColor = EleganceTheme.SuperficieOscura),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EleganceTheme.GrisBorde)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Image(
                            painter = painterResource(id = item.imagen),
                            contentDescription = item.titulo,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(text = item.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EleganceTheme.TextoBlanco, modifier = Modifier.padding(top = 8.dp))
                        Text(text = item.descripcion, fontSize = 13.sp, color = EleganceTheme.TextoGris, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun LifeReadyPantallaIntroduccion(indexCurso: Int, perfilEdad: PerfilEdad, perfilMedico: PerfilMedico, onVolver: () -> Unit) {
    BackHandler { onVolver() }

    val context = LocalContext.current

    var moduloIniciado   by remember { mutableStateOf(false) }
    var modoPanico       by remember { mutableStateOf(false) }
    var tiempoRestante   by remember { mutableStateOf(300) } // 5 minutos en segundos
    var timerActivo      by remember { mutableStateOf(false) }
    var tiempoAgotado    by remember { mutableStateOf(false) }

    // ── TEXT TO SPEECH ─────────────────────────────────────────────────
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale("es", "MX")
                tts?.setSpeechRate(0.85f) // 1.0 = normal, menos de 1.0 = más lento/tranquilo
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    // ── MEDIA PLAYER PARA ALARMA ────────────────────────────────────────
    val mediaPlayer = remember {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            try {
                setDataSource(context, uri)
                prepare()
                isLooping = true
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    // ── TIMER COUNTDOWN ─────────────────────────────────────────────────
    LaunchedEffect(timerActivo) {
        if (timerActivo) {
            while (tiempoRestante > 0 && timerActivo) {
                delay(1000L)
                tiempoRestante--
            }
            if (tiempoRestante == 0) {
                tiempoAgotado = true
                timerActivo = false
                try { mediaPlayer.start() } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    // ── FORMATO TIMER MM:SS ─────────────────────────────────────────────
    val minutos  = tiempoRestante / 60
    val segundos = tiempoRestante % 60
    val timerTexto = "%02d:%02d".format(minutos, segundos)

    // Color del timer según urgencia
    val timerColor = when {
        tiempoRestante <= 30  -> Color(0xFFFF4444)
        tiempoRestante <= 60  -> Color(0xFFFFC107)
        else                  -> Color(0xFF6DE19D)
    }

    val contenidos = listOf(
        DetalleCurso(
            introduccion = "El reconocimiento temprano de una crisis puede marcar la diferencia entre la vida y la muerte. Este módulo te enseña a leer señales físicas, conductuales y ambientales para actuar con rapidez y seguridad.",
            puntosClave = listOf(
                "Identificación de pánico y crisis nerviosa",
                "Evaluación del entorno para garantizar tu seguridad",
                "Activación correcta de servicios de emergencia"
            ),
            pasos = listOf(
                PasoModulo(1, "Mantén la calma", "Antes de actuar, respira profundo. Una mente clara toma mejores decisiones. Si tú entras en pánico, no podrás ayudar a nadie. Repite mentalmente: Estoy a salvo, puedo ayudar."),
                PasoModulo(2, "Evalúa el entorno", "Observa si existe algún peligro activo: fuego, gas, tráfico, agresores. No te acerques a la víctima si el área no es segura. Tu seguridad es la prioridad número uno."),
                PasoModulo(3, "Identifica señales de crisis", "Busca: pérdida de conciencia, convulsiones, dificultad para respirar, piel pálida o azulada, confusión extrema, dolor intenso en el pecho o incapacidad de moverse."),
                PasoModulo(4, "Habla con la persona", "Si está consciente, pregunta con voz firme y calmada: ¿Me escuchas? ¿Dónde te duele? ¿Tienes alguna condición médica? Su respuesta guiará tus siguientes pasos."),
                PasoModulo(5, "Activa los servicios de emergencia", "Llama al 911. Proporciona: tu ubicación exacta, qué ocurrió, cuántas personas están afectadas y el estado de la víctima. No cuelgues hasta que te lo indiquen."),
                PasoModulo(6, "Permanece con la víctima", "Habla con ella para mantenerla tranquila y consciente. No la muevas a menos que exista un peligro inmediato. Monitorea su respiración hasta que llegue la ayuda.")
            )
        ),
        DetalleCurso(
            introduccion = "Los signos vitales son los indicadores más directos del estado de salud de una persona. Aprender a medirlos correctamente te permite reportar información precisa a los paramédicos y detectar cambios críticos a tiempo.",
            puntosClave = listOf(
                "Medición del pulso y frecuencia cardíaca",
                "Conteo de la frecuencia respiratoria",
                "Uso del termómetro e interpretación de resultados"
            ),
            pasos = listOf(
                PasoModulo(1, "Pulso — Frecuencia cardíaca", "Coloca dos dedos, índice y medio, sobre la muñeca, justo debajo del pulgar, o en el cuello junto a la tráquea. Cuenta los latidos durante 30 segundos y multiplica por 2. Normal en adultos: 60 a 100 latidos por minuto."),
                PasoModulo(2, "Interpreta el pulso", "Más de 100 latidos por minuto puede indicar fiebre, deshidratación o shock. Menos de 60 puede ser normal en atletas, o señal de problemas cardíacos. Pulso irregular requiere atención médica inmediata."),
                PasoModulo(3, "Frecuencia respiratoria", "Observa el pecho de la persona sin que ella lo note. Cuenta las veces que sube y baja el pecho en 30 segundos y multiplica por 2. Normal en adultos: 12 a 20 respiraciones por minuto."),
                PasoModulo(4, "Temperatura corporal", "Usa un termómetro digital bajo la axila o en la frente. Espera la señal sonora. Normal: 36.5 a 37.5 grados. Fiebre: 38 grados o más. Hipotermia: menos de 35 grados. Ambos extremos requieren atención urgente."),
                PasoModulo(5, "Nivel de conciencia", "Evalúa la respuesta con la escala AVDI: A igual a Alerta, V igual a responde solo cuando se le habla, D igual a responde solo ante dolor, I igual a Inconsciente. Cualquier nivel por debajo de Alerta es una emergencia."),
                PasoModulo(6, "Registra y reporta", "Anota los valores obtenidos con la hora exacta. Al llegar los paramédicos, diles: pulso, respiración, temperatura y nivel de conciencia. Esta información acelera el diagnóstico y puede salvar la vida.")
            )
        ),
        // Módulo 3: Primeros Auxilios Básicos -> usa pasos ADAPTADOS por edad
        DetalleCurso(
            introduccion = "Los primeros 5 minutos tras una emergencia son los más críticos. Conocer las técnicas básicas de primeros auxilios te convierte en el primer eslabón de la cadena de supervivencia antes de que llegue la ayuda profesional. Los pasos de esta sección se ajustan según el perfil de edad seleccionado.",
            puntosClave = listOf(
                "Maniobra de RCP adaptada a la edad de la víctima",
                "Técnica de desobstrucción de vía aérea según edad",
                "Contención correcta de hemorragias y manejo de medicamentos"
            ),
            pasos = pasosAdaptadosPorEdad(perfilEdad)
        ),
        DetalleCurso(
            introduccion = "Una vez aplicados los primeros auxilios, el trabajo no termina. Monitorear y registrar la evolución de la víctima es fundamental para que los paramédicos tomen decisiones rápidas y acertadas al llegar.",
            puntosClave = listOf(
                "Bitácora de cambios en el estado del paciente",
                "Cómo hacer un reporte estructurado a emergencias",
                "Técnicas de estabilización psicológica"
            ),
            pasos = listOf(
                PasoModulo(1, "Establece una línea base", "Tan pronto como sea posible, registra el estado inicial de la víctima: nivel de conciencia, pulso, respiración y temperatura. Anota la hora exacta. Esta es tu referencia para detectar si mejora o empeora."),
                PasoModulo(2, "Monitorea cada 5 minutos", "Repite la evaluación de signos vitales cada 5 minutos y anota los cambios. Presta atención a pérdida de conciencia progresiva, dificultad creciente para respirar, y agitación o confusión que aumenta."),
                PasoModulo(3, "Registra síntomas nuevos", "Si la persona está consciente, pregúntale regularmente: ¿Sientes algo nuevo? ¿El dolor aumentó? ¿Sientes entumecimiento? Cualquier síntoma nuevo debe anotarse con la hora en que apareció."),
                PasoModulo(4, "Estructura tu reporte al 911", "Cuando hables con emergencias, usa este orden: primero tu ubicación exacta, segundo qué ocurrió y cuándo, tercero número de víctimas, cuarto estado actual del paciente, quinto lo que ya hiciste. Sé claro y conciso."),
                PasoModulo(5, "Estabilización psicológica", "Si la víctima está consciente, háblale con calma y seguridad. Di frases como: Ya llamé al 911, la ayuda viene en camino. Estás a salvo, estoy aquí contigo. La tranquilidad reduce el shock."),
                PasoModulo(6, "Entrega el mando a los paramédicos", "Al llegar los servicios de emergencia, preséntate y entrega tu bitácora verbal: estado inicial, cambios observados, hora, intervenciones realizadas y medicamentos que la víctima mencionó tomar. Luego retírate para darles espacio.")
            )
        )
    )

    val titulos = listOf(
        "Reconocimiento de Crisis",
        "Toma de Signos Vitales",
        "Primeros Auxilios Básicos",
        "Seguimiento de Síntomas"
    )

    val cursoActual  = contenidos[indexCurso]
    val tituloActual = titulos[indexCurso]
    val avisosMedicos = advertenciasPerfil(perfilMedico)

    // ── PANTALLA DE TIEMPO AGOTADO ──────────────────────────────────────
    if (tiempoAgotado) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A0000))
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "⏰", fontSize = 72.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "TIEMPO AGOTADO",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF4444),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No lograste completar el módulo a tiempo.\nInténtalo de nuevo.",
                fontSize = 15.sp,
                color = EleganceTheme.TextoGris,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    mediaPlayer.stop()
                    try { mediaPlayer.prepare() } catch (e: Exception) {}
                    tiempoAgotado  = false
                    moduloIniciado = false
                    timerActivo    = false
                    tiempoRestante = 300
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "INTENTAR DE NUEVO", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    mediaPlayer.stop()
                    try { mediaPlayer.prepare() } catch (e: Exception) {}
                    onVolver()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, EleganceTheme.GrisBorde, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = EleganceTheme.SuperficieOscura),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "VOLVER AL MENÚ", color = EleganceTheme.TextoBlanco, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EleganceTheme.FondoProfundo)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = tituloActual,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = EleganceTheme.TextoBlanco,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── CHIP DE MODO DE EDAD ACTIVO ──────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .border(1.dp, Color(0xFF4A90D9), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF003466).copy(alpha = 0.3f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(text = "${perfilEdad.emoji} Adaptado para: ${perfilEdad.etiqueta}", fontSize = 12.sp, color = EleganceTheme.TextoBlanco, fontWeight = FontWeight.Medium)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EleganceTheme.SuperficieMedia),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EleganceTheme.GrisBorde)
            ) {
                Text(
                    text = cursoActual.introduccion,
                    fontSize = 15.sp,
                    color = EleganceTheme.TextoGris,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 22.sp
                )
            }
        }

        // ── ADVERTENCIAS PERSONALIZADAS SEGÚN PERFIL MÉDICO ──────────────
        if (indexCurso == 2) {
            items(avisosMedicos.size) { i ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1500)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFC107))
                ) {
                    Text(text = avisosMedicos[i], fontSize = 13.sp, color = Color(0xFFFFC107), modifier = Modifier.padding(14.dp), lineHeight = 19.sp)
                }
            }
        }

        item {
            Text(
                text = "Lo que aprenderás:",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = EleganceTheme.TextoBlanco,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(cursoActual.puntosClave.size) { i ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(Color(0xFF6DE19D)))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = cursoActual.puntosClave[i], fontSize = 15.sp, color = EleganceTheme.TextoGris, lineHeight = 22.sp)
            }
        }

        // ── TOGGLE MODO PÁNICO ─────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (modoPanico) Color(0xFFFF4444) else EleganceTheme.GrisBorde,
                        RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (modoPanico) Color(0xFF2A0000) else EleganceTheme.SuperficieOscura)
                    .clickable { if (!moduloIniciado) modoPanico = !modoPanico }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (modoPanico) "🚨" else "🔕", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Modo Pánico",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (modoPanico) Color(0xFFFF4444) else EleganceTheme.TextoBlanco
                    )
                    Text(
                        text = if (modoPanico) "Activado"
                        else "Desactivado",
                        fontSize = 12.sp,
                        color = EleganceTheme.TextoGris
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (modoPanico) Color(0xFFFF4444) else EleganceTheme.GrisBorde),
                    contentAlignment = Alignment.Center
                ) {
                    if (modoPanico) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── TIMER (visible solo si modoPanico y moduloIniciado) ─────────
        if (modoPanico && moduloIniciado) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, timerColor, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(timerColor.copy(alpha = 0.1f))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timerTexto,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "TIEMPO RESTANTE",
                            fontSize = 11.sp,
                            color = timerColor.copy(alpha = 0.7f),
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── BOTÓN INICIAR ───────────────────────────────────────────────
        item {
            Button(
                onClick = {
                    moduloIniciado = true
                    if (modoPanico) {
                        timerActivo = true
                        // Leer todos los pasos en voz alta
                        tts?.stop()
                        val textoCompleto = cursoActual.pasos.joinToString(". ") { paso ->
                            "Paso ${paso.numero}: ${paso.titulo}. ${paso.descripcion}"
                        }
                        tts?.speak(textoCompleto, TextToSpeech.QUEUE_FLUSH, null, "pasos_modulo")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (modoPanico) Color(0xFFCC0000) else Color(0xFF6DE19D)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (modoPanico) Color.White else Color(0xFF012511),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (modoPanico) "INICIAR CON MODO PÁNICO" else "INICIAR MÓDULO",
                    color = if (modoPanico) Color.White else Color(0xFF012511),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }

        // ── PASOS ───────────────────────────────────────────────────────
        if (moduloIniciado) {
            item {
                HorizontalDivider(color = EleganceTheme.GrisBorde)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PASOS DEL MÓDULO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EleganceTheme.TextoBlanco,
                    letterSpacing = 1.5.sp
                )
            }

            items(cursoActual.pasos.size) { i ->
                val paso = cursoActual.pasos[i]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EleganceTheme.SuperficieOscura),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EleganceTheme.GrisBorde)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6DE19D).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF6DE19D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${paso.numero}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6DE19D))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = paso.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EleganceTheme.TextoBlanco)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = paso.descripcion, fontSize = 13.sp, color = EleganceTheme.TextoGris, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // ── BOTÓN CERRAR MÓDULO ─────────────────────────────────────
            item {
                Button(
                    onClick = {
                        moduloIniciado = false
                        timerActivo    = false
                        tiempoRestante = 300
                        tts?.stop()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, Color(0xFFFF4444), RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CERRAR MÓDULO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.5.sp)
                }
            }
        }

        item {
            Button(
                onClick = {
                    tts?.stop()
                    mediaPlayer.stop()
                    onVolver()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, EleganceTheme.GrisBorde, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = EleganceTheme.SuperficieOscura),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "VOLVER AL MENÚ", color = EleganceTheme.TextoBlanco, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
