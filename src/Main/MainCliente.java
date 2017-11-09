package Main;

import View.ViewLogin;
import View.ViewPrincipal;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainCliente {
    private int porta;
    private InetAddress host;
    
    public static void main(String[] args) throws InterruptedException, UnknownHostException{
        new ViewLogin().setVisible(true);
    }
}
