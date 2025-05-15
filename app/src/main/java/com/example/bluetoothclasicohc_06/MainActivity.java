package com.example.bluetoothclasicohc_06;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
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

    final UUID UUID_ARDUINO_SERVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter adaptadorBluetooth;
    BluetoothDevice dispositivoSeleccionado;
    BluetoothSocket socketBluetooth;
    OutputStream flujoSalida;
    InputStream flujoEntrada;
    ArrayList<String> listaDispositivos = new ArrayList<>();
    boolean estaConectado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listaDispositivosj = findViewById(R.id.listaDispositivos);
        Button botonConectarj = findViewById(R.id.botonConectar);
        Button botonDesconectarj = findViewById(R.id.botonDesconectar);
        Button botonEncenderj = findViewById(R.id.botonEncender);
        Button botonApagarj = findViewById(R.id.botonApagar);
        TextView estadoConexionj = findViewById(R.id.estadoConexion);
        TextView valorPotenciometroj = findViewById(R.id.valorPotenciometro);

        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaDispositivos);
        listaDispositivosj.setAdapter(adaptador);

        adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> dispositivosEmparejados = adaptadorBluetooth.getBondedDevices();
        List<BluetoothDevice> listaDispositivosEmparejados = new ArrayList<>(dispositivosEmparejados);

        for (int i = 0; i < listaDispositivosEmparejados.size(); i++) {
            BluetoothDevice dispositivo = listaDispositivosEmparejados.get(i);
            listaDispositivos.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
        }

        adaptador.notifyDataSetChanged();

        botonConectarj.setEnabled(false);
        botonDesconectarj.setEnabled(false);
        botonEncenderj.setEnabled(false);
        botonApagarj.setEnabled(false);

        listaDispositivosj.setOnItemClickListener((parent, view, position, id) -> {
            String direccionMAC = listaDispositivos.get(position).split("\n")[1];
            dispositivoSeleccionado = adaptadorBluetooth.getRemoteDevice(direccionMAC);
            estadoConexionj.setText("Seleccionado: " + dispositivoSeleccionado.getName());

            botonConectarj.setEnabled(true);
        });

        botonConectarj.setOnClickListener(v -> new Thread(() -> {
            try {
                socketBluetooth = dispositivoSeleccionado.createRfcommSocketToServiceRecord(UUID_ARDUINO_SERVICE);
                socketBluetooth.connect();
                flujoSalida = socketBluetooth.getOutputStream();
                flujoEntrada = socketBluetooth.getInputStream();
                estaConectado = true;

                runOnUiThread(() -> {
                    estadoConexionj.setText("Conectado");
                    botonConectarj.setEnabled(false);
                    botonDesconectarj.setEnabled(true);
                    botonEncenderj.setEnabled(true);
                    botonApagarj.setEnabled(true);
                });

                new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    int bytes;

                    while (estaConectado) {
                        try {
                            bytes = flujoEntrada.read(buffer);
                            String datosRecibidos = new String(buffer, 0, bytes);

                            runOnUiThread(() -> {
                                valorPotenciometroj.setText("Valor: " + datosRecibidos.trim());
                            });

                        } catch (IOException e) {
                            break;
                        }
                    }
                }).start();

            } catch (IOException e) {
                runOnUiThread(() -> estadoConexionj.setText("Error de conexión"));
            }
        }).start());

        botonDesconectarj.setOnClickListener(v -> {
            try {
                socketBluetooth.close();
                estaConectado = false;
                runOnUiThread(() -> {
                    estadoConexionj.setText("Desconectado");
                    botonConectarj.setEnabled(true);
                    botonDesconectarj.setEnabled(false);
                    botonEncenderj.setEnabled(false);
                    botonApagarj.setEnabled(false);
                    valorPotenciometroj.setText("Valor: --");
                });
            } catch (IOException e) {
                estadoConexionj.setText("Error al desconectar");
            }
        });

        botonEncenderj.setOnClickListener(v -> {
            try {
                flujoSalida.write("E".getBytes());
            } catch (IOException e) {
            }
        });
        botonApagarj.setOnClickListener(v ->{
            try {
                flujoSalida.write("A".getBytes());
            } catch (IOException e) {
            }
        });
    }
}