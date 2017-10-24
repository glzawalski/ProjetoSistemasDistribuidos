/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import View.ViewConexao;
import View.ViewLogin;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 *
 * @author gabriel
 */
public class ModelCliente extends Thread {
    ModelCliente() {
        
    }
    
    public static void main(String args[]) {        
        ViewConexao conexao = new ViewConexao();
        conexao.setVisible(true);
        
        int port = 0;
        InetAddress host = null;
        DatagramSocket socketCliente = null;
        try {
            socketCliente = new DatagramSocket();
         } catch (SocketException e) {}
        
        ViewLogin viewLogin = new ViewLogin(socketCliente, host, port);
        viewLogin.setVisible(true);
        
        while (true) {
            
        }
    }
}
