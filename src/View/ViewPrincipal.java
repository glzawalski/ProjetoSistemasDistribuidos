package View;

import Model.ModelCliente;
import View.ViewCriarSala;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

public class ViewPrincipal extends JFrame {

    private DefaultTableModel modeloTabelaSalas;
    private DefaultTableModel modeloTabelaUsuarios;
    private ModelCliente cliente;
    
    public ViewPrincipal() throws InterruptedException {
        initComponents();
    }
    
    public void setCliente(ModelCliente client){
        cliente = client;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        paneSalas = new javax.swing.JScrollPane();
        tabelaSalas = new javax.swing.JTable();
        LabelUser = new javax.swing.JLabel();
        buttonLogout = new javax.swing.JButton();
        buttonCriarSala = new javax.swing.JButton();
        deslogado = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        tabelaSalas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        paneSalas.setViewportView(tabelaSalas);

        LabelUser.setFont(new java.awt.Font("Comic Sans MS", 1, 18)); // NOI18N
        LabelUser.setToolTipText("");

        buttonLogout.setText("Logout");
        buttonLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLogoutActionPerformed(evt);
            }
        });

        buttonCriarSala.setText("Criar Sala");
        buttonCriarSala.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCriarSalaActionPerformed(evt);
            }
        });

        deslogado.setFont(new java.awt.Font("Comic Sans MS", 1, 18)); // NOI18N
        deslogado.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        deslogado.setText("DESCONECTADO");
        deslogado.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(LabelUser, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonLogout))
                        .addComponent(paneSalas, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(buttonCriarSala)
                    .addComponent(deslogado, javax.swing.GroupLayout.PREFERRED_SIZE, 425, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LabelUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonLogout, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCriarSala, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(23, 23, 23)
                .addComponent(paneSalas, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addComponent(deslogado)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLogoutActionPerformed
        cliente.fazerLogout();
        JOptionPane.showMessageDialog(null, "Logout executado.", "Logout", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_buttonLogoutActionPerformed

    private void buttonCriarSalaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCriarSalaActionPerformed
        new ViewCriarSala(cliente).setVisible(true);
    }//GEN-LAST:event_buttonCriarSalaActionPerformed
    
    public void atualizarUser(String username){
        LabelUser.setText(username);
    }

    public void atualizarListaSalas(DefaultTableModel model){
        modeloTabelaSalas = model;
        tabelaSalas.setModel(modeloTabelaSalas);
    }
    
    public void logout(){
        buttonCriarSala.setEnabled(false);
        buttonLogout.setEnabled(false);
        tabelaSalas.setEnabled(false);
        deslogado.setEnabled(true);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel LabelUser;
    private javax.swing.JButton buttonCriarSala;
    private javax.swing.JButton buttonLogout;
    private javax.swing.JLabel deslogado;
    private javax.swing.JScrollPane paneSalas;
    private javax.swing.JTable tabelaSalas;
    // End of variables declaration//GEN-END:variables

}
