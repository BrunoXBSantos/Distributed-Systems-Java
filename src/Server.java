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
import java.util.concurrent.locks.Condition;



class ClientHandler implements Runnable{
	public static String CSI = "\u001B[";
	private Socket socket;  // o socket devolvido por accept
	private DataBaseServer dataBaseServer;
	private ReceiveCasesList receiveCasesList;

	public ClientHandler(Socket socket, DataBaseServer dataBaseServer, ReceiveCasesList receiveCasesList){
		this.socket=socket;
		this.dataBaseServer = dataBaseServer;
		this.receiveCasesList = receiveCasesList;
	} 

	public void run(){
		try{

		boolean conected = true;
		BufferedReader in = new BufferedReader(
			new InputStreamReader(this.socket.getInputStream()));
		boolean autoFlush = true;
		PrintWriter out = new PrintWriter(this.socket.getOutputStream(),autoFlush);

		// Pergunta pelo nome e pela pass
		String authQ = "\tInsira autenticacao (username password): ";
		out.println(authQ);

		// recebe o username e a pass
		String authR = in.readLine();

		StringTokenizer st = new StringTokenizer(authR, " ");
		String name = st.nextToken();
		String password = st.nextToken();

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

		/*

boolean flag = false;
			do{
				try{
					int opcao = verifyNumberOption(msg);
				}
				catch(NumberFormatException e){
					System.out.println(e.getMessage());
					
				}
				catch(NumberInvalidException e){
					System.out.println(e.getMessage());
				}
			}
			while(!flag);
		*/


		else{
			out.println("Utilizador nao registado. (1 - Registar) (0 -  Sair)");
			String msg = in.readLine();
			boolean flagR = false;
			int opcao = 0;
			do{
				try{
						opcao = Integer.parseInt(msg);
						flagR = true;
				}
				catch(NumberFormatException e){
					out.println("Opcao tem que ser um numero inteiro. Introduza de novo: ");
					msg = in.readLine();
				}
			}	
			while(!flagR);
			if(opcao == 1){
				this.dataBaseServer.createClient(name, password);
				out.println("Autenticacao realizada com sucesso");
			}
			else{
				out.println("Obrigado!");
				conected = false;
			}
		}

		System.out.print (CSI + "32" + "m");
  	 	System.out.print ("\t" + name.toUpperCase() + " autenticado com sucesso!\n");
    	System.out.println (CSI + "m");

		while(conected){
			String msg = in.readLine();
			if(msg == null || msg.equals("quit")){
				conected = false;
			}
			else{
				int cases = 0;
				boolean flagC = false;
				do{
					try{
						cases = Util.verifyCases(msg,this.dataBaseServer.getContacts(name));
						flagC = true;
					}
					catch(NumberFormatException e){
						out.println(e.getMessage());
						msg = in.readLine();
					}
					catch(NumberInvalidException e){
						out.println(e.getMessage());
						msg = in.readLine();
					}	
				}
				while(!flagC);

				this.dataBaseServer.setReportedCases(name, Integer.parseInt(msg));
				float avgProportion = (float) this.dataBaseServer.avgProportion();
				String avgProportionS = String.format("%.2f",avgProportion * 100);
				String msgSend = new String("Average Proportion Atual: " + avgProportion + " -> " + avgProportionS + "%");
				this.receiveCasesList.putCases(msgSend);
				
				System.out.print (CSI + "38" + "m");
				System.out.print("\t" + name + ": " + msg + "/150");
    			System.out.println (CSI + "m");
    			System.out.print (CSI + "31" + "m");
  	 			System.out.print ("\t" + msgSend + "\n");
    			System.out.println (CSI + "m");
			}
		}

		System.out.print (CSI + "31" + "m");		
		System.out.println("\n\t" + name.toUpperCase() + " Disconnected");
		System.out.println (CSI + "m");	
	
		this.socket.shutdownInput();
		this.socket.shutdownOutput();
		this.socket.close();

		}

		catch(Exception e){

		}
	}
}





// Contem as mensagens recebidas pelos clientes e os clientes ligados
class ReceiveCasesList{

	private int connectedClients;    // lista de threads ligadas
	private LinkedList<String> list;  // lista de casos recebidos pelo servidor
	private int count;
	private ReentrantLock lock;
	private Condition isEmptyCondition;  // se nao haver msgs para ler
	private Condition notReadClient;	// se todas as threads nao recebem a msg

	public ReceiveCasesList(){
		this.list = new LinkedList<>();
		this.connectedClients = 0;
		this.count = 0;
		this.lock = new ReentrantLock();
		this.isEmptyCondition = this.lock.newCondition();
		this.notReadClient = this.lock.newCondition();
	}

	public void putCases(String cases){
		this.lock.lock();
		this.list.add(cases);
		this.isEmptyCondition.signalAll();
		this.lock.unlock();
	}

