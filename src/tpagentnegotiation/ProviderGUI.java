package tpagentnegotiation;

import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.*;

class ProviderGUI extends JFrame {	
    private ProviderAgent provider;

    private JTextField departureField, destinationField, desiredField, minField;
    private JFormattedTextField dateField;
    private SimpleDateFormat dateFormat;

    ProviderGUI(ProviderAgent a) {
        super(a.getLocalName());

        provider = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(5, 2));
        
        p.add(new JLabel("Departure:"));
        departureField = new JTextField(15);
        p.add(departureField);
        
        p.add(new JLabel("Destination:"));
        destinationField = new JTextField(15);
        p.add(destinationField);
        
        p.add(new JLabel("Date:"));
        dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        dateField = new JFormattedTextField(dateFormat);
        dateField .addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
              char c = e.getKeyChar();
              if (!((c >= '0') && (c <= '9') ||
                (c == KeyEvent.VK_BACK_SPACE) ||
                (c == KeyEvent.VK_DELETE) || (c == KeyEvent.VK_SLASH))) {
                JOptionPane.showMessageDialog(null, "Please Enter Valid");
                e.consume();
                }
            }
        });           
        p.add(dateField);
        
        p.add(new JLabel("Desired Price:"));
        desiredField = new JTextField(15);
        p.add(desiredField);
        
        p.add(new JLabel("Minimum Price:"));
        minField = new JTextField(15);
        p.add(minField);
        
        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                try {
                    String departure = departureField.getText().trim();
                    String destination = destinationField.getText().trim();
                    String date = dateField.getText().trim();
                    String[] parts = date.split("/");
                    int desiredPrice = Integer.parseInt(desiredField.getText().trim());
                    int minPrice = Integer.parseInt(minField.getText().trim());
                    provider.addWish(new Wish(new Ticket(destination, departure, new GregorianCalendar(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])).getTime()), desiredPrice, minPrice));
                    departureField.setText("");
                    destinationField.setText("");
                    dateField.setText("");
                    desiredField.setText("");
                    minField.setText("");
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(ProviderGUI.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
                }
            }
        } );
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes 
        // the GUI using the button on the upper right corner	
        addWindowListener(new	WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                    provider.doDelete();
            }
        } );

        setResizable(false);
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }	
}
