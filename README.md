# Guía Completa de Aplicación Bluetooth para Android con Arduino

## Introducción

Esta guía explica paso a paso cómo crear una aplicación Android para comunicarse con dispositivos Bluetooth como el módulo HC-05, comúnmente usado con Arduino. El proyecto permite:

1.  Listar dispositivos Bluetooth emparejados
    
2.  Establecer conexión con un módulo HC-05
    
3.  Enviar comandos para controlar un dispositivo
    
4.  Recibir y mostrar valores de sensores (como un potenciómetro)
    

## Estructura del Proyecto

### 1\. Archivo AndroidManifest.xml

```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permisos esenciales para Bluetooth -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <!-- Necesario para descubrir dispositivos desde Android 6.0 -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
        tools:ignore="CoarseFineLocation" />
    <!-- Permisos específicos para Android 12+ -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

    <!-- Garantiza que la app solo se instale en dispositivos con Bluetooth -->
    <uses-feature android:name="android.hardware.bluetooth" android:required="true" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**Explicación de permisos**:

- `BLUETOOTH`: Permite operaciones básicas de Bluetooth
    
- `BLUETOOTH_ADMIN`: Permite descubrir dispositivos y cambiar configuraciones
    
- `ACCESS_FINE_LOCATION`: Requerido desde Android 6.0 para descubrir dispositivos
    
- `BLUETOOTH_CONNECT/SCAN`: Nuevos permisos en Android 12+
    

### 2\. Diseño de Interfaz (activity_main.xml)

```
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <!-- Estado de conexión -->
    <TextView
        android:id="@+id/estadoConexion"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="No conectado"
        android:textSize="16sp"
        android:gravity="center"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- Lista de dispositivos -->
    <TextView
        android:id="@+id/tituloDispositivos"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Dispositivos emparejados:"
        app:layout_constraintTop_toBottomOf="@id/estadoConexion"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <ListView
        android:id="@+id/listaDispositivos"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/tituloDispositivos"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/botonesConexion"/>

    <!-- Botones de conexión -->
    <LinearLayout
        android:id="@+id/botonesConexion"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/valorPotenciometro">

        <Button
            android:id="@+id/botonConectar"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Conectar"
            android:enabled="false"/>

        <Button
            android:id="@+id/botonDesconectar"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Desconectar"
            android:enabled="false"/>
    </LinearLayout>

    <!-- Visualización de datos recibidos -->
    <TextView
        android:id="@+id/valorPotenciometro"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Valor: --"
        android:textSize="20sp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/botonesControl"/>

    <!-- Botones de control -->
    <LinearLayout
        android:id="@+id/botonesControl"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent">

        <Button
            android:id="@+id/botonEncender"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Encender (1)"
            android:enabled="false"/>

        <Button
            android:id="@+id/botonApagar"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Apagar (0)"
            android:enabled="false"/>
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

![Screenshot from 2025-05-13 02-13-50.png](:/f35a812a997c437ea9d7cabd26ee3f92)

**Características de la interfaz**:

- Diseño responsivo con ConstraintLayout
    
- Lista de dispositivos emparejados
    
- Indicador de estado de conexión
    
- Botones para controlar la conexión
    
- Visualización de datos recibidos
    
- Controles para enviar comandos
    

### 3\. Lógica Principal (MainActivity.java)

