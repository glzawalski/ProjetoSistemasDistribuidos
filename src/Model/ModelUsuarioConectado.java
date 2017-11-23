/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import java.net.InetAddress;

/**
 *
 * @author gabriel
 */
public class ModelUsuarioConectado {
    private String nome;
    private String ra;
    private InetAddress endrecoIP;
    private int porta;

    public ModelUsuarioConectado(String nome, String ra, InetAddress enderecoIP, int porta) {
        this.nome = nome;
        this.ra = ra;
        this.endrecoIP = enderecoIP;
        this.porta = porta;
    }
    
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public InetAddress getEndrecoIP() {
        return endrecoIP;
    }

    public void setEndrecoIP(InetAddress endrecoIP) {
        this.endrecoIP = endrecoIP;
    }
    
    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }

    /**
     * @return the ra
     */
    public String getRa() {
        return ra;
    }

    /**
     * @param ra the ra to set
     */
    public void setRa(String ra) {
        this.ra = ra;
    }
}
