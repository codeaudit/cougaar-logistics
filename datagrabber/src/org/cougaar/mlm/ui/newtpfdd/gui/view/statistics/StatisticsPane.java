/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.mlm.ui.newtpfdd.gui.view.statistics;

import org.cougaar.mlm.ui.newtpfdd.gui.view.NewTPFDDShell;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.validator.Validator;
import org.cougaar.mlm.ui.grabber.validator.Test;
import org.cougaar.mlm.ui.grabber.validator.HTMLizer;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.EditorKit;
import javax.swing.text.Document;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.*;

/**
 * Displays statistics information based on grabber Validation tests
 *
 * @since 5/3/01
 **/
public class StatisticsPane extends JPanel implements ActionListener{

  //Constants:
  ////////////

  public static final String NAME="Run Statistics";

  //Variables:
  ////////////

  protected HTMLizer logger=new HTMLizer(System.out);

  protected NewTPFDDShell shell;
  protected Validator validator;

  //  protected List testIndices;
  protected JComboBox choices;
  protected JEditorPane editor;
  protected Map choiceToTestID;
  protected JComponent scrollPane;
  
  //Constructors:
  ///////////////

  public StatisticsPane(NewTPFDDShell shell){
    this.shell=shell;
    setupGUI();
  }

  //Members:
  //////////

  protected void setupGUI(){
    setLayout(new BorderLayout());

    choices=new JComboBox();
	choiceToTestID = new HashMap ();
	int choicesAdded = 0;
	
	List testIndices;
	testIndices=getValidator().getTestIndicesForFailureLevel(Test.RESULT_INFO);
    for(int i=0;i<testIndices.size();i++){
	  Integer testID = (Integer) testIndices.get(i);
	  int intTestID = testID.intValue();
	  if (addTest (getValidator().getTest(intTestID))) {
		choices.addItem(getValidator().getDescription(intTestID));
		choiceToTestID.put (new Integer (choicesAdded++), testID);
	  }
    }
    choices.setActionCommand("displayPage");
    choices.addActionListener(this);
    add(choices,BorderLayout.NORTH);
	JComponent panel = createContent ();
    JScrollPane sp=new JScrollPane(panel);
	scrollPane = sp;
    add(sp,BorderLayout.CENTER);
  }
  
  protected boolean addTest (Test test) {	return true;  }
  
  protected JComponent createContent () {
    editor=new JEditorPane("text/html","<HTML><CENTER><H1>Choose a test</H1></CENTER></HTML>");
    editor.setEditable(false);
	return editor;
  }
  
  public void actionPerformed(ActionEvent e){
    if(e.getActionCommand().equals("displayPage")){
      int sel=choices.getSelectedIndex();
	  //      int idx=((Integer)testIndices.get(sel)).intValue();
	  int idx = ((Integer) choiceToTestID.get (new Integer (sel))).intValue();
	  
      try{
	Statement s=getStatement();
	if(getValidator().getTestResult(logger,s,getRunID(),idx)==
	   Test.RESULT_NOT_RUN){
	  setText("<HTML><CENTER><H1>Running test</H1></CENTER></HTML");
	  runTest(s,idx);
	}else{
	  setText("<HTML><CENTER><H1>Requesting data</H1></CENTER></HTML");
	  displayResult(s,idx);
	}
      }catch(SQLException ex){
	System.err.println(ex);
      }
    }
  }

  public void runTest(Statement s,int idx){
    final int fin_idx=idx;
    final int fin_runID=getRunID();
    final Statement fin_s=s;
    final SwingWorker worker = new SwingWorker(){
	int idx=fin_idx;
	int runID=fin_runID;
	Statement s=fin_s;
        public Object construct() {
	  getValidator().runTest(logger,shell.getDBConfig(),runID,idx);    
	  return null;
        }
        //Runs on the event-dispatching thread.
        public void finished(){
	  displayResult(s,idx);
        }
      };
    worker.start();  
  }

  public void displayResult(Statement s, int idx){
    final int fin_idx=idx;
    final int fin_runID=getRunID();
    final Statement fin_s=s;
    final EditorKit fin_kit=(EditorKit)editor.getEditorKit().clone();
    final SwingWorker worker = new SwingWorker(){
	int idx=fin_idx;
	int runID=fin_runID;
	Statement s=fin_s;
	EditorKit kit=fin_kit;

	Document document;
	
        public Object construct() {
	  ByteArrayOutputStream bos=new ByteArrayOutputStream();
	  PrintStream ps = new PrintStream(bos);
	  HTMLizer h=new HTMLizer(ps);
	  getValidator().displayTest(s,h,runID,idx);    
	  ps.close();
	  String doc="<HTML><CENTER>"+bos.toString()+"</CENTER></HTML>";
	  
	  document=kit.createDefaultDocument();
	  StringReader sr=new StringReader(doc);
	  try{
	    kit.read(sr,document,0);
	  }catch(Exception e){
	    System.err.println(e);
	  }
	  return doc;

        }
        //Runs on the event-dispatching thread.
        public void finished(){
	  setText("<HTML><CENTER><H1>Rendering Page</H1></CENTER></HTML>");
	  showPage(document);
        }
      };
    worker.start();  
  }

  protected void showPage(Document doc){
    final Document fin_document=doc;
    final SwingWorker worker = new SwingWorker(){
	Document document=fin_document;
        public Object construct(){
	  return null;
        }
        //Runs on the event-dispatching thread.
        public void finished(){
	  editor.setDocument(document);
        }
      };
    worker.start();      
  }

  protected void setText(String text){
    editor.setText(text);
  }

  protected DBConfig getDBConfig(DatabaseConfig d){
	return d.getDBConfig ();
  }
  
  protected Validator getValidator(){
    if(validator==null){
      DatabaseConfig d=shell.getDBConfig();
      validator=new Validator(getDBConfig(d));
    }
    return validator;
  }

  protected Statement getStatement() throws SQLException{
    return shell.getDBConfig().getConnection().createStatement();
  }

  protected int getRunID(){
    return shell.getRun().getRunID();
  }

  //InnerClasses:
  ///////////////
}
