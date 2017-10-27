/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import View.ViewConexao;
import View.ViewLogin;
import View.ViewPrincipal;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.json.JSONObject;

/**
 *
 * @author gabriel
 */
public class ModelCliente extends Thread {
    ModelCliente() {}
    
    public static void main(String args[]) throws IOException{        
        int port = 0;
        InetAddress host = null;
        DatagramSocket socketCliente = null;
        try {
            socketCliente = new DatagramSocket();
         } catch (SocketException e) {}
        
        ViewLogin viewLogin = new ViewLogin(socketCliente, host, port);
        ViewPrincipal viewPrincipal = new ViewPrincipal();
        viewLogin.setVisible(true);
        
        while (true) {
            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer,buffer.length);
            socketCliente.receive(reply);
            String received = new String(reply.getData(),0,reply.getLength());
            JSONObject resposta = new JSONObject(received);
            System.out.println(resposta);
            
            switch (resposta.getInt("tipo")) {
                    /*case -2: ping(); break;
                    case -1: erroMalFormada(); break;
                    case 0: checarLogin(request, JSONReceived); break;*/
                    case 1: loginErrado(); break;
                    case 2: loginSucedido(resposta, viewLogin, viewPrincipal); break;/*
                    case 3: criarSala(); break;
                    case 4: atualizarListaSalas(); break;
                    case 5: acessoSala(); break;
                    case 6: historicoUsuariosSala(); break;
                    case 7: statusVotacao(); break;
                    case 8: mensagemChat(); break;
                    case 9: mensagemChatServidor(); break;
                    case 10: break;
                    *///case 11: respostaAcessoSala(); break;
                    /*
                    case 12: respostaCriarSala(); break;
                    case 13: mensagemEspecifica(); break;
                    case 14: salaEspecifica(); break;
                    case 15: computarVoto(); break;
                    case 16: des-conectarSala(); break;
                    */
                }
        }
    }
    
    private static void loginErrado(){
        JOptionPane.showMessageDialog(null, "Erro de login", "Erro", JOptionPane.ERROR_MESSAGE);
    }
    
    private static void loginSucedido(JSONObject login, ViewLogin view, ViewPrincipal viewPrincipal){
        view.setVisible(false);
        
        DefaultListModel listModel = new DefaultListModel();
        JPanel panel = new JPanel(new GridBagLayout());
        listModel.addElement("teste");
        
        JList listaSalas = new JList(listModel);
        panel.add(listaSalas);
        viewPrincipal.add(panel);
        viewPrincipal.setVisible(true);
        
    }
}