	public String getCases(){
		this.lock.lock();
		String cases = null;
		try{
			while(this.list.size() == 0){
				isEmptyCondition.await();
			}

			cases = this.list.getFirst();
			if(count < connectedClients - 1){
				this.count++;
				this.notReadClient.await();
			}
			else{
				this.notReadClient.signalAll();
				this.list.removeFirst();
			}

			this.count = 0;

		}
		catch(Exception e){}
		this.lock.unlock();

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
	private Thread clientThread;	// thread com a comunicacao com o client
	private ReceiveCasesList receiveCasesList;   // Lista de mensagens a enviar

	public SendProportionHandler(Socket socket, Thread clientThread, ReceiveCasesList receiveCasesList){
		this.socket = socket;
		this.clientThread = clientThread;
		this.receiveCasesList = receiveCasesList;
	}

	public void run(){
		//Só envia para os clientes
		try{
			PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
			boolean flag = true;
			while(flag){
				String msg = this.receiveCasesList.getCases();
				out.println(msg);
				if(!this.clientThread.isAlive()){
					flag = false;
					this.receiveCasesList.decrementThreads();
				}
			}			
		}
		catch(Exception e){}
	}

}	

class Server{
	public static void main(String[] args){

		DataBaseServer dataBaseServer = new DataBaseServer("Server");
		ReceiveCasesList receiveCasesList  = new ReceiveCasesList(); // recebe casos dos clientes
		String CSI = "\u001B[";   // cores no terminal
		ServerSocket listener = null;
		int port = 3001;
		// verificacao do numero de parametros de args
		// É necessario a introduzao do numero da porta
		if(args.length != 1){
			System.out.println("Error! Necessário introduzir o numero da porta.");
			System.exit(1);
		}

		try{
			port = Util.verifyPort(args[0]);  // string para int do num da porta
		}
		catch(NumberPortInvalidException e){
			System.out.println("Error! " + e.getMessage());
			System.exit(1);
		}
		catch(NumberFormatException e){
			System.out.println("Error! " + e.getMessage());
			System.exit(1);
		}

		try{
			listener = new ServerSocket(port);  // socket, bind e listener
		}
		catch(IOException e){
			System.out.println("Error creating the socket!");
			System.exit(1);
		}


		System.out.print (CSI + "44" + "m");
		System.out.println("\n\tServer initialized: " + listener.getLocalSocketAddress());
    	System.out.print(CSI + "m");
		System.out.print("\n");

		boolean flag = true;

		while(flag){
			try{
				Socket socket = listener.accept();
				System.out.print (CSI + "32" + "m");
				System.out.println("\tNew connection");
				System.out.print(CSI + "m");

				// criacao das threads
				ClientHandler client = new ClientHandler(socket, dataBaseServer, receiveCasesList);
				Thread clientThread = new Thread(client);
				SendProportionHandler sendProportionHandler = new SendProportionHandler(socket, clientThread, receiveCasesList);
				Thread sendProportionThread = new Thread(sendProportionHandler);

				//inicio
				clientThread.start();
				sendProportionThread.start();

				receiveCasesList.incrementThreads(); // incremento o numero de utilizadores ligados
				
				if("quit".equals("or")){
					flag = false;
				}
			}	
			catch(IOException e){
				System.out.println("Error accepting the connection");
			}	
		}

		try{
			listener.close();
		}
		catch(IOException e){
			System.out.println("Error closing listener");
			System.exit(1);
		}		
	}


}



class NumberPortInvalidException extends Exception{
	NumberPortInvalidException(String s){
		super(s);
	}
}
class NumberInvalidException extends Exception{
	NumberInvalidException(String s){
		super(s);
	}
}

class Util{
	public static int verifyPort(String s) throws NumberFormatException, NumberPortInvalidException{
		try{
			int n = Integer.parseInt(s);
			if(n<3000){
				throw new NumberPortInvalidException("Numero da porta Tem de ser maior que 3000");
			}		
			else
				return n;
		}
		catch(NumberFormatException e){
			throw new NumberFormatException("Porta tem de ser um número inteiro");
		}
		
	}

	public static int verifyNumberOption(String s) throws NumberFormatException, NumberInvalidException {
		int n = 0;
		try{
			n = Integer.parseInt(s);
			if(n == 1 || n == 0){
				return n;
			}
			else{
				throw new NumberInvalidException("Opcao tem de ser 0 ou 1");
			}
		}
		catch(NumberFormatException e){
			throw new NumberFormatException("Numero tem de ser um inteiro");
		}
	}

	public static int verifyCases(String msg, int contacts) throws NumberFormatException, NumberInvalidException{
		int cases = 0;
		try{
			cases = Integer.parseInt(msg);
			if(cases >= 0 && cases <= contacts){
				return cases;
			}
			else if(cases < 0 )
				throw new NumberInvalidException("Número de casos não pode ser negativo!");
			else if(cases > contacts)
				throw new NumberInvalidException("Número de casos não pode ser superior aos contactos!");
			else{
				throw new NumberInvalidException("Erro no numero de casos");
			}
		}
		catch(NumberFormatException e){
			throw new NumberFormatException("Numero tem de ser um inteiro");
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