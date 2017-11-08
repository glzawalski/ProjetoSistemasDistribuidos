package Main;

import ViewCliente.ViewLogin;
import ViewCliente.ViewPrincipal;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainCliente {
    private int porta;
    private InetAddress host;
    
    public static void main(String[] args) throws InterruptedException, UnknownHostException{
        new ViewLogin().setVisible(true);
    }
}
