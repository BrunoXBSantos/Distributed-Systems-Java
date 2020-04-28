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
		this.registerDate=new GregorianCalendar(); // vai buscar a data atual
		this.lock = new ReentrantLock();
		this.reportedCases = 0;
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

class ClientHandler implements Runnable{
	private Socket socket;  // o socket devolvido por accept
	private DataBaseServer dataBaseServer;
	private ReceiveCasesClient receiveCasesClient;

	public ClientHandler(Socket socket, DataBaseServer dataBaseServer, ReceiveCasesClient receiveCasesClient){
		this.socket=socket;
		this.dataBaseServer = dataBaseServer;
		this.receiveCasesClient = receiveCasesClient;
	} 

	public void run(){
		boolean conected = true;
		try{
			BufferedReader in = new BufferedReader(
				new InputStreamReader(this.socket.getInputStream()));
			boolean autoFlush = true;
			PrintWriter out = new PrintWriter(this.socket.getOutputStream(),autoFlush);

			// Pergunta pelo nome e pela pass
			String authQ = "Insira (username password): ";
			out.println(authQ);

			// recebe o username e a pass
			String authR = in.readLine();

			StringTokenizer st = new StringTokenizer(authR, " ");
			String name = st.nextToken();
			String password = st.nextToken();

			System.out.println("name: "+name + " pass: " + password);
			
			boolean existClient = this.dataBaseServer.existClient(name);
			if(existClient){
				// verifica a pass
				boolean checkPassword = this.dataBaseServer.checkPassword(name, password);
				if(checkPassword){
					// pass correta
					out.println("Autenticacao realizada com sucesso");
				}
				else{
					out.println("Password errada! Tente novamente.");
					conected = false;

				}
			}
			else{
				out.println("Utilizador nao registado. (1 - Registar) (0 -  Sair)");
				String msg = in.readLine();
				int opcao = Integer.parseInt(msg);
				if(opcao == 1){
					this.dataBaseServer.createClient(name, password);
					out.println("Autenticacao realizada com sucesso");
				}
				else{
					out.println("Obrigado!");
					conected = false;
				}
			}	

			while(conected){
				String msg = in.readLine();
				if(msg == null || msg.equals("quit")){
					conected = false;
				}
				else{
					this.receiveCasesClient.putCases(msg);
					System.out.println("casos: " + msg + " name: " + name);
					this.dataBaseServer.setReportedCases(name, Integer.parseInt(msg));
				}
			}		
			System.out.println("Client Disconnected");	
		
			this.socket.shutdownInput();
			this.socket.shutdownOutput();
			this.socket.close();
		}	
		catch(IOException e){
			conected = false;
			e.printStackTrace();
		}
	}
}





// Contem as mensagens recebidas pelos clientes e os clientes ligados
class ReceiveCasesClient{

	private int connectedClients;    // lista de threads ligadas
	private LinkedList<String> list;  // lista de casos recebidos pelo servidor
	private int count;

	public ReceiveCasesClient(){
		this.list = new LinkedList<>();
		this.connectedClients = 0;
		this.count = 0;
	}

	public synchronized void putCases(String cases){
		this.list.add(cases);

		notifyAll();
	}

	public synchronized String getCases(){
		String cases = null;
		try{
			while(this.list.size() == 0){
				wait();
			}

			cases = this.list.getFirst();
			System.out.println("debug");
			if(count < connectedClients - 1){
				this.count++;
				wait();
			}
			else{
				notifyAll();
				this.list.removeFirst();
			}

			this.count = 0;

		}
		catch(Exception e){}
		return cases;
		
	}
	
	public void decrementThreads(){
		this.connectedClients--;
	}

	public void incrementThreads(){
		this.connectedClients++;
	}
}

class SendProportionHandler implements Runnable{
	private Socket socket;    // socket
	private Thread clientHandlerThread;	// thread com a comunicacao com o client
	private ReceiveCasesClient receiveCasesClient;   // Lista de mensagens a enviar

