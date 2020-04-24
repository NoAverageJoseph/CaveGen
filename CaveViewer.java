import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Document;

public class CaveViewer {

    static CaveViewer caveViewer;

    static boolean active = false;
    Thread caveGenThread = null;

    ArrayList<String> nameBuffer = new ArrayList<String>();
    ArrayList<BufferedImage> imageBuffer = new ArrayList<BufferedImage>();
    StringBuilder reportBuffer = new StringBuilder();
    int currentImage = 0;
    boolean firstImageDisplayedYet = false;
    static boolean guiOnly = false;

    public static void main(String args[]) {
        caveViewer = new CaveViewer();
        caveViewer.run();
    }

    final JFrame jfr = new JFrame("CaveViewer");
    final JButton jbuttonRun = new JButton("Run");
    final JTextPane jtextReport = new JTextPane();
    KeyListener keyListener, keyListener2;

    final JFrame jfrView = new JFrame("");
    final NavigableImagePanel viewPanel = new NavigableImagePanel();

    final String fontfamily = "Arial, sans-serif";
    final String fontfamilyMono = "Monospaced";
    final Font font = new Font(fontfamily, Font.PLAIN, 12);
    final Font font10 = new Font(fontfamily, Font.PLAIN, 10);
    final Font fontMono = new Font(fontfamilyMono, Font.PLAIN, 12);
	final Font fontMono16 = new Font(fontfamilyMono, Font.PLAIN, 16);
    final Font fontMono10 = new Font(fontfamilyMono, Font.PLAIN, 10);

    class Arg {
        String name;
        JTextPane jtext = new JTextPane();
        JCheckBox jCheckBox = null;
        JTextField jTextField = null;
        JComboBox<String> jComboBox = null;

        Arg(String config, int x, int y) {
            String[] s = config.split(",");
            name = s[0].toLowerCase();
            jtext.setBounds(x+111, y-3, s[0].length() < 10 ? 85 : 115, 20);
            jtext.setText(s[0]);
            jtext.setFont(font);
            //jtext.setMargin(new Insets(1, 1, 1, 1));
            jtext.setEditable(false);
            jtext.setContentType("text/plain");
            jtext.setBackground(null);
            jtext.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            jtext.addKeyListener(keyListener);
            jfr.add(jtext);

            for (int i = 1; i < s.length; i++) {
                if (s[i].equals("#")) {
                    jCheckBox = new JCheckBox("",false);
                    jCheckBox.setBounds(x+93,y-2,20,20);
                    jCheckBox.addKeyListener(keyListener);
                    jfr.add(jCheckBox);
                } else if (s[i].equals("_")) {
                    jTextField = new JTextField();
                    jTextField.setBounds(x+10, y, 100, 18);
                    jTextField.setFont(font10);
                    jTextField.addKeyListener(keyListener2);
                    //jTextField.setMargin(new Insets(1, 1, 1, 1));
                    jfr.add(jTextField);
                } else if (s[i].equals("dropdown")) {
                    String[] options = s[++i].split("[|]+");
                    jComboBox = new JComboBox<String>(options);
                    jComboBox.setFont(font10);
                    jComboBox.setEditable(s[++i].equals("editable"));
                    jComboBox.setBounds(x+10, y, 100, 18);
                    jComboBox.addKeyListener(keyListener2);
                    jComboBox.getEditor().getEditorComponent().addKeyListener(keyListener2);
                    jfr.add(jComboBox);
                }
            }
        }
    }

