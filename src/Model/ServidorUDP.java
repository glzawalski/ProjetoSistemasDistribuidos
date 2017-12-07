//bugs : broadcast de votos recebe duas vezes -> limpar buffer cliente
//       falha na atualização de troca de votos

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
    //dar uma olhada nas structs também seria interessante pra estudar
    private static ArrayList<ModelSalas> infoSalas; //lista de structs que guardam informações da sala
    private static ArrayList<ModelUsuarioConectado> usuariosConectados; //lista de structs que guardam informações do cliente logados no servidor
    private static DefaultTableModel modeloTabelaSalas;
    private static DefaultTableModel modeloTabelaUsuarios;
    
    Runnable checagemSala = new Runnable() { //função que roda baseada na chamada de um executor
        public void run() {
            threadChecagemSalas(); //checagem se as salas foram encerradas ou não
        }
    };

    public ServidorUDP() {
        infoSalas = new ArrayList();
        usuariosConectados = new ArrayList();
        inicializarSalas();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(checagemSala, 0, 1, TimeUnit.MINUTES); //executor que chama a checagem de validade da sala implementado ali em cima
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
                try { //abertura do socket do servidor
                    socketServidor = new DatagramSocket(porta);
                } catch (SocketException ex) {
                    System.out.println("Falha na porta do servidor");
                }
                if (socketServidor != null) {
                    GUIServidor.buttonConfirmar.setEnabled(false);
                    GUIServidor.textfieldPorta.setEditable(false);
                }
                System.out.println("Servidor aberto em : " + socketServidor.getLocalPort());
                System.out.println("Numero de salas abertas: " + infoSalas.size());
                byte[] buffer = new byte[1024];
                while (!socketServidor.isClosed()) { //loop de recebimento de mensagens udp
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    try {
                        socketServidor.receive(request);
                    } catch (IOException ex) {
                        System.out.println("Falha no recebimento de pacotes");
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
    
    private void mensagemMalFormada(DatagramPacket request, JSONObject JSONReceived) { //retorna a mensagem inteira de volta pra quem enviou
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
            System.out.println("Falha de envio de mensagem");
        }
    }
    
    private void inicializarSalas() { //varre o arquivo de salas para popular a lista com as informações
        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./salas/salas"));
            line = bufferedReader.readLine();
            while(line != null){ //le cada linha do arquivo
                ModelSalas novaSala = new ModelSalas();
                JSONObject info = new JSONObject(line);
                novaSala.setInfoSalas(info); //adiciona as informações da sala encontradas no arquivo num struct
                String arquivoMensagens = "./salas/mensagens/".concat(Integer.toString(info.getInt("id"))); //inicia leitura das mensagens da sala
                BufferedReader bufferedReaderMsg = new BufferedReader(new FileReader(arquivoMensagens));
                String mensagem = bufferedReaderMsg.readLine();
                ArrayList<JSONObject> mensagens = new ArrayList<>();
                while (mensagem != null) { //le cada linha do arquivo de mensagens
                    mensagens.add(new JSONObject(mensagem));
                    mensagem = bufferedReaderMsg.readLine();
                }
                novaSala.setMensagens(mensagens); //salva na struct a lista das mensagens
                infoSalas.add(novaSala); //adiciona a struct na lista de salas
                line = bufferedReader.readLine();
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Falha ao encontrar arquivo de mensagens");
        } catch (IOException ex) {
            System.out.println("Falha de escrita de mensagens");
        }
    }
    
    private static void threadChecagemSalas() { //checa se as salas estão abertas ou não
        new Thread() {
            public void run() {
                for (ModelSalas s : infoSalas) { //para cada uma das salas na lista de structs
                    if (Long.valueOf(s.getInfoSalas().getString("fim")) <= Instant.now().getEpochSecond() && s.getInfoSalas().getBoolean("status")) {
                        //substitui no arquivo a linha que contem a sala que expirou pra uma sala encerrada
                        System.out.println("sala passou da data limite");
                        JSONObject fechamentoSala = new JSONObject(s.getInfoSalas().toString());
                        fechamentoSala.remove("status");
                        fechamentoSala.put("status", false);
                        substituirSalaArquivo(s.getInfoSalas(), fechamentoSala); //função que faz a troca da linha do arquivo
                        s.setInfoSalas(fechamentoSala);
                        for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                            enviarStatusVotacao(u.getEndrecoIP(), u.getPorta(), s); //envia o resultado da votação quando acaba
                        }
                    }
                }
            }
        }.start();
    }
    
    private static void substituirSalaArquivo(JSONObject salaAberta, JSONObject salaFechada) { //abre o arquivo de salas e substitui a salaAberta pela salaFechada
        System.out.println("fechando sala expirada      " + salaAberta.toString());
        System.out.println(salaFechada.toString());
        String line;
        StringBuilder buffer = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./salas/salas"));
            line = bufferedReader.readLine();
            while(line != null){ //le arquivo, escreve a mesma coisa do arquivo caso não tenha expirado ou substitui caso tenha
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
            saida.write(buffer.toString()); //escreve o buffer no arquivo de salas
            saida.flush();
        } catch (FileNotFoundException ex) {
            System.out.println("Falha ao encontrar arquivo de mensagens");
        } catch (IOException ex) {
            System.out.println("Falha de escrita de mensagens");
        }
    }
    
    private static void checarLogin(DatagramPacket request, JSONObject received) { //checagem de login
        String fileName = "loginhash.txt";
        String line;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            boolean flagEncontrado = false;
            while((line = bufferedReader.readLine()) != null) { //le cada linha do arquivo de login
                JSONObject user = new JSONObject(line);
                if (user.getString("ra").equals(received.getString("ra"))) { //marca como encontrado caso ache o ra
                    flagEncontrado = true; 
                    if (user.getString("senha").equals(received.getString("senha"))) { //caso ache a senha retorna sucesso
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
                        novoUsuario.setAtivo(true);
                        usuariosConectados.add(novoUsuario);
                        updateModeloTabelaUsuarios();
                        streamSalas(request.getAddress(), request.getPort()); //envia a lista de salas pro novo login
                        break;
                    } else { // envia falha caso senha não bata
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
            if (flagEncontrado == false) { // foi preciso colocar um flag pois seria enviado erro a cada ra encontrado que não fosse o requisitado ou que não batesse a senha
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
            System.out.println("Falha ao encontrar arquivo de mensagens");
        } catch (IOException ex) {
            System.out.println("Falha de escrita de mensagens");
        }
    }
    
    private static void streamSalas(InetAddress address, int port) { //faz a leitura da struct de salas e envia todas para o cliente que logou com sucesso
        new Thread() {
            public void run() {
                byte[] buffer = new byte[1024];
                for (ModelSalas s : infoSalas) { //faz o envio individual para cada sala da lista de structs
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
                        System.out.println("Falha de escrita de mensagens");
                    }
                }
            }
        }.start();
    }
    
    private static void pedidoSalaEspecifica(DatagramPacket request, JSONObject received) { //faz a varredura da lista de salas e envia a do id requisitado
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
                    System.out.println("Falha de escrita de mensagens");
                }
                break;
            }
        }
    }
    
    private static void criarSala(DatagramPacket request, JSONObject received) { //usa o próprio json recebido para colocar as informações da sala no arquivo e na struct
        received.remove("tipo");
        received.put("id", infoSalas.size()); //coloca uma id baseada no numero de salas
        String criador = null;
        for (ModelUsuarioConectado u : usuariosConectados) { //encontra o usuario que enviou a requisição e coloca o nome dele nas informações da sala
            if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                criador = u.getNome();
            }
        }
        System.out.println(criador);
        received.put("criador", criador);
        received.put("inicio", Long.toString(Instant.now().getEpochSecond()));
        received.put("status", true);
        salvarSalaArquivo(received); //salva no arquivo
        criarArquivoSala(infoSalas.size()); //cria arquivo de mensagens da sala
        criarArquivoVotos(infoSalas.size()); //criar arquivo de votos da sala
        ModelSalas novaSala = new ModelSalas();
        novaSala.setInfoSalas(received);
        infoSalas.add(novaSala); //salva sala na lista de structs
        received.put("tamanho", infoSalas.size()); //adiciona informações do protocolo para enviar pros clientes a nova sala
        received.put("tipo", 4);
        byte[] buffer = new byte[1024];
        buffer = received.toString().getBytes();
        broadcast(buffer); //envia para todos os conectados no servidor a sala nova
        System.out.println("mensagem enviada: " + received);
        updateModeloTabelaSalas();
    }
    
    public void logoutServidor(DatagramPacket request, JSONObject received) { //varre a lista de usuarios até encontrar o que requisitou e o tira da lista de conectados
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
        while (iterator.hasNext()) { //procura em todas as salas a que possui o id requisitado
            ModelSalas s = iterator.next();
            if (s.getInfoSalas().getInt("id") == received.getInt("id")) {
                System.out.println("sala encontrada, enviando informações da sala...");
                for (ModelUsuarioConectado u : usuariosConectados) { //busca na lista de usuarios conectados ao servidor quem fez a requisição
                    if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                        u.setAtivo(true);
                        atualizacaoUsuariosSala(s, u, true); //atualiza para todos os conectados na sala da entrada do novo cliente
                        System.out.println("            usuario adicionado");
                        s.addUsuariosConectados(u);
                        break;
                    }
                }
                JSONObject histSala = new JSONObject();
                histSala.put("tipo", 8);
                histSala.put("tamanho", s.getMensagens().size());
                ArrayList<JSONObject> usuariosSala = new ArrayList<>();
                for (ModelUsuarioConectado u : s.getUsuariosConectados()) { 
                    //busca todos os usuarios conectados e ativos na sala pra enviar pro cliente que pediu acesso a lista de cliente conectados
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
                //envia historico e lista de cliente pro usuario novo conectado
                try {
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                    socketServidor.send(reply);
                    System.out.println("informacoes enviadas: " + histSala);
                } catch (IOException ex) {
                    System.out.println("Falha de envio de datagrama");
                }
                enviarStatusVotacao(request.getAddress(), request.getPort(), s); //envia status votação pro cliente novo
                streamMensagensSala(request, s.getMensagens()); //envia todas as mensagens da sala pro cliente novo
            }
        }
    }
    
    private void logoutSala(DatagramPacket request) {
        boolean flagUsuario = false;
        for (ModelSalas s : infoSalas) { //busca em todas as salas qual delas o usuario que mandou a requisição se encontra e remove ele da lista
            if (!flagUsuario) {
                System.out.println("            procurando sala...");
                for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                    System.out.println("                procurando usuarios conectados...");
                    if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                        flagUsuario = true;
                        System.out.println("                    usuario encontrado...");
                        s.getUsuariosConectados().remove(u);
                        System.out.println("                    usuario removido...");
                        atualizacaoUsuariosSala(s, u, false); //atualiza todos os outros da sala da saída do cliente
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }
    
    private static void atualizacaoUsuariosSala(ModelSalas s, ModelUsuarioConectado novoAcesso, boolean adicionar) {
        //para todos os cliente conectados na sala, manda uma mensagem com adicionar/remover o usuario novoAcesso
        System.out.println("                        atualizando lista de usuarios da sala...");
        JSONObject novoConectadoSala = new JSONObject();
        novoConectadoSala.put("tipo", 10);
        novoConectadoSala.put("adicionar", adicionar); //pode ser true ou false
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
                System.out.println("Falha de envio de datagrama");
            }
        }
    }
    
    public static void enviarStatusVotacao(InetAddress address, int port, ModelSalas s) {
        JSONObject votacao = new JSONObject();
        votacao.put("tipo", 9);
        if (s.getInfoSalas().getBoolean("status")) {
            votacao.put("acabou", false);
        } else {
            votacao.put("acabou", true); //envia o status da votação mesmo se for false pois estava confuso essa parte do protocolo
        }
        JSONArray opcoes = new JSONArray();
        for (Object o : s.getInfoSalas().getJSONArray("opcoes")) {
            //busca na struct os nomes das opções de voto e usa como key para a quantidade de votos na opção
            String nomeOpcao = new JSONObject(o.toString()).getString("nome");
            int qtdVotos = 0;
            for (JSONObject jo : s.getVotos()) {
                if (jo.getString("voto").equals(nomeOpcao)) {
                    qtdVotos = qtdVotos + 1;
                }
            }
            JSONObject opcao = new JSONObject();
            opcao.put(nomeOpcao, qtdVotos);
            opcoes.put(opcao);
        }
        votacao.put("resultados", opcoes);
        byte[] buffer = new byte[1024];
        buffer = votacao.toString().getBytes();
        //envia resultado como array de json
        try {
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length, address, port);
            socketServidor.send(reply);
            System.out.println("status votacao enviada: " + votacao.toString());
        } catch (IOException ex) {
            System.out.println("Falha de envio de datagrama");
        }
    }
    
    private void streamMensagensSala(DatagramPacket request, ArrayList<JSONObject> mensagens) {
        new Thread() { //cria nova thread para não ocupar demais o servidor
            public void run() {
                for (JSONObject m : mensagens) { //percorre a lista de json de parametro e envia com o tipo e tamanho
                    m.put("tipo", 12);
                    m.put("tamanho", mensagens.size());
                    byte[] buffer = new byte[1024];
                    buffer = m.toString().getBytes();
                    try {
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                        socketServidor.send(reply);
                        System.out.println("mensagem de chat enviada: " + m);
                    } catch (IOException ex) {
                        System.out.println("Falha de envio de datagrama");
                    }
                }
            }
        }.start();
    }
    
    private void pedidoMensagemEspecifica(DatagramPacket request, JSONObject received) {
        new Thread() { //cria nova thread para não sobrecarregar o servidor
            public void run() {
                ModelSalas sala = infoSalas.get(received.getInt("id_sala")); //busca sala do id
                JSONObject m = new JSONObject(sala.getMensagens().get(received.getInt("id_msg")).toString()); //busca mensagem do id
                byte[] buffer = new byte[1024];
                buffer = m.toString().getBytes();
                //envia a mensagem específica
                try {
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
                    socketServidor.send(reply);
                    System.out.println("mensagem de chat enviada: " + m);
                } catch (IOException ex) {
                    System.out.println("Falha de envio de datagrama");
                }
            }
        }.start();
    }
    
    private void mensagemChatServidor(DatagramPacket request, JSONObject received) {
        ModelUsuarioConectado remetenteMensagem = null;
        for (ModelUsuarioConectado u : usuariosConectados) { //varre os usuarios conectados no servidor
            if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                System.out.println("remetente encontrado na lista de conectados...");
                remetenteMensagem = u;
                for (ModelSalas s : infoSalas) { //varre as salas procurando pela que contém o usuario que enviou a mensagem
                    System.out.println("procurando sala");
                    if (s.getUsuariosConectados().contains(remetenteMensagem)) {
                        System.out.println("sala encontrada re-enviando mensagem chat");
                        received.remove("tipo");
                        received.put("id", s.getMensagens().size());
                        received.put("timestamp", Long.toString(Instant.now().getEpochSecond()));
                        s.addMensagens(received);
                        salvarMensagemSalaArquivo(s.getInfoSalas().getInt("id"), received);
                        //adiciona informações das mensagens que o servidor precisa
                        received.put("tipo", 12);
                        received.put("tamanho", s.getMensagens().size());
                        byte[] buffer = new byte[1024];
                        buffer = received.toString().getBytes();
                        for (ModelUsuarioConectado uc : s.getUsuariosConectados()) { //reenvia para todos os clientes da sala
                            try {
                                DatagramPacket reply = new DatagramPacket(buffer, buffer.length, uc.getEndrecoIP(), uc.getPorta());
                                socketServidor.send(reply);
                                System.out.println("mensagem de chat enviada: " + received);
                            } catch (IOException ex) {
                                System.out.println("Falha de envio de datagrama");
                            }
                        }
                    }
                    System.out.println("    procurando...");
                }
            }
        }
    }
    
    private static void computarVoto(DatagramPacket request, JSONObject received) {
        for (ModelSalas s : infoSalas) { //varre lista de salas
            if (s.getInfoSalas().getInt("id") == received.getInt("sala")) { //encontra sala do id
                for (ModelUsuarioConectado u : s.getUsuariosConectados()) { //varre lista de usuarios na sala
                    if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort() && u.isAtivo()) { //encontra usuario da requisição
                        for (Object o : s.getInfoSalas().getJSONArray("opcoes")) { //varre opções de votação
                            if (received.getString("opcao").equals(new JSONObject(o.toString()).getString("nome"))) {
                                System.out.println("opcao valida, computando voto...");
                                JSONObject votoUsuario = new JSONObject();
                                votoUsuario.put("ra", u.getRa());
                                votoUsuario.put("voto", received.getString("opcao"));
                                boolean flag = false;
                                for (JSONObject jo : s.getVotos()) {
                                    if (jo.getString("ra").equals(votoUsuario.getString("ra"))) {
                                        System.out.println("voto atualizado");
                                        flag = true; //atualiza o voto caso já tenha um com o ra da requisição
                                        substituirVotoArquivo(jo, votoUsuario, s.getInfoSalas().getInt("id"));
                                        jo.remove("opcao");
                                        jo.put("opcao", votoUsuario.getString("voto"));
                                    }
                                }
                                if (flag == false) {
                                    System.out.println("novo voto computado");
                                    s.addVoto(votoUsuario); //adiciona voto novo caso seja a primeira vez que vota
                                    salvarVotoSalaArquivo(s.getInfoSalas().getInt("id"), votoUsuario);
                                }
                                break;
                            }
                        }
                        byte[] buffer = new byte[1024];
                        buffer = received.toString().getBytes();
                        try {
                            DatagramPacket ackVoto = new DatagramPacket(buffer, buffer.length, u.getEndrecoIP(), u.getPorta());
                            socketServidor.send(ackVoto);
                            System.out.println("resposta ack voto: " + received.toString());
                        } catch (IOException ex) {
                                System.out.println("Falha de envio de datagrama");
                        }
                    }
                    //enviarStatusVotacao(u.getEndrecoIP(), u.getPorta(), s);
                }
            }
        }
    }
    
    private static void salvarMensagemSalaArquivo(int id, JSONObject mensagem) { //escreve as mensagens enviadas pelo servidor no arquivo de mensagens
        BufferedWriter saida = null;
        try {
            saida = new BufferedWriter(new FileWriter("./salas/mensagens/" + id, true));
            saida.write(mensagem.toString() + "\n");
            saida.flush();
        } catch (IOException ex) {
            System.out.println("Falha de abertura de arquivo de mensagens");
        } finally {
            if (saida != null) {
                try {
                    saida.close();
                } catch (IOException ex) {
                    System.out.println("Falha de fechamento de arquivo");
                }
            }
        }
    }
    
    private static void salvarSalaArquivo(JSONObject sala) { //salva as informações da sala no arquivo de salas
        BufferedWriter saida = null;
        try {
            saida = new BufferedWriter(new FileWriter("./salas/salas", true));
            saida.write(sala.toString() + "\n");
            saida.flush();
        } catch (IOException ex) {
            System.out.println("Falha de abertura de arquivo de mensagens");
        } finally {
            if (saida != null) {
                try {
                    saida.close();
                } catch (IOException ex) {
                    System.out.println("Falha de fechamento de arquivo");
                }
            }
        }
    }
    
    private static void criarArquivoSala(int id) { //cria arquivo de mensagens da nova sala criada, devo ter errado na nomenclatura na hora de programar
        String data = "";
        try {
            Files.write(Paths.get("./salas/mensagens/" + Integer.toString(id)), data.getBytes());
        } catch (IOException ex) {
            System.out.println("Falha de escrita de arquivo de salas");
        }
    }
    
    private static void criarArquivoVotos(int id) { //cria arquivo de votos da nova sala criada
        String data = "";
        try {
            Files.write(Paths.get("./salas/votos/" + Integer.toString(id)), data.getBytes());
        } catch (IOException ex) {
            System.out.println("Falha de escrita de arquivo de votos");
        }
    }
    
    private static void salvarVotoSalaArquivo(int id, JSONObject mensagem) { //salva os votos no arquivo de votos
        BufferedWriter saida = null;
        try {
            saida = new BufferedWriter(new FileWriter("./salas/votos/" + id, true));
            saida.write(mensagem.toString() + "\n");
            saida.flush();
        } catch (IOException ex) {
            System.out.println("Falha de escrita de arquivo de salas");
        } finally {
            if (saida != null) {
                try {
                    saida.close();
                } catch (IOException ex) {
                    System.out.println("Falha de fechamento de arquivo");
                }
            }
        }
    }
    
    private static void substituirVotoArquivo(JSONObject votoAntigo, JSONObject votoNovo, int id) { //substitui o voto no arquivo, mesmo das salas porém para os votos
        System.out.println("computando mudança de voto      " + votoAntigo.toString());
        System.out.println(votoNovo.toString());
        String line;
        StringBuilder buffer = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./salas/votos/" + id));
            line = bufferedReader.readLine();
            while(line != null){
                System.out.println("lendo votos antigos     " + line);
                if (line.equals(votoAntigo.toString())) {
                    System.out.println("voto a ser mudado encontrado");
                    buffer.append(votoNovo.toString().concat("\n"));
                } else {
                    buffer.append(line.concat("\n"));
                }
                line = bufferedReader.readLine();
            }            
            BufferedWriter saida = null;
            saida = new BufferedWriter(new FileWriter("./salas/votos/" + id, false));
            saida.write(buffer.toString());
            saida.flush();
        } catch (FileNotFoundException ex) {
            System.out.println("Falha ao encontrar aquivo de votos");
        } catch (IOException ex) {
            System.out.println("Falha de escrita de arquivo");
        }
    }
    
    private static void broadcast(byte[] buffer) { //broadcast para todos os clientes conectados no servidor
        for (ModelUsuarioConectado u : usuariosConectados) {
            try {
                DatagramPacket atualizacaoListaSalas = new DatagramPacket(buffer, buffer.length, u.getEndrecoIP(), u.getPorta());
                socketServidor.send(atualizacaoListaSalas);
            } catch (IOException ex) {
                    System.out.println("Falha de envio de datagrama");
            }
        }
    }
    
    public static String sha256(String base) { //criptografia sha256
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
            System.out.println("Falha de escrita de arquivo");
            return null;
        }
    }
    
    public static void iniciarPing(DatagramPacket request, JSONObject received) { //função para setar o ping dos clientes que enviam como positivo
        for (ModelUsuarioConectado u : usuariosConectados) { //percorre lista procurando o cliente que enviou o ping
            System.out.println("checando usuarios...");
            if (u.getEndrecoIP().equals(request.getAddress()) && u.getPorta() == request.getPort()) {
                System.out.println("setando usuario ativo...");
                u.setAtivo(true); //seta como ativo
                break;
            }
        }
        if (received.getInt("sala") != -1) { //caso esteja em uma sala procura nas salas qual ele se encontra e seta como ativo também
            ModelSalas sala = null;
            for (ModelSalas s : infoSalas) { //encontra a sala 
                if (s.getInfoSalas().getInt("id") == received.getInt("sala")) {
                    sala = s;
                    break;
                }
            }
            if (sala != null) {
                for (ModelUsuarioConectado u : sala.getUsuariosConectados()) { //varre os usuarios e seta como ativo quem enviou ping
                    if (u.getEndrecoIP().equals(request.getAddress().toString()) && u.getPorta() == request.getPort()) {
                        System.out.println("setando usuario ativo na sala...");
                        u.setAtivo(true);
                    }
                }
            }
        }
    }
    
    public static void contagemPing() {
        new Thread() { //nova thread
            public void run() {
                while (true) {
                    try {
                        for (ModelUsuarioConectado u : usuariosConectados) {
                            System.out.println("setando usuario como inativo...");
                            u.setAtivo(false); //seta todos do servidor como inativos
                        }
                        for (ModelSalas s : infoSalas) {
                            for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                                System.out.println("Setando usuario na sala como inativo...");
                                u.setAtivo(false); //seta todos de todas as sala como inativos
                            }
                        }
                        Thread.sleep(30000); //espera 30 segundos
                        for (ModelSalas s : infoSalas) { //varre as salas e remove todos os clientes inativos de cada sala
                            for (ModelUsuarioConectado u : s.getUsuariosConectados()) {
                                if (!u.isAtivo()) { 
                                    System.out.println("removendo usuario inativo da sala...");
                                    s.getUsuariosConectados().remove(u);
                                    atualizacaoUsuariosSala(s, u, false);
                                }
                            }
                        }
                        for (ModelUsuarioConectado u : usuariosConectados) { //remove todos os cliente inativos do servidor
                            if (!u.isAtivo()) {
                                System.out.println("removendo usuario inativo do servidor...");
                                usuariosConectados.remove(u);
                                updateModeloTabelaUsuarios();
                            }
                        }
                    } catch (InterruptedException ex) {
                        System.out.println("Falha de thread de ping");
                    }
                }
            }
        }.start();
    }
}
