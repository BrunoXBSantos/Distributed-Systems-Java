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

class DataBaseServer{

	private HashMap<String, ClientData> clientsList;  // lista de clientes
	private String serverName;
	private int lastId; 
	private ReentrantLock dataBaseServer;


	public DataBaseServer(String serverName){
		this.serverName = serverName;
		this.clientsList = new HashMap<>();
		this.dataBaseServer = new ReentrantLock();
		this.lastId = 0;
	}

	// Verificar se Existe cliente
	public boolean existClient(String name){
		boolean flag;
		this.dataBaseServer.lock();
		flag = this.clientsList.containsKey(name);
		this.dataBaseServer.unlock();
		return flag;
	}

	// ver o numero de contactos
	public int getContacts(String name){
		this.dataBaseServer.lock();
		if(this.clientsList.containsKey(name)){
			int n = this.clientsList.get(name).getContacts();
			this.dataBaseServer.unlock();
			return n;  // cliente criado
		}
		else{
			this.dataBaseServer.unlock();
			return -1;  // cliente já existe
		}		
	}

	// cria um cliente, recebendo o nome e a pass
	public boolean createClient(String name, String pass){
		this.dataBaseServer.lock();
		if(!this.clientsList.containsKey(name)){
			ClientData clientData = new ClientData(pass, ++lastId);
			this.clientsList.put(name,clientData);
			this.dataBaseServer.unlock();
			return true;  // cliente criado
		}
		else{
			this.dataBaseServer.unlock();
			return false;  // cliente já existe
		}
	}

	// VERIFICA A PASSWORD
	public boolean checkPassword(String name, String pass){
		this.dataBaseServer.lock();
		if(this.clientsList.containsKey(name)){
			ClientData clientData = this.clientsList.get(name);
			clientData.lock();
			this.dataBaseServer.unlock();
			boolean flag = pass.equals(clientData.getPassword());
			clientData.unlock();
			if(flag){
				return true; // pass faz match
			}
			else{
				return false; // pass nao corresponde
			}
		}
		else{
			this.dataBaseServer.unlock();
			return false;
		}

	}

	// Obtem a proporcao media dos casos
	public double avgProportion(){
		double total = 0.0;

		this.dataBaseServer.lock();

	    // after locking the dataBaseServer we know that no account can be created or deleted, and thus `locked` is all existing accounts
		Set<String> locked = this.clientsList.keySet();

		//lock all accounts
		for(String name: locked){
			this.clientsList.get(name).lock();
		}

		this.dataBaseServer.unlock();

		// calcula a proporcao total
		for(String name: locked){
			ClientData clientData = this.clientsList.get(name);
			total += clientData.getProportion();
			clientData.unlock();
		}
		return total / this.clientsList.size();
	}

	// total de casos reportados
	public int totalCasesReported(){

		this.dataBaseServer.lock();
	    // after locking the dataBaseServer we know that no account can be created or deleted, and thus `locked` is all existing accounts
		Set<String> locked = this.clientsList.keySet();

		// lock all accounts
		for(String name : locked){
			this.clientsList.get(name).lock();
		}

		this.dataBaseServer.unlock(); 
		// all accounts are locked: we can release the bank lock

		// compute the total cases
		int total = 0;
		for(String name : locked){
			ClientData clientData = this.clientsList.get(name);
			total += clientData.getReportedCases();
			clientData.unlock();
		}

		return total;
	}

	// proporcao de casos
	public double proportionCasesReported(){

		this.dataBaseServer.lock();
	    // after locking the dataBaseServer we know that no account can be created or deleted, and thus `locked` is all existing accounts
		Set<String> locked = this.clientsList.keySet();

		// lock all accounts
		for(String name : locked){
			this.clientsList.get(name).lock();
		}

		this.dataBaseServer.unlock(); 
		// all accounts are locked: we can release the bank lock

		// compute the total cases
		int totalCases = 0;
		int totalContacts = 0;
		for(String name : locked){
			ClientData clientData = this.clientsList.get(name);
			totalCases += clientData.getReportedCases();
			totalContacts += clientData.getContacts();
			clientData.unlock();
		}

		return (double) totalCases / totalContacts;
	}


	// modificar casos reportados de um cliente
	public boolean setReportedCases(String name, int newCases){
		this.dataBaseServer.lock();
		if(this.clientsList.containsKey(name)){
			ClientData clientData = this.clientsList.get(name);
			clientData.lock();
			this.dataBaseServer.unlock();
			clientData.setReportedCases(newCases);
			clientData.unlock();
			return true;
		}
		else{
			this.dataBaseServer.unlock();
			return false;
		}
	}	

}