    String caveArgs = "EC,SCx,FC,HoB,WFG,BK,SH,CoS,GK,SR,SC,CoC,HoH,DD,Story,PoD,CMAL,All,CH1,CH2,CH3,CH4,CH5,CH6,CH7,CH8,CH9,CH10,CH11,CH12,CH13,CH14,CH15,CH16,CH17,CH18,CH19,CH20,CH21,CH22,CH23,CH24,CH25,CH26,CH27,CH28,CH29,CH30,AT,IM,AD,GD,FT,WF,GdD,SC,AS,SS,CK,PoW,PoM,EA,DD,PP";
    String argString1 = "Output,dropdown,GUI only|None (no images)|Cave Folder|Seed Folder|Both Folders,x;Cave,dropdown," + caveArgs.replace(",", "|") + ",editable;Sublevel,dropdown,1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|All,x;Num,_;Seed,_";
    String argString2 = "AdditionalArgs,_;Region,dropdown,US|JPN|PAL,x;FindGoodLayouts,_;ConsecutiveSeeds,#;CaveInfoReport,#";
    String argString3 = "NoImages,NoStats,NoPrints,NoWayPointGraph,ChallengeMode,StoryMode,DrawSpawnPoints,DrawAngles,DrawScores,DrawEnemyScores,DrawDoorLinks,DrawAllScores,DrawDoorIds,DrawWayPoints,DrawAllWayPoints,DrawTreasureGauge,DrawHoleProbs,DrawNoPlants,DrawNoObjects,DrawNoWaterBox,DrawNoBuriedItems,DrawNoItems,DrawNoTekis,DrawNoGates,DrawNoHoles,DrawNoFallType,Aggregator,AggRooms,WriteMemo,ReadMemo,251";
    ArrayList<Arg> args = new ArrayList<Arg>();
    HashMap<String,Arg> argMap = new HashMap<String,Arg>();

