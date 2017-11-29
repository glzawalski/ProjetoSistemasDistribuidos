/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import java.util.ArrayList;
import org.json.JSONObject;

/**
 *
 * @author gabriel
 */
public class ModelSalas {
    private JSONObject infoSalas;
    private ArrayList<JSONObject> mensagens;
    private ArrayList<JSONObject> votos;
    private ArrayList<ModelUsuarioConectado> usuariosConectados;

    public ModelSalas() {
        mensagens = new ArrayList<>();
        usuariosConectados = new ArrayList<>();
        votos = new ArrayList<>();
    }
    /**
     * @return the infoSalas
     */
    public JSONObject getInfoSalas() {
        return infoSalas;
    }

    /**
     * @param infoSalas the infoSalas to set
     */
    public void setInfoSalas(JSONObject infoSalas) {
        this.infoSalas = infoSalas;
    }

    /**
     * @return the mensagens
     */
    public ArrayList<JSONObject> getMensagens() {
        return mensagens;
    }

    /**
     * @param mensagens the mensagens to set
     */
    public void setMensagens(ArrayList<JSONObject> mensagens) {
        this.mensagens = mensagens;
    }
    
    public void addMensagens(JSONObject mensagem) {
        mensagens.add(mensagem);
    }

    /**
     * @return the usuariosConectados
     */
    public ArrayList<ModelUsuarioConectado> getUsuariosConectados() {
        return usuariosConectados;
    }

    /**
     * @param usuariosConectados the usuariosConectados to set
     */
    public void setUsuariosConectados(ArrayList<ModelUsuarioConectado> usuariosConectados) {
        this.usuariosConectados = usuariosConectados;
    }
    
    public void addUsuariosConectados(ModelUsuarioConectado novoUsuario) {
        usuariosConectados.add(novoUsuario);
    }

    /**
     * @return the votos
     */
    public ArrayList<JSONObject> getVotos() {
        return votos;
    }

    /**
     * @param votos the votos to set
     */
    public void setVotos(ArrayList<JSONObject> votos) {
        this.votos = votos;
    }
    
    public void addVoto(JSONObject voto) {
        votos.add(voto);
    }
}
