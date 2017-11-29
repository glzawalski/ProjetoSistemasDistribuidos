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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
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
    private static DefaultTableModel modeloTabelaSalas;
    private static DefaultTableModel modeloTabelaUsuarios;
    private static ViewPrincipal GUICliente;
    private static ViewDiscussao viewdiscussao;
    private StyledDocument doc;
    private boolean pingState;
    
    public ModelCliente(ViewPrincipal view, ViewDiscussao discussao) {
        infoSalas = new ArrayList();
        usuariosConectados = new ArrayList();
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

    public void setModeloTabelaUsuarios(DefaultTableModel modeloTabelaUsuarios) {
        this.modeloTabelaUsuarios = modeloTabelaUsuarios;
    }
    
    public void initModeloTabelaSalas() {
        setModeloTabelaSalas(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"ID Sala", "Criador", "Descrição", "Inicio", "Fim"}));
    }
    
    public void initModeloTabelaUsuarios() {
        setModeloTabelaUsuarios(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"Nome","RA"}));
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
            rowData[0] = infoSalas.get(index).getInt("id");
            rowData[1] = infoSalas.get(index).getString("criador");
            rowData[2] = infoSalas.get(index).getString("descricao");
            rowData[3] = infoSalas.get(index).getString("inicio");
            rowData[4] = infoSalas.get(index).getString("fim");
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
            rowData[1] = "teste";//usuariosConectados.get(index).getString("ra");
            modeloTabelaUsuarios.addRow(rowData);
            index++;
        }
    }
    
    public void setViewDiscussao(ViewDiscussao view){
        viewdiscussao = view;
    }
    
    public void init(){
        initModeloTabelaSalas();
        initModeloTabelaUsuarios();
        pingState = false;
        new Thread() {
            public void run() {
                try {
                     qtdSalas = 0;
                     updateModeloTabelaSalas();
                     socketCliente = new DatagramSocket();
                     doc = new DefaultStyledDocument();

                     System.out.println("Numero de salas abertas: " + qtdSalas);
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
                            case 4: receberSala(JSONReceived); break;
                            case 8: receberDiscussao(JSONReceived); break;
                            case 10: atualizarUsers(JSONReceived); break;
                            case 12: receberMensagem(JSONReceived); break;
                            default: System.out.println("Mensagem com Id não identificado");break;
                        } 
                     }
                } catch (SocketException ex){} catch (FileNotFoundException ex) {
                    Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ViewLogin.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ViewLogin.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Mensagem enviada: " + JSONLogout);
        
        GUICliente.logout();
    }
    
    public void acessarSala(int idSala){
        JSONObject JSONacessar = new JSONObject();
        JSONacessar.put("tipo",7);
        JSONacessar.put("id",idSala);
        
        byte[] buffer = new byte[2048];
        buffer = JSONacessar.toString().getBytes();
        
        DatagramPacket msgAcessar = new DatagramPacket(buffer, buffer.length, host, porta);
        
        try {
            socketCliente.send(msgAcessar);
        } catch (IOException ex) {
            Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Mensagem enviada: " + JSONacessar);
    }
    
    public void sairSala(){
        viewdiscussao.limparMensagens();
        usuariosConectados.clear();
        updateModeloTabelaUsuarios();
                
        JSONObject JSONsair = new JSONObject();
        JSONsair.put("tipo",11);
        
        byte[] buffer = new byte[2048];
        buffer = JSONsair.toString().getBytes();
        
        DatagramPacket msgSair = new DatagramPacket(buffer, buffer.length, host, porta);
        
        try {
            socketCliente.send(msgSair);
        } catch (IOException ex) {
            Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
        }
        viewdiscussao.setVisible(false);
        System.out.println("Mensagem enviada: " + JSONsair);
        pingState = false;
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
            Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (IOException ex) {
            Logger.getLogger(ViewLogin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void ping(){
        new Thread(){
            @Override
            public void run(){
                while(pingState){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    JSONObject JSONPing = new JSONObject();
                    JSONPing.put("tipo", 16);
                    JSONPing.put("sala", id_Sala);

                    byte[] buffer = new byte[2048];
                    buffer = JSONPing.toString().getBytes();

                    DatagramPacket msgAcessar = new DatagramPacket(buffer, buffer.length, host, porta);

                    try {
                        socketCliente.send(msgAcessar);
                        System.out.println("Mensagem enviada: " + JSONPing);
                        Thread.sleep(5000);
                    } catch (IOException ex) {
                        Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ModelCliente.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }
    
    private static void loginErrado(){
        JOptionPane.showMessageDialog(null, "Erro de login", "Erro", JOptionPane.ERROR_MESSAGE);
    }
    
    private void loginSucedido(JSONObject login){
        JOptionPane.showMessageDialog(null, login, "Login Sucedido", JOptionPane.INFORMATION_MESSAGE);
        setUsername(login.getString("nome"));
        GUICliente.atualizarUser(username);
    }
    
    private void receberDiscussao(JSONObject discussao){
        JSONArray users = discussao.getJSONArray("usuarios");
        for(int i = 0; i < users.length(); i++){
            JSONObject atual = users.getJSONObject(i);
            usuariosConectados.add(atual);
        }
        updateModeloTabelaUsuarios();
        viewdiscussao.setVisible(true);
        viewdiscussao.atualizarListaUsers(modeloTabelaUsuarios);
        pingState = true;
        ping();
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
                }
                i++;
            }
        }
        updateModeloTabelaUsuarios();
    }
    
    private void receberMensagem(JSONObject mensagem){
        String str = mensagem.getString("mensagem");
        String criador = mensagem.getString("criador");
        viewdiscussao.atualizarMensagens(str, criador);
    }
    
    private static void receberSala(JSONObject sala){
        sala.remove("tipo");
        infoSalas.add(sala);
        qtdSalas = qtdSalas + 1;
        updateModeloTabelaSalas();
        GUICliente.atualizarListaSalas(modeloTabelaSalas);
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
