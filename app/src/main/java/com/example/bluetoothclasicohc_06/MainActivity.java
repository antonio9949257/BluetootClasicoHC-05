package com.example.bluetoothclasicohc_06;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final UUID MI_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter adaptadorBluetooth;
    BluetoothDevice dispositivoSeleccionado;
    BluetoothSocket socketBluetooth;
    OutputStream flujoSalida;
    InputStream flujoEntrada;
    ArrayList<String> listaDispositivos = new ArrayList<>();
    boolean estaConectado = false;
    TextView valorPotenciometro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listaVistaDispositivos = findViewById(R.id.listaDispositivos);
        Button botonConectar = findViewById(R.id.botonConectar);
        Button botonDesconectar = findViewById(R.id.botonDesconectar);
        Button botonEnviarOn = findViewById(R.id.botonEncender);
        Button botonEnviarOff = findViewById(R.id.botonApagar);
        TextView estadoConexion = findViewById(R.id.estadoConexion);
        valorPotenciometro = findViewById(R.id.valorPotenciometro);

        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaDispositivos);
        listaVistaDispositivos.setAdapter(adaptador);

        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> dispositivosEmparejados = adaptadorBluetooth.getBondedDevices();
        List<BluetoothDevice> listaDispositivosEmparejados = new ArrayList<>(dispositivosEmparejados);

        for (int i = 0; i < listaDispositivosEmparejados.size(); i++) {
            BluetoothDevice dispositivo = listaDispositivosEmparejados.get(i);
            listaDispositivos.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
        }

        adaptador.notifyDataSetChanged();

        botonConectar.setEnabled(false);
        botonDesconectar.setEnabled(false);
        botonEnviarOn.setEnabled(false);
        botonEnviarOff.setEnabled(false);

        listaVistaDispositivos.setOnItemClickListener((parent, view, position, id) -> {
            String direccionMAC = listaDispositivos.get(position).split("\n")[1];
            dispositivoSeleccionado = adaptadorBluetooth.getRemoteDevice(direccionMAC);
            estadoConexion.setText("Seleccionado: " + dispositivoSeleccionado.getName());

            botonConectar.setEnabled(true);
        });

        botonConectar.setOnClickListener(v -> new Thread(() -> {
            try {
                socketBluetooth = dispositivoSeleccionado.createRfcommSocketToServiceRecord(MI_UUID);
                socketBluetooth.connect();
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

                new Thread(() -> {
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
                            break;
                        }
                    }
                }).start();

            } catch (IOException e) {
                runOnUiThread(() -> estadoConexion.setText("Error de conexión"));
            }
        }).start());

        botonDesconectar.setOnClickListener(v -> {
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
                estadoConexion.setText("Error al desconectar");
            }
        });

        botonEnviarOn.setOnClickListener(v -> {
            try {
                flujoSalida.write("E".getBytes());
            } catch (IOException e) {
            }
        });
        botonEnviarOff.setOnClickListener(v ->{
            try {
                flujoSalida.write("A".getBytes());
            } catch (IOException e) {
            }
        });
    }
}