    void run() {

        active = true;

        jfr.getContentPane().setLayout(null);
		jfr.setSize(434, 555);
        jfr.setResizable(false);
        jfr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_EQUALS) {

				}
				if (e.getKeyCode() == KeyEvent.VK_MINUS) {

                }
                if (e.getKeyCode() == KeyEvent.VK_W) {

				}
				if (e.getKeyCode() == KeyEvent.VK_A) {

                }
                if (e.getKeyCode() == KeyEvent.VK_S) {

				}
				if (e.getKeyCode() == KeyEvent.VK_D) {

                }
                if (e.getKeyCode() == KeyEvent.VK_X) {
                    jfrView.setVisible(false);
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    runCaveGen();
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_PERIOD) {
                    nextImg();
                }
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_COMMA) {
                    prevImg();
				}
			}
        };
        keyListener2 = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    runCaveGen();
                }
			}
        };
        jfr.addKeyListener(keyListener);
        jfrView.addKeyListener(keyListener);

        jbuttonRun.setFont(font);
		jbuttonRun.setBounds(10, 10, 400, 20);
        jfr.add(jbuttonRun);
        
        jbuttonRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				runCaveGen();
			}
		});
        
        jtextReport.setBounds(10, 375, 400, 135);
		jtextReport.setFont(font);
		jtextReport.setMargin(new Insets(3, 3, 3, 3));
		jtextReport.setEditable(false);
		JScrollPane jtextChatSP = new JScrollPane(jtextReport);
		jtextChatSP.setBounds(10, 375, 400, 135);
		jtextReport.setContentType("text/plain");
		jtextReport.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        jfr.add(jtextChatSP);
        
        jfrView.getContentPane().setLayout(null);
		jfrView.setSize(100, 100);
        jfrView.setResizable(true);

        NavigableImagePanel.navigationImageEnabled = false;
        viewPanel.setBounds(0, 0, 100, 100);
        jfrView.add(viewPanel);
        jfrView.setVisible(false);
        jfrView.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        jfrView.addComponentListener(new ComponentAdapter() {  
            public void componentResized(ComponentEvent evt) {
                viewPanel.setBounds(0,0,jfrView.getWidth()-14,jfrView.getHeight()-37);
            }
        });

        int y = 35;
        int x = 0;
        for (String s: argString1.split("[;]")) {
            Arg a = new Arg(s,x,y);
            y += 20;
            args.add(a);
            argMap.put(s.split(",")[0].toLowerCase(), a);
        }

        y = 35;
        x = 190;
        for (String s: argString2.split("[;]")) {
            Arg a = new Arg(s,x,y);
            y += 20;
            args.add(a);
            argMap.put(s.split(",")[0].toLowerCase(), a);
        }

        y = 145;
        x = -85;
        for (String s: argString3.split("[,]")) {
            Arg a = new Arg(s+",#",x,y);
            y += 20;
            args.add(a);
            argMap.put(s.split(",")[0].toLowerCase(), a);
            if (y > 345) {
                x += 134;
                y = 145;
            }
        }

        jfr.revalidate();
		jfr.repaint();
		jfr.setVisible(true);
    }

    void nextImg() {
        currentImage += 1;
        currentImage = Math.min(imageBuffer.size() - 1, currentImage);
        currentImage = Math.max(0, currentImage);
        dispCurrentImage();
    }

    void prevImg() {
        currentImage -= 1;
        currentImage = Math.max(0, currentImage);
        dispCurrentImage();
    }

    void dispCurrentImage() {
        EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
                if (imageBuffer.size() > 0) {
                    try {
                        BufferedImage img = imageBuffer.get(currentImage);
                        viewPanel.setImage(img);
                        int w = img.getWidth();
                        int h = img.getHeight();
                        System.out.println(w + " " + h);
                        float scale = 900.0f / Math.max(Math.max(900, w), h);
                        jfrView.setSize((int)(w * scale) + 14, (int)(h * scale) + 37);
                        jfrView.setVisible(true);
                        jfrView.setTitle(nameBuffer.get(currentImage) + " (" + (currentImage+1) + "/" + nameBuffer.size() + ")");
                        viewPanel.setBounds(0, 0, (int)(w * scale), (int)(h * scale));
                        firstImageDisplayedYet = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    void update() {
        if (!CaveViewer.active) return;
        final String s = reportBuffer.toString();
        reportBuffer = new StringBuilder();
        EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
                Document doc = jtextReport.getStyledDocument();
                try {
                    doc.insertString(doc.getLength(), s, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if (!firstImageDisplayedYet) {
            prevImg();
        }
    }

    @SuppressWarnings( "deprecation" )
    void runCaveGen() {
        try {
            if (caveGenThread != null && caveGenThread.isAlive()) {
                caveGenThread.stop();
                //Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        nameBuffer.clear();
        imageBuffer.clear();
        reportBuffer = new StringBuilder();
        jtextReport.setText("");
        currentImage = 0;
        firstImageDisplayedYet = false;

        StringBuilder s = new StringBuilder();
        for (Arg a: args) {
            if (a.name.equals("output")) {
                guiOnly = false;
                switch(a.jComboBox.getSelectedIndex()) {
                    case 0:
                        guiOnly = true;
                        s.append("cave ");
                        break;
                    case 1:
                        s.append("none ");
                        break;
                    case 2:
                        s.append("cave ");
                        break;
                    case 3:
                        s.append("seed ");
                        break;
                    case 4:
                        s.append("both ");
                } 
            } else if (a.name.equals("cave")) {
                s.append((String)(a.jComboBox.getSelectedItem()) + " ");
            } else if (a.name.equals("sublevel")) {
                if (((String)(a.jComboBox.getSelectedItem())).equalsIgnoreCase("all"))
                    s.append("0 ");
                else s.append((String)(a.jComboBox.getSelectedItem()) + " ");
            } else if (a.name.equals("additionalargs")) {
                s.append(a.jTextField.getText() + " ");
            } else {
                if (a.jCheckBox != null && a.jCheckBox.isSelected()) {
                    s.append("-" + a.name + " ");
                }
                else if (a.jTextField != null && !a.jTextField.getText().equals("")) {
                    s.append("-" + a.name + " " + a.jTextField.getText() + " ");
                }
                else if (a.jComboBox != null) {
                    s.append("-" + a.name + " " + (String)(a.jComboBox.getSelectedItem()) + " ");
                }
            }
        }
        final String ss = s.toString();
        caveGenThread = new Thread() {
            public void run() {
                try {
                    CaveGen.run(ss.split("[ ]+"));
                } catch (Exception e) {
                    e.printStackTrace();
                    reportBuffer.append("Crash log:\n");
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    CaveViewer.caveViewer.reportBuffer.append(errors.toString());
                }
            }
        };

        caveGenThread.start();
    }

}