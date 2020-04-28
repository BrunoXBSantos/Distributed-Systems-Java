import java.util.Calendar; 
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Set;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.StringTokenizer;
import java.util.LinkedList;

class ClientData{
	private String password;	// password
	private int id;			// id do client
	private int contacts;   // contactos que conhece
	private int reportedCases;	// casos reportados dos contactos
	private Calendar registerDate;		// data de registo
	private ReentrantLock lock;

	public ClientData(String password, int id){
		this.password=password;
		this.id=id;
		this.contacts = 150;
    this.reportedCases = 0;

		this.registerDate=new GregorianCalendar(); // vai buscar a data atual
		this.lock = new ReentrantLock();
	}

	// devolve a password
	public String getPassword(){
		return this.password;
	}

	public Calendar getRegisterDate(){
		return this.registerDate;   // this.registerDate.clone();
  	}

  	// numero de contactos
  	public int getContacts(){
  		return this.contacts;
  	}

  	// modifica o numero de casos reportados
  	public void setReportedCases(int reportCases){
  		this.reportedCases = reportCases;
  	}

    // devolve o num de casos reportados
    public int getReportedCases(){
      return this.reportedCases;
    }

    // Obtem a proporcao media
    public double getProportion(){
      System.out.println(this.getReportedCases());
      return ((double) this.reportedCases / this.contacts);
    }

  	// adicionar casos reportados
  	public void addReportCases(int cases){
  		this.reportedCases += cases;
  	}

  	public void lock(){
  		this.lock.lock();
  	}

  	public void unlock(){
  		this.lock.unlock();
  	}

}