/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import ViewServidor.GUIServidor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import org.json.JSONObject;

/**
 *
 * @author gabriel
 */
public class ServidorUDP {
    private static DatagramSocket socketServidor = null;
    private static int qtdSalas; 
    private int porta;
    private static ArrayList<JSONObject> infoSalas;
    private static ArrayList<ModelUsuarioConectado> usuariosConectados;
    private DefaultTableModel modeloTabelaSalas;
    private static DefaultTableModel modeloTabelaUsuarios;

    public ServidorUDP() {
        infoSalas = new ArrayList();
        usuariosConectados = new ArrayList();
    }
    
    public int getQtdSalas() {
        return qtdSalas;
    }
    
    public void setQtdSalas(int aQtdSalas) {
        qtdSalas = aQtdSalas;
    }
    
    public int getPorta() {
        return porta;
    }

    public void setPorta(int aPorta) {
        porta = aPorta;
    }
    
    public DefaultTableModel getModeloTabelaSalas() {
        return modeloTabelaSalas;
    }

    public void setModeloTabelaSalas(DefaultTableModel modeloTabela) {
        this.modeloTabelaSalas = modeloTabela;
    }
    
    public DefaultTableModel getModeloTabelaUsuarios() {
        return modeloTabelaUsuarios;
    }

    public void setModeloTabelaUsuarios(DefaultTableModel modeloTabelaUsuarios) {
        this.modeloTabelaUsuarios = modeloTabelaUsuarios;
    }
    
