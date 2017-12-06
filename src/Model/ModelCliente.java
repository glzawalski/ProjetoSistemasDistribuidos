/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import View.ViewDiscussao;
import View.ViewLogin;
import View.ViewPrincipal;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author gabriel
 */
public class ModelCliente extends Thread {
    private static DatagramSocket socketCliente = null;
    private static int qtdSalas; 
    private int porta;
    private int id_Sala;
    private InetAddress host;
    private String senha, login, username;
    private static ArrayList<JSONObject> infoSalas;
    private static ArrayList<JSONObject> usuariosConectados;
    private static ArrayList<JSONObject> votosCompletos;
    private static ArrayList<String> opcoesVotos;
    private static ArrayList<Integer> idsMensagens;
    private static DefaultTableModel modeloTabelaSalas;
    private static DefaultTableModel modeloTabelaUsuarios;
    private static DefaultTableModel modeloTabelaVotos;
    private static ViewPrincipal GUICliente;
    private static ViewDiscussao viewdiscussao;
    private StyledDocument doc;
    private boolean pingVar;
    
    public ModelCliente(ViewPrincipal view, ViewDiscussao discussao) {
        infoSalas = new ArrayList();
        usuariosConectados = new ArrayList();
        votosCompletos = new ArrayList();
        GUICliente = view;
        viewdiscussao = discussao;
    }
    
    public void setSocket(DatagramSocket aSocket){
        socketCliente = aSocket;
    }
    
    public DatagramSocket getSocket(){
        return socketCliente;
    }
    
    public int getQtdSalas() {
        return qtdSalas;
    }
    
    public void setQtdSalas(int aQtdSalas) {
        qtdSalas = aQtdSalas;
    }
    
    public int getIdSala() {
        return id_Sala;
    }
    
    public void setIdSala(int aid_sala) {
        id_Sala = aid_sala;
    }
    
    public int getPorta() {
        return porta;
    }
    
    public void setHost(InetAddress aHost) {
        host = aHost;
    }
    