```
public class MainActivity extends AppCompatActivity {
    // UUID estándar para SPP (Serial Port Profile)
    final UUID MI_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // Componentes Bluetooth
    BluetoothAdapter adaptadorBluetooth;
    BluetoothDevice dispositivoSeleccionado;
    BluetoothSocket socketBluetooth;
    OutputStream flujoSalida;
    InputStream flujoEntrada;
    
    // Componentes de UI
    ArrayList<String> listaDispositivos = new ArrayList<>();
    TextView valorPotenciometro;
    boolean estaConectado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de vistas
        ListView listaVistaDispositivos = findViewById(R.id.listaDispositivos);
        Button botonConectar = findViewById(R.id.botonConectar);
        Button botonDesconectar = findViewById(R.id.botonDesconectar);
        Button botonEnviarOn = findViewById(R.id.botonEncender);
        Button botonEnviarOff = findViewById(R.id.botonApagar);
        TextView estadoConexion = findViewById(R.id.estadoConexion);
        valorPotenciometro = findViewById(R.id.valorPotenciometro);

        // Configurar adaptador para la lista
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_1, listaDispositivos);
        listaVistaDispositivos.setAdapter(adaptador);

        // Obtener adaptador Bluetooth
        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (adaptadorBluetooth == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Listar dispositivos emparejados
        listarDispositivosEmparejados(adaptador);

        // Configurar listeners
        configurarListeners(listaVistaDispositivos, botonConectar, botonDesconectar, 
                          botonEnviarOn, botonEnviarOff, estadoConexion, adaptador);
    }

    private void listarDispositivosEmparejados(ArrayAdapter<String> adaptador) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
            == PackageManager.PERMISSION_GRANTED) {
            
            Set<BluetoothDevice> dispositivosEmparejados = adaptadorBluetooth.getBondedDevices();
            listaDispositivos.clear();
            
            if (dispositivosEmparejados.size() > 0) {
                for (BluetoothDevice dispositivo : dispositivosEmparejados) {
                    listaDispositivos.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
                }
            } else {
                listaDispositivos.add("No hay dispositivos vinculados");
            }
            adaptador.notifyDataSetChanged();
        } else {
            // Solicitar permisos si no están concedidos
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 
                REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private void configurarListeners(ListView listaVistaDispositivos, Button botonConectar,
                                   Button botonDesconectar, Button botonEnviarOn, 
                                   Button botonEnviarOff, TextView estadoConexion,
                                   ArrayAdapter<String> adaptador) {
        
        // Selección de dispositivo
        listaVistaDispositivos.setOnItemClickListener((parent, view, position, id) -> {
            if (!listaDispositivos.get(position).equals("No hay dispositivos vinculados")) {
                String direccionMAC = listaDispositivos.get(position).split("\n")[1];
                dispositivoSeleccionado = adaptadorBluetooth.getRemoteDevice(direccionMAC);
                estadoConexion.setText("Seleccionado: " + dispositivoSeleccionado.getName());
                botonConectar.setEnabled(true);
            }
        });

        // Conexión al dispositivo
        botonConectar.setOnClickListener(v -> conectarDispositivo(estadoConexion, botonConectar, 
                                botonDesconectar, botonEnviarOn, botonEnviarOff));

        // Desconexión
        botonDesconectar.setOnClickListener(v -> desconectarDispositivo(estadoConexion, 
                                botonConectar, botonDesconectar, botonEnviarOn, botonEnviarOff));

        // Envío de comandos
        botonEnviarOn.setOnClickListener(v -> enviarComando("E"));
        botonEnviarOff.setOnClickListener(v -> enviarComando("A"));
    }

    private void conectarDispositivo(TextView estadoConexion, Button botonConectar,
                                   Button botonDesconectar, Button botonEnviarOn, 
                                   Button botonEnviarOff) {
        new Thread(() -> {
            try {
                // Crear socket RFCOMM
                socketBluetooth = dispositivoSeleccionado.createRfcommSocketToServiceRecord(MI_UUID);
                adaptadorBluetooth.cancelDiscovery(); // Cancelar descubrimiento para ahorrar energía
                socketBluetooth.connect();
                
                // Obtener streams de entrada/salida
                flujoSalida = socketBluetooth.getOutputStream();
                flujoEntrada = socketBluetooth.getInputStream();
                estaConectado = true;

                runOnUiThread(() -> {
                    estadoConexion.setText("Conectado");
                    botonConectar.setEnabled(false);
                    botonDesconectar.setEnabled(true);
                    botonEnviarOn.setEnabled(true);
                    botonEnviarOff.setEnabled(true);
                });

                // Hilo para recibir datos
                new Thread(this::recibirDatos).start();

            } catch (IOException e) {
                runOnUiThread(() -> estadoConexion.setText("Error de conexión"));
            }
        }).start();
    }

    private void recibirDatos() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (estaConectado) {
            try {
                bytes = flujoEntrada.read(buffer);
                String datosRecibidos = new String(buffer, 0, bytes);
                
                runOnUiThread(() -> {
                    valorPotenciometro.setText("Valor: " + datosRecibidos.trim());
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    valorPotenciometro.setText("Valor: --");
                    estaConectado = false;
                });
                break;
            }
        }
    }

    private void desconectarDispositivo(TextView estadoConexion, Button botonConectar,
                                      Button botonDesconectar, Button botonEnviarOn, 
                                      Button botonEnviarOff) {
        try {
            socketBluetooth.close();
            estaConectado = false;
            
            runOnUiThread(() -> {
                estadoConexion.setText("Desconectado");
                botonConectar.setEnabled(true);
                botonDesconectar.setEnabled(false);
                botonEnviarOn.setEnabled(false);
                botonEnviarOff.setEnabled(false);
                valorPotenciometro.setText("Valor: --");
            });
            
        } catch (IOException e) {
            runOnUiThread(() -> estadoConexion.setText("Error al desconectar"));
        }
    }

    private void enviarComando(String comando) {
        if (estaConectado) {
            new Thread(() -> {
                try {
                    flujoSalida.write(comando.getBytes());
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, 
                        "Error al enviar comando", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (estaConectado) {
            try {
                socketBluetooth.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

## Conceptos Clave Explicados

### 1\. UUID Bluetooth

El UUID `00001101-0000-1000-8000-00805F9B34FB` es el identificador estándar para el perfil SPP (Serial Port Profile), que emula una conexión serial sobre Bluetooth, compatible con módulos como el HC-05/HC-06.

### 2\. Flujo de Conexión Bluetooth

1.  **Obtener adaptador Bluetooth**: `BluetoothAdapter.getDefaultAdapter()`
    
2.  **Listar dispositivos emparejados**: `getBondedDevices()`
    
3.  **Establecer conexión**:
    
    - Crear socket: `createRfcommSocketToServiceRecord()`
        
    - Conectar: `socket.connect()`
        
    - Obtener streams: `getInputStream()` y `getOutputStream()`
        

### 3\. Manejo de Hilos

Las operaciones Bluetooth deben ejecutarse en hilos secundarios porque:

- `connect()` puede bloquearse por varios segundos
    
- `read()` es una operación bloqueante
    
- Evitamos congelar la interfaz de usuario
    

### 4\. Protocolo de Comunicación

- **Envío**: Comandos simples como "E" (Encender) y "A" (Apagar)
    
- **Recepción**: Valores numéricos (ej. de potenciómetro) como cadenas de texto
    

## Configuración del Lado Arduino

Para que funcione con un módulo HC-06, el sketch de Arduino sería similar a:

arduino

Copy

Download

```
char comando;
int valor;

void setup() {
  pinMode(13, OUTPUT);
  Serial.begin(9600);
}

void loop() {

  valor = analogRead(A0); 
  Serial.println(valor); 
  delay(400);

  if (Serial.available()) {
    comando = Serial.read();
    if (comando == 'E') digitalWrite(13, HIGH);
    else if (comando == 'A') digitalWrite(13, LOW); 
  }
}
```
