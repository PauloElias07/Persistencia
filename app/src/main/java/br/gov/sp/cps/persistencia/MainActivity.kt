package br.gov.sp.cps.persistencia

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.gov.sp.cps.persistencia.ui.theme.PersistenciaTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Persistência com DataStore - Apenas o nome do motorista é gravado
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias_viagens")
val NOME_MOTORISTA = stringPreferencesKey("nome_motorista")

data class Viagem(
    val data: String,
    val kmInicial: Double,
    val kmFinal: Double,
    val litros: Double,
    val valorCombustivel: Double,
    val pedagio: Double
) {
    fun calcularKm(): Double = kmFinal - kmInicial
    fun custoCombustivel(): Double = litros * valorCombustivel
    fun custoTotal(): Double = custoCombustivel() + pedagio
    fun mediaKmLitro(): Double = if (litros > 0) calcularKm() / litros else 0.0
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersistenciaTheme {
                val navController = rememberNavController()
                val viagens = remember { mutableStateListOf<Viagem>() }

                NavHost(navController = navController, startDestination = "cadastro") {
                    composable("cadastro") {
                        CadastroScreen(navController, viagens)
                    }
                    composable("resultado") {
                        ResultadoScreen(navController, viagens)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroScreen(navController: NavController, viagens: SnapshotStateList<Viagem>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Leitura do nome do motorista gravado no DataStore
    val motoristaSalvoFlow = remember(context) {
        context.dataStore.data.map { preferences ->
            preferences[NOME_MOTORISTA] ?: ""
        }
    }
    val motoristaSalvo by motoristaSalvoFlow.collectAsState(initial = "")

    var motoristaText by remember(motoristaSalvo) { mutableStateOf(motoristaSalvo) }
    var dataText by remember { mutableStateOf("") }
    var kmInicialText by remember { mutableStateOf("") }
    var kmFinalText by remember { mutableStateOf("") }
    var litrosText by remember { mutableStateOf("") }
    var valorCombustivelText by remember { mutableStateOf("") }
    var pedagioText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Controle de Viagens") },
                actions = {
                    IconButton(onClick = { navController.navigate("resultado") }) {
                        Icon(Icons.Default.List, contentDescription = "Ver Histórico")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Campo de gravação persistente apenas para o Nome do Motorista
            OutlinedTextField(
                value = motoristaText,
                onValueChange = { motoristaText = it },
                label = { Text("Nome do Motorista (Salvo no DataStore)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dataText,
                onValueChange = { dataText = it },
                label = { Text("Data (ex: 20/05)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = kmInicialText,
                    onValueChange = { kmInicialText = it },
                    label = { Text("Km Inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = kmFinalText,
                    onValueChange = { kmFinalText = it },
                    label = { Text("Km Final") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = litrosText,
                    onValueChange = { litrosText = it },
                    label = { Text("Litros") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = valorCombustivelText,
                    onValueChange = { valorCombustivelText = it },
                    label = { Text("Preço p/ Litro (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = pedagioText,
                onValueChange = { pedagioText = it },
                label = { Text("Pedágio (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val kmIni = kmInicialText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val kmFin = kmFinalText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val lit = litrosText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val valComb = valorCombustivelText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val ped = pedagioText.replace(",", ".").toDoubleOrNull() ?: 0.0

                    // Gravar apenas o nome do motorista no DataStore
                    scope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[NOME_MOTORISTA] = motoristaText
                        }
                    }

                    if (dataText.isNotBlank()) {
                        viagens.add(Viagem(dataText, kmIni, kmFin, lit, valComb, ped))
                        dataText = ""; kmInicialText = ""; kmFinalText = ""
                        litrosText = ""; valorCombustivelText = ""; pedagioText = ""
                        navController.navigate("resultado")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cadastrar e Gravar Motorista")
            }

            Button(
                onClick = { navController.navigate("resultado") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Ver Histórico de Viagens")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoScreen(navController: NavController, viagens: List<Viagem>) {
    val context = LocalContext.current

    // Recupera o nome do motorista gravado
    val motoristaSalvoFlow = remember(context) {
        context.dataStore.data.map { preferences ->
            preferences[NOME_MOTORISTA] ?: "Não informado"
        }
    }
    val motoristaSalvo by motoristaSalvoFlow.collectAsState(initial = "")

    val totalKm = viagens.sumOf { it.calcularKm() }
    val totalGasto = viagens.sumOf { it.custoTotal() }
    val totalLitros = viagens.sumOf { it.litros }
    val mediaGeral = if (totalLitros > 0) totalKm / totalLitros else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultados") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Resumo Geral", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Motorista: $motoristaSalvo", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Total Percorrido: %.1f km".format(totalKm))
                    Text("Total Gasto: R$ %.2f".format(totalGasto))
                    Text("Média Geral: %.2f km/L".format(mediaGeral))
                }
            }

            Text("Viagens Registradas", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viagens) { viagem ->
                    ItemViagem(viagem)
                }
            }

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nova Viagem")
            }
        }
    }
}

@Composable
fun ItemViagem(viagem: Viagem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Data: ${viagem.data}", fontWeight = FontWeight.Bold)
                Text("%.1f km".format(viagem.calcularKm()))
            }
            Text("Custo: R$ %.2f | Média: %.2f km/L".format(viagem.custoTotal(), viagem.mediaKmLitro()), fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ControleViagensPreview() {
    PersistenciaTheme {
        val navController = rememberNavController()
        val viagens = remember { mutableStateListOf<Viagem>() }
        CadastroScreen(navController, viagens)
    }
}