    public InetAddress getHost() {
        return host;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
    
    public void setModeloTabelaVotos(DefaultTableModel modeloTabela) {
        this.modeloTabelaVotos = modeloTabela;
    }

    public void setModeloTabelaUsuarios(DefaultTableModel modeloTabelaUsuarios) {
        this.modeloTabelaUsuarios = modeloTabelaUsuarios;
    }
    
    public void initModeloTabelaSalas() {
        setModeloTabelaSalas(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"ID Sala", "Criador", "Nome", "Descrição", "Inicio", "Fim"}));
    }
    
    public void initModeloTabelaUsuarios() {
        setModeloTabelaUsuarios(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"Nome","RA"}));
    }
    
    public void initModeloTabelaVotos() {
        setModeloTabelaVotos(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"Opção","Votos"}));
    }
    
    private static void updateModeloTabelaSalas() {
        Object rowData[] = new Object[6];
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        String stringdata;
        int index = 0;
        if (modeloTabelaSalas.getRowCount() > 0) {
            for (int i = modeloTabelaSalas.getRowCount() - 1; i > -1; i--) {
                modeloTabelaSalas.removeRow(i);
            }
        }

        while (index < infoSalas.size()) {
            rowData[0] = infoSalas.get(index).getInt("id");
            rowData[1] = infoSalas.get(index).getString("criador");
            rowData[2] = infoSalas.get(index).getString("nome");
            rowData[3] = infoSalas.get(index).getString("descricao");
            Long timestamp = Long.valueOf(infoSalas.get(index).getString("inicio"));
            Date date = new Date(timestamp * 1000L);
            stringdata = df.format(date);
            rowData[4] = stringdata;
            timestamp = Long.valueOf(infoSalas.get(index).getString("fim"));
            date = new Date(timestamp * 1000L);
            stringdata = df.format(date);
            rowData[5] = stringdata;
            modeloTabelaSalas.addRow(rowData);
            index++;
        }
    }
    
    private static void updateModeloTabelaUsuarios() {
        String rowData[] = new String[2];
        int index = 0;
        if (modeloTabelaUsuarios.getRowCount() > 0) {
            for (int i = modeloTabelaUsuarios.getRowCount() - 1; i > -1; i--) {
                modeloTabelaUsuarios.removeRow(i);
            }
        }

        while (index < usuariosConectados.size()) {
            rowData[0] = usuariosConectados.get(index).getString("nome");
            rowData[1] = usuariosConectados.get(index).getString("ra");
            modeloTabelaUsuarios.addRow(rowData);
            index++;
        }
    }
    
    private static void updateModeloTabelaVotos() {
        Object rowData[] = new Object[2];
        int index = 0;
        if (modeloTabelaVotos.getRowCount() > 0) {
            for (int i = modeloTabelaVotos.getRowCount() - 1; i > -1; i--) {
                modeloTabelaVotos.removeRow(i);
            }
        }

        while (index < votosCompletos.size()) {
            rowData[0] = votosCompletos.get(index).getString("nome");
            rowData[1] = votosCompletos.get(index).getInt("votos");
            modeloTabelaVotos.addRow(rowData);
            index++;
        }
    }
    
    private void setPingVar(boolean ping){
        this.pingVar = ping;
    }
    
    public void setViewDiscussao(ViewDiscussao view){
        viewdiscussao = view;
    }
    
    public void init(){
        initModeloTabelaSalas();
        initModeloTabelaUsuarios();
        initModeloTabelaVotos();
        id_Sala = -1;
        setPingVar(false);
        new Thread() {
            public void run() {
                try {
                     qtdSalas = 0;
                     updateModeloTabelaSalas();
                     socketCliente = new DatagramSocket();
                     doc = new DefaultStyledDocument();

                     byte[] buffer = new byte[1024];
                     while (!socketCliente.isClosed()) {
                        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                        socketCliente.receive(request);
                        String received = new String(request.getData(),0,request.getLength());
                        JSONObject JSONReceived = new JSONObject(received);
                        System.out.println("Mensagem recebida: " + JSONReceived);
                        Integer tipo = JSONReceived.getInt("tipo");
                        switch (tipo) {
                            case 1: loginErrado(); break;
                            case 2: loginSucedido(JSONReceived); break;
                            case 4: receberSala(JSONReceived); checkSalas(); break;
                            case 8: receberDiscussao(JSONReceived); break;
                            case 9: receberVotacao(JSONReceived);break;
                            case 10: atualizarUsers(JSONReceived); break;
                            case 12: receberMensagem(JSONReceived); break;
                            case 15: ackVoto(JSONReceived); break;
                            default: System.out.println("Mensagem com Id não identificado");break;
                        } 
                     }
                } catch (SocketException ex){
                    System.out.println("Erro ao enviar mensagem");
                } catch (FileNotFoundException ex) {
                    System.out.println("Arquivo não encontrado");
                } catch (IOException ex) {
                    System.out.println("Erro ao enviar mensagem");
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
            System.out.println("Erro ao enviar mensagem");
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
        return countSalas;
    }
    
    public void fazerLogin(){
        JSONObject JSONLogin = new JSONObject();
        JSONLogin.put("tipo", 0);
        JSONLogin.put("ra", login);
        JSONLogin.put("senha", sha256(senha));
        //JSONLogin.put("senha", senha);
        
        byte[] buffer = new byte[1024];
        buffer = JSONLogin.toString().getBytes();
        
        DatagramPacket msgLogin = new DatagramPacket(buffer, buffer.length, host, porta);
        try {
            socketCliente.send(msgLogin);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
        System.out.println("Mensagem enviada: " + JSONLogin);
    }
    
    public void fazerLogout(){
        JSONObject JSONLogout = new JSONObject();
        JSONLogout.put("tipo", 3);
        
        byte[] buffer = new byte[1024];
        buffer = JSONLogout.toString().getBytes();
        
        DatagramPacket msgLogout = new DatagramPacket(buffer,buffer.length, host, porta);
        
        try{
            socketCliente.send(msgLogout);
        } catch(IOException ex){
            System.out.println("Erro ao enviar mensagem");
        }
        System.out.println("Mensagem enviada: " + JSONLogout);
        
        setPingVar(false);
        GUICliente.logout();
    }
    
    public void acessarSala(int idSala){
        viewdiscussao.limparMensagens();
        usuariosConectados.clear();
        votosCompletos.clear();
        JSONObject JSONacessar = new JSONObject();
        JSONacessar.put("tipo",7);
        JSONacessar.put("id",idSala);
        
        byte[] buffer = new byte[2048];
        buffer = JSONacessar.toString().getBytes();
        
        DatagramPacket msgAcessar = new DatagramPacket(buffer, buffer.length, host, porta);
        
        try {
            socketCliente.send(msgAcessar);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
        System.out.println("Mensagem enviada: " + JSONacessar);
    }
    
    public void sairSala(){
        viewdiscussao.limparMensagens();
        usuariosConectados.clear();
        votosCompletos.clear();
        updateModeloTabelaUsuarios();
        updateModeloTabelaVotos();
        viewdiscussao.atualizarVoto("");
                
        JSONObject JSONsair = new JSONObject();
        JSONsair.put("tipo",11);
        
        byte[] buffer = new byte[2048];
        buffer = JSONsair.toString().getBytes();
        
        DatagramPacket msgSair = new DatagramPacket(buffer, buffer.length, host, porta);
        
        try {
            socketCliente.send(msgSair);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
        viewdiscussao.setVisible(false);
        System.out.println("Mensagem enviada: " + JSONsair);
        id_Sala = -1;
    }
    
    public void enviarMsg(String mensagem){
        JSONObject msg = new JSONObject();
        msg.put("tipo", 14);
        msg.put("criador",username);
        msg.put("mensagem", mensagem);
        
        byte[] buffer = new byte[2048];
        buffer = msg.toString().getBytes();
        
        DatagramPacket msgmsg = new DatagramPacket(buffer, buffer.length, host, porta);
        
        try {
            socketCliente.send(msgmsg);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
        System.out.println("Mensagem enviada: " + msg);
    }
    
    public void criarSala(JSONObject JSONSala){
        JSONSala.put("tipo", 6);
        
        byte[] buffer = new byte[1024];
        buffer = JSONSala.toString().getBytes();
        
        DatagramPacket msgCriarSala = new DatagramPacket(buffer, buffer.length, host, porta);
        try {
            socketCliente.send(msgCriarSala);
            System.out.println("Mensagem enviada: " + JSONSala);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
    }
    
    public void votar(String opcao){
        JSONObject JSONVoto = new JSONObject();
        JSONVoto.put("tipo",15);
        JSONVoto.put("sala",id_Sala);
        JSONVoto.put("opcao",opcao);
        
        byte[] buffer = new byte[2048];
        buffer = JSONVoto.toString().getBytes();
        
        DatagramPacket msgVoto = new DatagramPacket(buffer, buffer.length, host, porta);
        try {
            socketCliente.send(msgVoto);
            System.out.println("Mensagem enviada: " + JSONVoto);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
    }
    
    public void ping(){
        new Thread(){
            @Override
            public void run(){
                while(pingVar){
                    JSONObject JSONPing = new JSONObject();
                    JSONPing.put("tipo", 16);
                    JSONPing.put("sala", id_Sala);

                    byte[] buffer = new byte[2048];
                    buffer = JSONPing.toString().getBytes();

                    DatagramPacket msgPing = new DatagramPacket(buffer, buffer.length, host, porta);

                    try {
                        socketCliente.send(msgPing);
                        System.out.println("Mensagem enviada: " + JSONPing);
                        Thread.sleep(9500);
                    } catch (IOException ex) {
                        System.out.println("Erro ao enviar mensagem");
                    } catch (InterruptedException ex) {
                        System.out.println("Thread interrompida");
                    }
                }
            }
        }.start();
    }
    
    public void checkMsg(){
        new Thread(){
            @Override
            public void run(){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    System.out.println("Thread interrompida");
                }
                for(int i = 0; i < idsMensagens.size(); i++){
                    if(idsMensagens.get(i) != i){
                        pedirMensagem(i);
                        break;
                    }
                }
            }
        }.start();
    }
    
    public void checkSalas(){
        new Thread(){
            @Override
            public void run(){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    System.out.println("Thread interrompida");
                }
                for(int i = 0; i < qtdSalas; i++){
                    JSONObject atual = infoSalas.get(i);
                    if(atual.getInt("id") != i){
                        pedirSala(i);
                        break;
                    }
                }
            }
        }.start();
    }
    
    private void pedirMensagem(int id){
        JSONObject JSONPedir = new JSONObject();
        JSONPedir.put("tipo",13);
        JSONPedir.put("id_msg",id);
        JSONPedir.put("id_sala",id_Sala);
        
        byte[] buffer = new byte[2048];
        buffer = JSONPedir.toString().getBytes();
        
        DatagramPacket msgPedir = new DatagramPacket(buffer, buffer.length, host, porta);
        try {
            socketCliente.send(msgPedir);
            System.out.println("Mensagem enviada: " + JSONPedir);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
    }
    
    private void pedirSala(int id){
        JSONObject sala = new JSONObject();
        sala.put("tipo",5);
        sala.put("id_sala",id);
        
        byte[] buffer = new byte[2048];
        buffer= sala.toString().getBytes();
        
        DatagramPacket msgSala = new DatagramPacket(buffer, buffer.length, host, porta);
        try {
            socketCliente.send(msgSala);
            System.out.println("Mensagem enviada: " + sala);
        } catch (IOException ex) {
            System.out.println("Erro ao enviar mensagem");
        }
    }
    
    private static void loginErrado(){
        JOptionPane.showMessageDialog(null, "Erro de login", "Erro", JOptionPane.ERROR_MESSAGE);
    }
    
    private void loginSucedido(JSONObject login){
        JOptionPane.showMessageDialog(null, login, "Login Sucedido", JOptionPane.INFORMATION_MESSAGE);
        setUsername(login.getString("nome"));
        GUICliente.atualizarUser(username);
        ping();
        setPingVar(true);
    }
    
    private void receberDiscussao(JSONObject discussao){
        idsMensagens = new ArrayList<>();
        JSONArray users = discussao.getJSONArray("usuarios");
        for(int i = 0; i < users.length(); i++){
            JSONObject atual = users.getJSONObject(i);
            usuariosConectados.add(atual);
        }
        updateModeloTabelaUsuarios();
        viewdiscussao.setVisible(true);
        viewdiscussao.atualizarListaUsers(modeloTabelaUsuarios);
    }
    
    public static Set<String> getAllKeys(JSONObject json) {
        return getAllKeys(json, new HashSet<>());
    }

    public static Set<String> getAllKeys(JSONArray arr) {
        return getAllKeys(arr, new HashSet<>());
    }

    private static Set<String> getAllKeys(JSONArray arr, Set<String> keys) {
        for (int i = 0; i < arr.length(); i++) {
            Object obj = arr.get(i);
            if (obj instanceof JSONObject) keys.addAll(getAllKeys(arr.getJSONObject(i)));
            if (obj instanceof JSONArray) keys.addAll(getAllKeys(arr.getJSONArray(i)));
        }

        return keys;
    }

    private static Set<String> getAllKeys(JSONObject json, Set<String> keys) {
        for (String key : json.keySet()) {
            Object obj = json.get(key);
            if (obj instanceof JSONObject) keys.addAll(getAllKeys(json.getJSONObject(key)));
            if (obj instanceof JSONArray) keys.addAll(getAllKeys(json.getJSONArray(key)));
        }

        keys.addAll(json.keySet());
        return keys;
    }
    
    private void receberVotacao(JSONObject votacao){
        votosCompletos.clear();
        JSONArray resultados = votacao.getJSONArray("resultados");
        Set<String>keys = new HashSet<>();    

        //keys = getAllKeys(resultados);
        //opcoesVotos = new ArrayList<String>(keys);
        
        for(int i = 0; i < resultados.length(); i++){
            JSONObject target = resultados.getJSONObject(i);
            keys = getAllKeys(target);
            opcoesVotos = new ArrayList<>(keys);
            JSONObject atual = new JSONObject();
            String s = opcoesVotos.get(0);
            atual.put("nome",s);
            atual.put("votos",target.get(s));
            votosCompletos.add(atual);
        }
        
        //for (int i = 0; i < opcoesVotos.size(); i++) {
            //JSONObject atual = new JSONObject();
            //JSONObject aux = resultados.getJSONObject(i);
            //String s = opcoesVotos.get(i);
            //atual.put("nome",s);
            //atual.put("votos",aux.getInt(s));
            //votosCompletos.add(atual);
            //System.out.println(opcoesVotos.get(i) + atual.toString());
        //}
        
        //System.out.println(votosCompletos.toString());
 
        updateModeloTabelaVotos();
        viewdiscussao.atualizarListaVotos(modeloTabelaVotos);
    }
    
    private void atualizarUsers(JSONObject user){
        Boolean opt = user.getBoolean("adicionar");
        
        if(opt){
            JSONObject usuario = new JSONObject();
            usuario.put("nome", user.getString("nome"));
            usuario.put("ra", user.getString("ra"));
            usuariosConectados.add(usuario);
        }else{
            Iterator<JSONObject> it = usuariosConectados.iterator();
            JSONObject aux = new JSONObject();
            String ra = user.getString("ra");
            int i = 0;
            while(it.hasNext()){
                aux = it.next();
                if(aux.getString("ra").equals(ra)){
                    usuariosConectados.remove(i);
                    break;
                }
                i++;
            }
        }
        updateModeloTabelaUsuarios();
    }
    
    private void receberMensagem(JSONObject mensagem){
        String str = mensagem.getString("mensagem");
        String criador = mensagem.getString("criador");
        Long timestamp = Long.valueOf(mensagem.getString("timestamp"));
        Date date = new Date(timestamp * 1000L);
        
        checkMsg();
        viewdiscussao.atualizarMensagens(str, criador, date);
    }
    
    private static void receberSala(JSONObject sala){
        sala.remove("tipo");
        infoSalas.add(sala);
        qtdSalas = qtdSalas + 1;
        updateModeloTabelaSalas();
        GUICliente.atualizarListaSalas(modeloTabelaSalas);
    }
    
    private void ackVoto(JSONObject ack){
        String opcao = ack.getString("opcao");
        viewdiscussao.atualizarVoto(opcao);
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
