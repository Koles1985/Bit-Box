/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bit.box;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.*;
import java.lang.Exception;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Koles
 */
public class BitBoxWindow {
    
    
    private ArrayList<JCheckBox> checkList;
    private Sequencer seqr;
    private Sequence seqe;
    private Track track;
    private JPanel mainPanel;
    private JFrame window;
    
    
    // Названия инструментов
    private String[] instrumentNames = {"Bass Drum", "Closed Hi-hat",
        "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand clup", 
        "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", 
        "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
    // Эти числа представляют собой фактические барабанные клавишы для канала Барабан
    int[] instrument = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58,
                            47, 67, 63};

    protected void buildGUI(){
        window = new JFrame("Cyber BitBox");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        
        
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        //пустая граница позволяет создать поля между полями
        // и местом размещения компонентов
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        checkList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);
        
        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);
        
        JButton upTempo = new JButton("Up Tempo");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);
        
        JButton downTempo = new JButton("Down Tempo");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);
        
        JButton serialize = new JButton("Serialize It");
        serialize.addActionListener(new MySendListener());
        buttonBox.add(serialize);
        
        JButton restore = new JButton("Restore");
        restore.addActionListener(new MyReadInListener());
        buttonBox.add(restore);
        
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for(int i = 0; i < instrumentNames.length; i++){
            nameBox.add(new JLabel(instrumentNames[i]));
        }
        
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);
        
        window.getContentPane().add(background);
        
        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);
        
        for(int i = 0; i < 256; i++){
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkList.add(c);
            mainPanel.add(c);
        }
        
        setUpMidi();
        
        window.setBounds(50, 50, 300, 300);
        window.pack();
        window.setVisible(true);
    }
    
    private void setUpMidi(){
        try{
            seqr = MidiSystem.getSequencer();
            seqr.open();
            seqe = new Sequence(Sequence.PPQ, 4);
            track = seqe.createTrack();
            seqr.setTempoInBPM(120);
        } catch(Exception e){
                e.printStackTrace();
            }
    }
    
    
    public void buildTrackAndStart(){
        /*
        Сщздаем массив чтобы хранить значения для каждого инструмента
        на все 16 тактов.
        */
        int[] trackList = null;
        /*
        избавляемся от старой дорожки и добавляем новую
        */
        seqe.deleteTrack(track);
        track = seqe.createTrack();
        
        for(int i = 0; i < 16; i++){
            trackList = new int[16];
            /*
            Задаем клавишу которая представляет инструмент мфссив
            содержит числа для каждого инструмента
            */
            int key = instrument[i];
            
            //делаем это для каждого такта текущего ряда
            for(int j = 0; j < 16; j++){
            JCheckBox jc = (JCheckBox) checkList.get(j + (16*i));
            if(jc.isSelected()){
                trackList[j] = key;
            }else{
                trackList[j] = 0;
            }
        }
            makeTrack(trackList);
            track.add(makeEvent(176,1,127,0,16));
        }
        /*
        Мы всегда должны быть уверены что событие на такте 16
        существует(они идут лт 0 до 15) иначе BitBox может не пройти все 16 тактов
        перед тем как заново начнет последовательность
        */
        track.add(makeEvent(192, 9, 1, 0, 15));
        try{
            seqr.setSequence(seqe);
        /*
        Позволяет количество повторений цыкла или как в этом случае 
        непрерывный цыкл
        */
        seqr.setLoopCount(seqr.LOOP_CONTINUOUSLY);
        seqr.start();
        seqr.setTempoInBPM(120);
        }catch(Exception e){
            e.printStackTrace();
        }  
    }
    
    public class MyStartListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            buildTrackAndStart();
        }
        
    }
    
    public class MyStopListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            seqr.stop();
        }
        
    }
    
    
    public class MyUpTempoListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
           float tempoFactor = seqr.getTempoFactor();
           seqr.setTempoFactor((float)(tempoFactor * 1.03));
        }
        
    }
    
    public class MyDownTempoListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = seqr.getTempoFactor();
            seqr.setTempoFactor((float) (tempoFactor * .97));
        }
        
    }
    
    
    
    
    /*
    метод создает события для одного инструмента за каждый проход цыкла для всех
    16 тактов можно полкчить int[] для Bass drumm и каждый элемент массива будет
    содержать либо клавишу этого инструмента либо 0, если это 0 то инструмент не
    должен играть на текущем такте иначе нужно создать событие и добавить его в 
    дорожку
    */
    
    public void makeTrack(int[] list){
        for(int i = 0; i < 16; i++){
            int key = list[i];
            if(key != 0){
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }
    
    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tic){
        MidiEvent event = null;
        try{
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tic);
        }catch(Exception e){
            e.printStackTrace();
        }
        return event;
    }
    
    private class MySendListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean[] checkBoxState = new boolean[256];
            for(int i = 0; i < 256; i++ ){
                JCheckBox check = (JCheckBox)checkList.get(i);
                if(check.isSelected()){
                    checkBoxState[i] = true;
                }
            }
            try{
                FileOutputStream fos = new FileOutputStream(new File("CheckBox.ser"));
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(checkBoxState);
                
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
        
    }
    
    private class MyReadInListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean[] checkBoxState = null;
            try{
                FileInputStream fis = new FileInputStream(new File("CheckBox.ser"));
                ObjectInputStream ois = new ObjectInputStream(fis);
                try {
                    checkBoxState = (boolean[])ois.readObject();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                    System.out.println("ClassNotFoundException in MyReadListener");
                }
            }catch(IOException ex){
                ex.printStackTrace();
            }  
            
            for(int i = 0; i < 256; i++){
                JCheckBox check = (JCheckBox)checkList.get(i);
                if(checkBoxState[i]){
                    check.setSelected(true);
                }
                else{
                    check.setSelected(false);
                }
                seqr.stop();
                buildTrackAndStart();
            }
    }
  }
}

