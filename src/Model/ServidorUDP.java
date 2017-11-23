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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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
    private int porta;
    private static ArrayList<ModelSalas> infoSalas;
    private static ArrayList<ModelUsuarioConectado> usuariosConectados;
    private static DefaultTableModel modeloTabelaSalas;
    private static DefaultTableModel modeloTabelaUsuarios;

    public ServidorUDP() {
        infoSalas = new ArrayList();
        usuariosConectados = new ArrayList();
        inicializarSalas();
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
        ServidorUDP.modeloTabelaSalas = modeloTabela;
    }
    
    public DefaultTableModel getModeloTabelaUsuarios() {
        return modeloTabelaUsuarios;
    }

    public void setModeloTabelaUsuarios(DefaultTableModel modeloTabelaUsuarios) {
        ServidorUDP.modeloTabelaUsuarios = modeloTabelaUsuarios;
    }
    
    public void initModeloTabelaSalas() {
        setModeloTabelaSalas(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"ID Sala", "Criador", "Descrição", "Inicio", "Fim"}));
    }
    
    public void initModeloTabelaUsuarios() {
        setModeloTabelaUsuarios(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"Nome", "IP", "Porta"}));
    }
    
    private static void updateModeloTabelaSalas() {
        Object rowData[] = new Object[5];
        int index = 0;
        if (modeloTabelaSalas.getRowCount() > 0) {
            for (int i = modeloTabelaSalas.getRowCount() - 1; i > -1; i--) {
                modeloTabelaSalas.removeRow(i);
            }
        }
        
        while (index < infoSalas.size()) {
            rowData[0] = infoSalas.get(index).getInfoSalas().getInt("id");
            rowData[1] = infoSalas.get(index).getInfoSalas().getString("criador");
            rowData[2] = infoSalas.get(index).getInfoSalas().getString("descricao");
            /*long inicio = Long.valueOf(infoSalas.get(index).getInfoSalas().getString("inicio"));
            rowData[3] = formatarData(inicio);
            long fim = Long.valueOf(infoSalas.get(index).getInfoSalas().getString("fim"));
            rowData[4] = formatarData(fim);*/
            rowData[3] = infoSalas.get(index).getInfoSalas().getString("inicio");
            rowData[4] = infoSalas.get(index).getInfoSalas().getString("fim");
            modeloTabelaSalas.addRow(rowData);
            index++;
        }
    }
    
    private static String formatarData(Long unixTimestamp) {
        SimpleDateFormat formatoData = new SimpleDateFormat("dd-MM-yyyy");
        Date data = new Date(unixTimestamp * 1000L);
        String dataFormatada = formatoData.format(data);
        return dataFormatada;
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
                updateModeloTabelaSalas();
                try {
                    socketServidor = new DatagramSocket(porta);
                } catch (SocketException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (socketServidor != null) {
                    GUIServidor.buttonConfirmar.setEnabled(false);
                    GUIServidor.textfieldPorta.setEditable(false);
                }
                System.out.println("Servidor aberto em : " + socketServidor.getLocalPort());
                System.out.println("Numero de salas abertas: " + infoSalas.size());
                byte[] buffer = new byte[1024];
                while (!socketServidor.isClosed()) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    try {
                        socketServidor.receive(request);
                    } catch (IOException ex) {
                        Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    String received = new String(request.getData(),0,request.getLength());
                    JSONObject JSONReceived = new JSONObject(received);
                    System.out.println("Mensagem recebida: " + JSONReceived);
                    if (JSONReceived.has("tipo") == false) {
                        mensagemMalFormada(request, JSONReceived);
                    }
                    Integer tipo = JSONReceived.getInt("tipo");
                    switch (tipo) {
                        case 0 : checarLogin(request, JSONReceived); break;
                        //1 = loginErrado(); break;
                        //2 = loginSucedido(); break;
                        case 3: logoutServidor(request, JSONReceived); break;
                        //4 = enviarSala(); break;
                        case 5: pedidoSalaEspecifica(request, JSONReceived); break;
                        case 6: criarSala(request, JSONReceived); break;
                        case 7: acessoSala(request, JSONReceived); break;
                        //8 = informacoesSala(); break;
                        //9 = statusVotacao(); break;
                        //10 = conexaoChat(); break;
                        //11 = logoutSala(); break; 
                        //case 12: mensagemChat(request, JSONReceived); break;
                        //13 = pedidoMensagemEspecifica(); break;
                        //14 = mensagemChatServidor(); break;
                        //15 = computarVoto(); break;
                        //16 = ping(); break;
                        default: mensagemMalFormada(request, JSONReceived); break;
                    } 
                }
            }
        }.start();
    }
    
    private void mensagemMalFormada(DatagramPacket request, JSONObject JSONReceived) {
        JSONObject JSONErro = new JSONObject();
        JSONErro.put("pacote", JSONReceived.toString());
        JSONErro.put("tipo", -1);        
        try {
            byte[] buffer = new byte[1024];
            buffer = JSONErro.toString().getBytes();
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
            socketServidor.send(reply);
            System.out.println("mensagem enviada: " + JSONErro);
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void inicializarSalas() {
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./salas/salas"));
            line = bufferedReader.readLine();
            while(line != null){
                ModelSalas novaSala = new ModelSalas();
                JSONObject info = new JSONObject(line);
                novaSala.setInfoSalas(info);
                String arquivoMensagens = "./salas/mensagens/".concat(Integer.toString(info.getInt("id")));
                int countMsg = 0;
                BufferedReader bufferedReaderMsg = new BufferedReader(new FileReader(arquivoMensagens));
                String mensagem = bufferedReaderMsg.readLine();
                ArrayList<JSONObject> mensagens = new ArrayList<>();
                while (mensagem != null) {
                    mensagens.add(new JSONObject(mensagem));
                    countMsg = countMsg + 1;
                    mensagem = bufferedReaderMsg.readLine();
                }
                novaSala.setMensagens(mensagens);
                infoSalas.add(novaSala);
                line = bufferedReader.readLine();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void checarLogin(DatagramPacket request, JSONObject received) {
        String fileName = "loginhash.txt";
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
                        JSONRespostaLoginSucedido.put("tamanho", infoSalas.size());
                        buffer = JSONRespostaLoginSucedido.toString().getBytes();
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        socketServidor.send(reply);
                        System.out.println("mensagem enviada: " + JSONRespostaLoginSucedido);
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
                        System.out.println("mensagem enviada: " + JSONRespostaLoginFalho);
                        break;
                    }
                }
            }
            if (flagEncontrado == false) {
                byte[] buffer = new byte[1024];
                JSONObject JSONRespostaLoginErro = new JSONObject();
                JSONRespostaLoginErro.put("tipo", 1);
                buffer = JSONRespostaLoginErro.toString().getBytes();
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                socketServidor.send(reply);
                System.out.println("mensagem enviada: " + JSONRespostaLoginErro);
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
        new Thread() {
            public void run() {
                byte[] buffer = new byte[1024];
                for (ModelSalas s : infoSalas) {
                    JSONObject informacoes = s.getInfoSalas();
                    informacoes.put("tipo", 4);
                    informacoes.put("tamanho", infoSalas.size());
                    informacoes.remove("usuarios");
                    buffer = informacoes.toString().getBytes();
                    try {
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, address, port);
                        socketServidor.send(reply);
                        System.out.println("mensagem enviada: " + informacoes);
                    } catch (IOException ex) {
                        Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }
    
    private static void pedidoSalaEspecifica(DatagramPacket request, JSONObject received) {
        for (ModelSalas s : infoSalas) {
            if (s.getInfoSalas().getInt("id") == received.getInt("id")) {
                JSONObject salaEspecifica = new JSONObject(s.getInfoSalas());
                salaEspecifica.put("tipo", 4);
                byte[] buffer = new byte[1024];
                buffer = salaEspecifica.toString().getBytes();
                try {
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                    socketServidor.send(reply);
                    System.out.println("mensagem enviada: " + salaEspecifica);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }
        }
    }
    
    private static void criarSala(DatagramPacket request, JSONObject received) {
        received.remove("tipo");
        received.put("id", infoSalas.size());
        String criador = null;
        for (ModelUsuarioConectado u : usuariosConectados) {
            if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                criador = u.getNome();
            }
        }
        received.put("criador", criador);
        received.put("inicio", Long.toString(Instant.now().getEpochSecond()));
        received.put("status", true);
        salvarSalaArquivo(received);
        criarArquivoSala(infoSalas.size());
        ModelSalas novaSala = new ModelSalas();
        novaSala.setInfoSalas(received);
        infoSalas.add(novaSala);
        received.put("tamanho", infoSalas.size());
        received.put("tipo", 4);
        byte[] buffer = new byte[1024];
        buffer = received.toString().getBytes();
        broadcast(buffer);
        System.out.println("mensagem enviada: " + received);
        updateModeloTabelaSalas();
    }
    
    public void logoutServidor(DatagramPacket request, JSONObject received) {
        Iterator<ModelUsuarioConectado> iterator = usuariosConectados.iterator();
        ModelUsuarioConectado u = null;
        System.out.println("procurando cliente logout");
        while (iterator.hasNext()) {
            u = iterator.next();
            if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                System.out.println("cliente encontrado");
                iterator.remove();
                System.out.println("cliente removido");
            }
        }
        updateModeloTabelaUsuarios();
        System.out.println("tabela atualizada");
    }
    
    //terminar
    public void acessoSala(DatagramPacket request, JSONObject received) {
        Iterator<ModelSalas> iterator = infoSalas.iterator();
        System.out.println("procurando sala");
        while (iterator.hasNext()) {
            ModelSalas s = iterator.next();
            if (s.getInfoSalas().getInt("id") == received.getInt("id")) {
                System.out.println("sala encontrada, enviando informações da sala...");
                ModelUsuarioConectado novoAcesso = null;
                for (ModelUsuarioConectado u : usuariosConectados) {
                    if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                        novoAcesso = new ModelUsuarioConectado(u.getNome(), u.getEndrecoIP(), u.getPorta());
                        break;
                    }
                }
                s.addUsuariosConectados(novoAcesso);
                streamMensagensSala(request, s.getMensagens());
            }
        }
    }
    
    //terminar
    private void streamMensagensSala(DatagramPacket request, ArrayList<JSONObject> mensagens) {
        new Thread() {
            public void run() {
                for (JSONObject m : mensagens) {
                    m.put("tipo", 12);
                    m.put("tamanho", mensagens.size());
                    byte[] buffer = new byte[1024];
                    buffer = m.toString().getBytes();
                    try {
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        socketServidor.send(reply);
                        System.out.println("mensagem de chat enviada: " + m);
                    } catch (IOException ex) {
                        Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }
    
    //terminar
    private void mensagemChat(DatagramPacket request, JSONObject received) {
        received.remove("tipo");
        salvarMensagemSalaArquivo(received);
        received.put("timestamp", Long.toString(Instant.now().getEpochSecond()));
    }
    
    private static void salvarMensagemSalaArquivo(JSONObject mensagem) {
        
    }
    
    private static void salvarSalaArquivo(JSONObject sala) {
        BufferedWriter saida = null;
        try {
            saida = new BufferedWriter(new FileWriter("./salas/salas", true));
            saida.write(sala.toString());
            saida.flush();
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (saida != null) {
                try {
                    saida.close();
                } catch (IOException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private static void criarArquivoSala(int id) {
        String data = "";
        try {
            Files.write(Paths.get("./salas/mensagens/" + Integer.toString(id)), data.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void broadcast(byte[] buffer) {
        for (ModelUsuarioConectado u : usuariosConectados) {
            try {
                DatagramPacket atualizacaoListaSalas = new DatagramPacket(buffer, buffer.length, u.getEndrecoIP(), u.getPorta());
                socketServidor.send(atualizacaoListaSalas);
            } catch (IOException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
