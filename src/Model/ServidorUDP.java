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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import org.json.JSONArray;
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
    
    Runnable checagemSala = new Runnable() {
        public void run() {
            threadChecagemSalas();
        }
    };

    public ServidorUDP() {
        infoSalas = new ArrayList();
        usuariosConectados = new ArrayList();
        inicializarSalas();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(checagemSala, 0, 1, TimeUnit.MINUTES);
        contagemPing();
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
                new Object[][]{}, new String[]{"ID Sala","Nome Sala", "Criador", "Descrição", "Inicio", "Fim"}));
    }
    
    public void initModeloTabelaUsuarios() {
        setModeloTabelaUsuarios(new javax.swing.table.DefaultTableModel(
                new Object[][]{}, new String[]{"Nome", "RA", "IP", "Porta"}));
    }
    
    private static void updateModeloTabelaSalas() {
        Object rowData[] = new Object[6];
        int index = 0;
        if (modeloTabelaSalas.getRowCount() > 0) {
            for (int i = modeloTabelaSalas.getRowCount() - 1; i > -1; i--) {
                modeloTabelaSalas.removeRow(i);
            }
        }
        
        while (index < infoSalas.size()) {
            rowData[0] = infoSalas.get(index).getInfoSalas().getInt("id");
            rowData[1] = infoSalas.get(index).getInfoSalas().getString("nome");
            rowData[2] = infoSalas.get(index).getInfoSalas().getString("criador");
            rowData[3] = infoSalas.get(index).getInfoSalas().getString("descricao");
            /*long inicio = Long.valueOf(infoSalas.get(index).getInfoSalas().getString("inicio"));
            rowData[4] = formatarData(inicio);
            long fim = Long.valueOf(infoSalas.get(index).getInfoSalas().getString("fim"));
            rowData[5] = formatarData(fim);*/
            rowData[4] = infoSalas.get(index).getInfoSalas().getString("inicio");
            rowData[5] = infoSalas.get(index).getInfoSalas().getString("fim");
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
        String rowData[] = new String[4];
        int index = 0;
        if (modeloTabelaUsuarios.getRowCount() > 0) {
            for (int i = modeloTabelaUsuarios.getRowCount() - 1; i > -1; i--) {
                modeloTabelaUsuarios.removeRow(i);
            }
        }
        while (index < usuariosConectados.size()) {
            rowData[0] = usuariosConectados.get(index).getNome();
            rowData[1] = usuariosConectados.get(index).getRa();
            rowData[2] = usuariosConectados.get(index).getEndrecoIP().toString();
            rowData[3] = Integer.toString(usuariosConectados.get(index).getPorta());
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
                    } else {
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
                            case 11: logoutSala(request); break; 
                            //12 = mensagemChat(); break;
                            case 13: pedidoMensagemEspecifica(request, JSONReceived); break;
                            case 14: mensagemChatServidor(request, JSONReceived); break;
                            case 15: computarVoto(request, JSONReceived); break;
                            case 16: iniciarPing(request, JSONReceived); break;
                            default: mensagemMalFormada(request, JSONReceived); break;
                        } 
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
                BufferedReader bufferedReaderMsg = new BufferedReader(new FileReader(arquivoMensagens));
                String mensagem = bufferedReaderMsg.readLine();
                ArrayList<JSONObject> mensagens = new ArrayList<>();
                while (mensagem != null) {
                    mensagens.add(new JSONObject(mensagem));
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
    
    private static void threadChecagemSalas() {
        new Thread() {
            public void run() {
                for (ModelSalas s : infoSalas) {
                    if (Long.valueOf(s.getInfoSalas().getString("fim")) <= Instant.now().getEpochSecond() && s.getInfoSalas().getBoolean("status")) {
                        System.out.println("sala passou da data limite");
                        JSONObject fechamentoSala = new JSONObject(s.getInfoSalas().toString());
                        fechamentoSala.remove("status");
                        fechamentoSala.put("status", false);
                        substituirSalaArquivo(s.getInfoSalas(), fechamentoSala);
                        s.setInfoSalas(fechamentoSala);
                        for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                            enviarStatusVotacao(u.getEndrecoIP(), u.getPorta(), s);
                        }
                    }
                }
            }
        }.start();
    }
    
    private static void substituirSalaArquivo(JSONObject salaAberta, JSONObject salaFechada) {
        System.out.println("fechando sala expirada      " + salaAberta.toString());
        System.out.println(salaFechada.toString());
        String line;
        StringBuilder buffer = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./salas/salas"));
            line = bufferedReader.readLine();
            while(line != null){
                System.out.println("lendo salas abertas     " + line);
                if (line.equals(salaAberta.toString())) {
                    System.out.println("sala a ser fechada encontrada");
                    buffer.append(salaFechada.toString().concat("\n"));
                } else {
                    buffer.append(line.concat("\n"));
                }
                line = bufferedReader.readLine();
            }            
            BufferedWriter saida = null;
            saida = new BufferedWriter(new FileWriter("./salas/salas", false));
            saida.write(buffer.toString());
            saida.flush();
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
                        ModelUsuarioConectado novoUsuario = new ModelUsuarioConectado(user.getString("nome"), user.getString("ra"), request.getAddress(), request.getPort());
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
                        novoAcesso = u;
                        novoAcesso.setAtivo(true);
                        break;
                    }
                }
                atualizacaoUsuariosSala(s, novoAcesso, true);
                System.out.println("            usuario adicionado");
                s.addUsuariosConectados(novoAcesso);
                JSONObject histSala = new JSONObject();
                histSala.put("tipo", 8);
                histSala.put("tamanho", s.getMensagens().size());
                ArrayList<JSONObject> usuariosSala = new ArrayList<>();
                for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                    if (u.isAtivo()) {
                        JSONObject uc = new JSONObject();
                        uc.put("nome", u.getNome());
                        uc.put("ra", u.getRa());
                        usuariosSala.add(uc);
                    }
                }
                histSala.put("usuarios", usuariosSala);
                byte[] buffer = new byte[1024];
                buffer = histSala.toString().getBytes();
                try {
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                    socketServidor.send(reply);
                    System.out.println("informacoes enviadas: " + histSala);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                }
                enviarStatusVotacao(request.getAddress(), request.getPort(), s);
                streamMensagensSala(request, s.getMensagens());
            }
        }
    }
    
    private void logoutSala(DatagramPacket request) {
        boolean flagUsuario = false;
        for (ModelSalas s : infoSalas) {
            if (!flagUsuario) {
                System.out.println("            procurando sala...");
                for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                    System.out.println("                procurando usuarios conectados...");
                    if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                        flagUsuario = true;
                        System.out.println("                    usuario encontrado...");
                        s.getUsuariosConectados().remove(u);
                        System.out.println("                    usuario removido...");
                        atualizacaoUsuariosSala(s, u, false);
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }
    
    private static void atualizacaoUsuariosSala(ModelSalas s, ModelUsuarioConectado novoAcesso, boolean adicionar) {
        System.out.println("                        atualizando lista de usuarios da sala...");
        JSONObject novoConectadoSala = new JSONObject();
        novoConectadoSala.put("tipo", 10);
        novoConectadoSala.put("adicionar", adicionar);
        novoConectadoSala.put("nome", novoAcesso.getNome());
        novoConectadoSala.put("ra", novoAcesso.getRa());
        byte[] buffer = new byte[1024];
        buffer = novoConectadoSala.toString().getBytes();
        for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
            System.out.println("                            encontrando usuario na lista de conectados...");
            try {
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length, u.getEndrecoIP(), u.getPorta());
                socketServidor.send(reply);
                System.out.println("mensagem enviada: " + novoConectadoSala);
            } catch (IOException ex) {
                Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void enviarStatusVotacao(InetAddress address, int port, ModelSalas s) {
        JSONObject votacao = new JSONObject();
        votacao.put("tipo", 9);
        if (s.getInfoSalas().getBoolean("status")) {
            votacao.put("acabou", true);
        } else {
            votacao.put("acabou", false);
        }
        JSONArray opcoes = new JSONArray();
        for (Object o : s.getInfoSalas().getJSONArray("opcoes")) {
            String nomeOpcao = new JSONObject(o.toString()).getString("nome");
            int qtdVotos = 0;
            for (JSONObject jo : s.getVotos()) {
                if (jo.getString("opcao").equals(nomeOpcao)) {
                    qtdVotos = qtdVotos + 1;
                }
            }
            JSONObject opcao = new JSONObject();
            opcao.put(nomeOpcao, qtdVotos);
            opcoes.put(opcao);
        }
        byte[] buffer = new byte[1024];
        buffer = votacao.toString().getBytes();
        try {
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length, address, port);
            socketServidor.send(reply);
            System.out.println("status votacao enviada: " + votacao.toString());
        } catch (IOException ex) {
            Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
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
    
    private void pedidoMensagemEspecifica(DatagramPacket request, JSONObject received) {
        new Thread() {
            public void run() {
                ModelSalas sala = infoSalas.get(received.getInt("id_sala"));
                JSONObject m = new JSONObject(sala.getMensagens().get(received.getInt("id_msg")).toString());
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
        }.start();
    }
    
    private void mensagemChatServidor(DatagramPacket request, JSONObject received) {
        ModelUsuarioConectado remetenteMensagem = null;
        for (ModelUsuarioConectado u : usuariosConectados) {
            if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                System.out.println("remetente encontrado na lista de conectados...");
                remetenteMensagem = u;
                for (ModelSalas s : infoSalas) {
                    System.out.println("procurando sala");
                    if (s.getUsuariosConectados().contains(remetenteMensagem)) {
                        System.out.println("sala encontrada re-enviando mensagem chat");
                        received.remove("tipo");
                        received.put("id", s.getMensagens().size());
                        received.put("timestamp", Long.toString(Instant.now().getEpochSecond()));
                        s.addMensagens(received);
                        salvarMensagemSalaArquivo(s.getInfoSalas().getInt("id"), received);
                        received.put("tipo", 12);
                        received.put("tamanho", s.getMensagens().size());
                        byte[] buffer = new byte[1024];
                        buffer = received.toString().getBytes();
                        for (ModelUsuarioConectado uc : s.getUsuariosConectados()) {
                            try {
                                DatagramPacket reply = new DatagramPacket(buffer, buffer.length, uc.getEndrecoIP(), uc.getPorta());
                                socketServidor.send(reply);
                                System.out.println("mensagem de chat enviada: " + received);
                            } catch (IOException ex) {
                                Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    System.out.println("    procurando...");
                }
            }
        }
    }
    
    private static void computarVoto(DatagramPacket request, JSONObject received) {
        ModelSalas sala = null;
        for (ModelSalas s : infoSalas) {
            if (s.getInfoSalas().getInt("id") == received.getInt("sala")) {
                sala = s;
                break;
            }
        }
        ModelUsuarioConectado usuario = null;
        if (sala != null) {
            for (ModelUsuarioConectado u : sala.getUsuariosConectados()) {
                if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort() && u.isAtivo()) {
                    usuario = u;
                    break;
                }
            }
        }
        if (usuario != null) {
            for (Object o : sala.getInfoSalas().getJSONArray("opcoes")) {
                if (received.getString("opcao").equals(new JSONObject(o.toString()).getString("opcao"))) {
                    JSONObject votoUsuario = new JSONObject();
                    votoUsuario.put("ra", usuario.getRa());
                    votoUsuario.put("voto", received.getString("opcao"));
                    boolean flag = false;
                    for (JSONObject jo : sala.getVotos()) {
                        if (jo.getString("ra").equals(votoUsuario.getString("ra"))) {
                            flag = true;
                            jo.remove("opcao");
                            jo.put("opcao", received.getString("opcao"));
                        }
                    }
                    if (flag == false) {
                        sala.addVoto(votoUsuario);
                    }
                    break;
                }
            }
        }
    }
    
    private static void salvarMensagemSalaArquivo(int id, JSONObject mensagem) {
        BufferedWriter saida = null;
        try {
            saida = new BufferedWriter(new FileWriter("./salas/mensagens/" + id, true));
            saida.write(mensagem.toString() + "\n");
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
    
    private static void salvarSalaArquivo(JSONObject sala) {
        BufferedWriter saida = null;
        try {
            saida = new BufferedWriter(new FileWriter("./salas/salas", true));
            saida.write(sala.toString() + "\n");
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
    
    public static void iniciarPing(DatagramPacket request, JSONObject received) {
        for (ModelUsuarioConectado u : usuariosConectados) {
            if (u.getEndrecoIP().equals(request.getAddress().toString()) && u.getPorta() == request.getPort()) {
                u.setAtivo(true);
                break;
            }
        }
        if (received.getInt("sala") != -1) {
            ModelSalas sala = null;
            for (ModelSalas s : infoSalas) {
                if (s.getInfoSalas().getInt("id") == received.getInt("sala")) {
                    sala = s;
                    break;
                }
            }
            if (sala != null) {
                for (ModelUsuarioConectado u : sala.getUsuariosConectados()) {
                    if (u.getEndrecoIP().equals(request.getAddress().toString()) && u.getPorta() == request.getPort()) {
                        u.setAtivo(true);
                    }
                }
            }
        }
    }
    
    //contagem de ping iniciada pelo servidor, precisa kickar usuarios inativos do servidor
    public static void contagemPing() {
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        for (ModelUsuarioConectado u : usuariosConectados) {
                            u.setAtivo(false);
                        }
                        for (ModelSalas s : infoSalas) {
                            for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                                u.setAtivo(false);
                            }
                        }
                        Thread.sleep(30000);
                        for (ModelSalas s : infoSalas) {
                            for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                                if (!u.isAtivo()) {
                                    s.getUsuariosConectados().remove(u);
                                    atualizacaoUsuariosSala(s, u, false);
                                    break;
                                }
                            }
                        }
                        for (ModelUsuarioConectado u : usuariosConectados) {
                            if (!u.isAtivo()) {
                                usuariosConectados.remove(u);
                                updateModeloTabelaUsuarios();
                                break;
                            }
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ServidorUDP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }
}
