/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import org.json.JSONObject;

/**
 *
 * @author gabriel
 */
public class ServidorUDP {
    private static DatagramSocket server = null;
    
    public static void main(String args[]) throws Exception {
         try {
             server = new DatagramSocket(20000);
             System.out.println("server up");
             byte[] buffer = new byte[1024];
             while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                server.receive(request);
                 
                String received = new String(request.getData(),0,request.getLength());
                JSONObject JSONReceived = new JSONObject(received);
                
                System.out.println(JSONReceived);
                 
                Integer tipo = JSONReceived.getInt("tipo");
                 
                switch (tipo) {
                    /*case -2: ping(); break;
                    case -1: erroMalFormada(); break;
                    */case 0: checarLogin(request, JSONReceived); break;
                    /*case 1: loginErrado(); break;
                    case 2: loginSucedido(); break;
                    case 3: criarSala(); break;
                    case 4: atualizarListaSalas(); break;
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
                    case 16: des-conectarSala(); break;
                    */
                } 
             }
         } catch (SocketException e){}
      }

    private static void checarLogin(DatagramPacket request, JSONObject received) {
        String fileName = "/home/gabriel/NetBeansProjects/ProjetoSD/login.txt";
        String line = null;
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
                        JSONRespostaLoginSucedido.put("nome", "");
                        JSONRespostaLoginSucedido.put("tamanho", 0);
                        
                        buffer = JSONRespostaLoginSucedido.toString().getBytes();
                        
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        server.send(reply);
                        break;
                    } else {
                        byte[] buffer = new byte[1024];
                        
                        JSONObject JSONRespostaLoginSucedido = new JSONObject();
                        JSONRespostaLoginSucedido.put("tipo", 1);
                        
                        buffer = JSONRespostaLoginSucedido.toString().getBytes();
                        
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        server.send(reply);
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
                        server.send(reply);
            }
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {}
        catch(IOException ex) {}
    }
}
