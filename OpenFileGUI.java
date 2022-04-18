import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class OpenFileGUI {
    private JFrame window;
    private JPanel canvas;
    private JButton dreamify;
    private JComboBox styles;
    private String dreamified;
    private BufferedImage origImage;
    private JFileChooser chooseFile = new JFileChooser(System.getProperty("user.dir"));
    private int myStyle;

    private OpenFileGUI() throws IOException {
        setupGUI();
    }

    public static void main(String[] args) throws IOException {
        
        OpenFileGUI openFile = new OpenFileGUI();
        

    }

    private void setupGUI() throws IOException {
        window = new JFrame("DeepDream Image Maker");
        window.setSize(700, 700);
        window.setResizable(false);
        window.setLayout(new BorderLayout());

        canvas = new JPanel();
        canvas.setLayout(new BorderLayout());
        canvas.setSize(700, 580);
        canvas.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BorderLayout());
        Border padding = new EmptyBorder(10, 10, 10, 10);
        Border topLine = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY);
        buttons.setBorder(new CompoundBorder(topLine, padding));

        JButton openFile = new JButton("Open File...");
        dreamify = new JButton("Dreamify");
        dreamify.setEnabled(false);

        String[] styleStrings = {"Style 1", "Style 2", "Style 3"};
        
        styles = new JComboBox(styleStrings);
        
        buttons.add(styles, BorderLayout.CENTER);

        styles.setEnabled(false);

        // window.add(canvas, BorderLayout.NORTH);

        openFile.setActionCommand("OPEN");
        openFile.addActionListener(new ButtonClickListener());

        dreamify.setActionCommand("DREAMIFY");
        dreamify.addActionListener(new ButtonClickListener());
        
        styles.setActionCommand("STYLE");
        styles.addActionListener(new ButtonClickListener());
        
        buttons.add(openFile, BorderLayout.WEST);
        buttons.add(dreamify, BorderLayout.EAST);

        // window.add(canvas, BorderLayout.NORTH);
        window.add(buttons, BorderLayout.SOUTH);
        window.setVisible(true);

        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event){
               System.exit(0);
            }        
         }); 

    }

    private void dreamify() throws IOException {
        // grab file name and path
        String filePath = chooseFile.getSelectedFile().toString();
            
        Boolean deleteFile = false;

        // convert image to jpg if necessary
        if (filePath.substring(filePath.length() - 3).equals("png")) {
            
            // create images objects for conversion
            BufferedImage baseImage = ImageIO.read(chooseFile.getSelectedFile());
            BufferedImage newImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            
            Graphics2D editImage = newImage.createGraphics();
            
            // convert to png
            editImage.setColor(Color.WHITE);
            editImage.fillRect(0, 0, newImage.getWidth(), newImage.getWidth());
            editImage.drawImage(baseImage, 0, 0, null);
            editImage.dispose();
            
            // create file and adjust path
            filePath = chooseFile.getSelectedFile().toString().replace(".png", ".jpg");
            ImageIO.write(newImage, "jpg", new File(filePath));

            deleteFile = true;
        }
        
        // styles with different layer combinations (taken from eistein-candidates)
        int[][] stylesSet = new int[3][2];
        stylesSet[0] = new int[]{8, 9};
        stylesSet[1] = new int[]{8, 1};
        stylesSet[2] = new int[]{9, 6};

        // start python script with file path and layers as arguments
        // last argument signals this is not the loop file
        ProcessBuilder startProcess = new ProcessBuilder("python", System.getProperty("user.dir") 
                                        + "\\main.py", filePath, ""+stylesSet[myStyle][0], ""+stylesSet[myStyle][1], "0");
        Process pythonScript = startProcess.start();

        
        // read output from script for debugging
        BufferedReader debugging = new BufferedReader(new InputStreamReader(pythonScript.getInputStream()));
        String pythonOutput = null;
        while((pythonOutput = debugging.readLine()) != null) {
            if (pythonOutput.contains("&&&")) {
                dreamified = pythonOutput.substring(3, pythonOutput.length());
            }
        }

        // delete newImage jpg file if exists
        if (deleteFile) {
            File newFile = chooseFile.getSelectedFile();
            newFile.delete();
        }

        origImage = ImageIO.read(new File("./output/" + dreamified));
        setImage();
        dreamify.setEnabled(false);
        styles.setEnabled(false);
        

    }
    
    private void setImage() throws IOException {
            double imageRatio;
            canvas.setSize(700, 580);
            if (origImage.getWidth() > origImage.getHeight()) {
                imageRatio = (double)(canvas.getWidth()) / origImage.getWidth();
            } else{
                imageRatio = (double)(canvas.getHeight()) / origImage.getHeight();
            }
            Image resized = origImage.getScaledInstance((int)(origImage.getWidth()*imageRatio), (int)(origImage.getHeight()*imageRatio), Image.SCALE_SMOOTH);
            ImageIcon newImage = new ImageIcon(resized);
            canvas.removeAll();
            
            canvas.add(new JLabel(newImage), BorderLayout.CENTER);
            window.add(canvas, BorderLayout.NORTH);
            canvas.revalidate();
            dreamify.setEnabled(true);
    }

    private void openFile() throws IOException{
        // create window to select a jpg or png file
        // JFileChooser chooseFile = new JFileChooser(System.getProperty("user.dir"));
        chooseFile.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter("JPG file", "jpg");
        chooseFile.addChoosableFileFilter(extensionFilter);

        int response = chooseFile.showOpenDialog(null);
        if (response == JFileChooser.APPROVE_OPTION) {
            origImage = ImageIO.read(chooseFile.getSelectedFile());
            setImage();
            styles.setEnabled(true);
        }
    }

    private class ButtonClickListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            try {
                if (command.equals("OPEN")) {
                    openFile();
                } else if (command.equals("DREAMIFY")) {
                    dreamify();
                }else if (command.equals("STYLE")) {
                    if (((String)styles.getSelectedItem()).contains("1")) myStyle = 0;
                    else if (((String)styles.getSelectedItem()).contains("2")) myStyle = 1;
                    else if (((String)styles.getSelectedItem()).contains("3")) myStyle = 2;
                }
            } catch(IOException ei) {
            }

        }		
     }

}