    public void initModeloTabelaSalas() {
        setModeloTabelaSalas(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"ID Sala", "Criador", "Descrição", "Inicio", "Fim"}));
    }
    
    public void initModeloTabelaUsuarios() {
        setModeloTabelaUsuarios(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"Nome", "IP", "Porta"}));
    }
    
    private void updateModeloTabelaSalas() {
        Object rowData[] = new Object[5];
        int index = 0;
        if (modeloTabelaSalas.getRowCount() > 0) {
            for (int i = modeloTabelaSalas.getRowCount() - 1; i > -1; i--) {
                modeloTabelaSalas.removeRow(i);
            }
        }
        while (index < infoSalas.size()) {
            rowData[0] = infoSalas.get(index).getInt("id");
            rowData[1] = infoSalas.get(index).getString("criador");
            rowData[2] = infoSalas.get(index).getString("descricao");
            rowData[3] = Long.toString(infoSalas.get(index).getLong("inicio"));
            rowData[4] = Long.toString(infoSalas.get(index).getLong("fim"));
            modeloTabelaSalas.addRow(rowData);
            index++;
        }
    }
    
    private static void updateModeloTabelaUsuarios() {
        String rowData[] = new String[3];
        int index = 0;
        if (modeloTabelaUsuarios.getRowCount() > 0) {
            for (int i = modeloTabelaUsuarios.getRowCount() - 1; i > -1; i--) {
                modeloTabelaUsuarios.removeRow(i);
            }
        }
        while (index < usuariosConectados.size()) {
            rowData[0] = usuariosConectados.get(index).getNome();
            rowData[1] = usuariosConectados.get(index).getEndrecoIP().toString();
            rowData[2] = Integer.toString(usuariosConectados.get(index).getPorta());
            modeloTabelaUsuarios.addRow(rowData);
            index++;
        }
    }
    
    public void init(){
        initModeloTabelaSalas();
        initModeloTabelaUsuarios();
        new Thread() {
            public void run() {
                try {
                     qtdSalas = contagemSalas();
                     updateModeloTabelaSalas();
                     socketServidor = new DatagramSocket(porta);
                     if (socketServidor != null) {
                         GUIServidor.buttonConfirmar.setEnabled(false);
                     }
                     System.out.println("Servidor aberto em : " + socketServidor.getLocalPort());
                     System.out.println("Numero de salas abertas: " + qtdSalas);
                     byte[] buffer = new byte[1024];
                     while (!socketServidor.isClosed()) {
                        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                        socketServidor.receive(request);
                        String received = new String(request.getData(),0,request.getLength());
                        JSONObject JSONReceived = new JSONObject(received);
                        System.out.println("Mensagem recebida: " + JSONReceived);
                        Integer tipo = JSONReceived.getInt("tipo");
                        switch (tipo) {
                            /*case -2: ping(); break;
                            case -1: erroMalFormada(); break;*/
                            case 0: checarLogin(request, JSONReceived); break;
                            /*case 1: loginErrado(); break;
                            case 2: loginSucedido(); break;*/
                            case 3: criarSala(request, JSONReceived); break;
                            /*case 4: atualizarListaSalas(); break;
                            case 5: acessoSala(); break;
                            case 6: historicoUsuariosSala(); break;
                            case 7: statusVotacao(); break;
                            case 8: mensagemChat(); break;
                            case 9: mensagemChatServidor(); break;
                            case 10: break;
                            case 11: respostaAcessoSala(); break;
                            case 12: respostaCriarSala(); break;
                            case 13: mensagemEspecifica(); break;
                            case 14: salaEspecifica(); break;
                            case 15: computarVoto(); break;
                            case 16: des-conectarSala(); break;*/
                        } 
                     }
                } catch (SocketException ex){} catch (FileNotFoundException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
    
    private int contagemSalas() {
        String rooms = "salas.txt";
        String line;
        Integer countSalas = 0;
        FileReader roomfile;
        try {
            roomfile = new FileReader(rooms);
            BufferedReader bufferedReader = new BufferedReader(roomfile);
            while((line = bufferedReader.readLine()) != null){
                infoSalas.add(new JSONObject(line));
                countSalas = countSalas + 1;
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        }
        return countSalas;
    }

    private static void checarLogin(DatagramPacket request, JSONObject received) {
        String fileName = "login.txt";
        String line;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            boolean flagEncontrado = false;
            while((line = bufferedReader.readLine()) != null) {
                JSONObject user = new JSONObject(line);
                if (user.getString("ra").equals(received.getString("ra"))) {
                    flagEncontrado = true;
                    if (user.getString("senha").equals(received.getString("senha"))) {
                        byte[] buffer = new byte[1024];
                        JSONObject JSONRespostaLoginSucedido = new JSONObject();
                        JSONRespostaLoginSucedido.put("tipo", 2);
                        JSONRespostaLoginSucedido.put("nome", user.getString("nome"));
                        JSONRespostaLoginSucedido.put("tamanho", qtdSalas);
                        buffer = JSONRespostaLoginSucedido.toString().getBytes();
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        socketServidor.send(reply);
                        ModelUsuarioConectado novoUsuario = new ModelUsuarioConectado(user.getString("nome"), request.getAddress(), request.getPort());
                        usuariosConectados.add(novoUsuario);
                        updateModeloTabelaUsuarios();
                        streamSalas(request.getAddress(), request.getPort());
                        break;
                    } else {
                        byte[] buffer = new byte[1024];
                        JSONObject JSONRespostaLoginFalho = new JSONObject();
                        JSONRespostaLoginFalho.put("tipo", 1);
                        buffer = JSONRespostaLoginFalho.toString().getBytes();
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        socketServidor.send(reply);
                        break;
                    }
                }
            }
            if (flagEncontrado == false) {
                byte[] buffer = new byte[1024];
                        JSONObject JSONRespostaLoginSucedido = new JSONObject();
                        JSONRespostaLoginSucedido.put("tipo", 1);
                        buffer = JSONRespostaLoginSucedido.toString().getBytes();
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        socketServidor.send(reply);
            }
            bufferedReader.close();         
        }
        catch (FileNotFoundException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void streamSalas(InetAddress address, int port) {
        byte[] buffer = new byte[1024];
        for (JSONObject s : infoSalas) {
            try {
                s.put("tipo", 11);
                buffer = s.toString().getBytes();
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length, address, port);
                socketServidor.send(reply);
            } catch (IOException ex) {
                Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private static void criarSala(DatagramPacket request, JSONObject received) {
        qtdSalas++;
        received.put("id", qtdSalas);
        String nomeCriador = null;
        int index = 0;
        if (modeloTabelaUsuarios.getRowCount() > 0) {
            while (index < usuariosConectados.size()) {
                if (usuariosConectados.get(index).getEndrecoIP() == request.getAddress()) {
                    nomeCriador = usuariosConectados.get(index).getNome();
                } else {
                    index++;
                }
            }
        }
        received.put("criador", nomeCriador);
        received.put("inicio", "agora no tempo do servidor");
        received.put("status", true);
        received.put("mensagens", 0);
        try {
            FileWriter escritaArquivo = new FileWriter("salas.txt", true);
            BufferedWriter saida = new BufferedWriter(escritaArquivo);
            saida.write("\n" + received);
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        }
        infoSalas.add(received);
        byte[] buffer = new byte[1024];
        received.put("tipo", 11);
        buffer = received.toString().getBytes();
        for (ModelUsuarioConectado u : usuariosConectados) {
            try {
                DatagramPacket atualizacaoListaSalas = new DatagramPacket(buffer, buffer.length, u.getEndrecoIP(), u.getPorta());
                socketServidor.send(atualizacaoListaSalas);
            } catch (IOException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