	public SendProportionHandler(Socket socket, Thread clientHandlerThread, ReceiveCasesClient receiveCasesClient){
		this.socket = socket;
		this.clientHandlerThread = clientHandlerThread;
		this.receiveCasesClient = receiveCasesClient;
	}

	public void run(){
		//Só envia para os clientes
		try{
			PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
			boolean flag = true;
			while(flag){
				String msg = this.receiveCasesClient.getCases();
				out.println(msg);
				if(!this.clientHandlerThread.isAlive()){
					flag = false;
					this.receiveCasesClient.decrementThreads();
				}
			}			
		}
		catch(Exception e){}
	}

}	

class Server{
	public static void main(String[] args){

		DataBaseServer dataBaseServer = new DataBaseServer("Server");
		ReceiveCasesClient receiveCasesClient  = new ReceiveCasesClient();

		try{
			int port;
			// verificacao do numero de parametros de args
			// É necessario a introduzao do numero da porta
			if(args.length != 1){
				System.out.println("Error! Necessário introduzir o numero da porta.");
				System.exit(1);
			}

			
			port = Util.parseInt(args[0]);  // string para int do num da porta

			ServerSocket listener = new ServerSocket(port);  // socket, bind e listener


			while(true){
				Socket socket = listener.accept();
				System.out.println("new connection");

				// criacao das threads
				ClientHandler client = new ClientHandler(socket, dataBaseServer, receiveCasesClient);
				Thread clientHandlerThread = new Thread(client);
				SendProportionHandler sendProportionHandler = new SendProportionHandler(socket, clientHandlerThread, receiveCasesClient);
				Thread sendProportionThread = new Thread(sendProportionHandler);

				//inicio
				clientHandlerThread.start();
				sendProportionThread.start();

				receiveCasesClient.incrementThreads();
			}	
		}
		catch(NumberFormatException e){
			System.out.println(e.getMessage());
		}
		catch(NumberPortInvalidException e){
			System.out.println(e.getMessage());
		}
		catch(IOException e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}


}



class NumberPortInvalidException extends Exception{
	NumberPortInvalidException(String s){
		super(s);
	}
}

class Util{
	public static int parseInt(String s) throws NumberFormatException, NumberPortInvalidException{
		try{
			int n = Integer.parseInt(s);
			if(n<3000){
				throw new NumberPortInvalidException("Numero da porta maior que 1000");
			}		
			else
				return n;
		}
		catch(NumberFormatException e){
			throw new NumberFormatException("Porta tem de ser um número inteiro");
		}
		
	}
}

// primeira mensagem do cliente é a autenticacao?
// depois o cliente comunica quantos casos ..
// o servidor envia a todos os clientes liagados uma nova estimativa



/*

class DataBaseConnected{
	private HashMap<String,Socket> clientsConnectedList;
	private ReentrantLock dataBaseConnected;

	public DataBaseConnected(){
		this.clientsConnectedList = new HashMap<>();
		this.dataBaseConnected = new ReentrantLock();
	}

	// adicionar um cliente à lista de clientes ligados
	public boolean addClientConnected(String name, Socket socket){
		this.dataBaseConnected.lock();
		if(!this.clientsConnectedList.containsKey(name)){
			this.clientsConnectedList.put(name,socket);
			this.dataBaseConnected.unlock();
			return true;
		}
		else{
			this.dataBaseConnected.unlock();
			return false;
		}
	}


	// remover um cliente da lista de clientes ligados
	public boolean removeClientConnected(String name){
		this.dataBaseConnected.lock();
		if(this.clientsConnectedList.containsKey(name)){
			this.clientsConnectedList.remove(name);
			this.dataBaseConnected.unlock();
			return true;
		}
		else{
			this.dataBaseConnected.unlock();
			return false;
		}
	}
}

*/