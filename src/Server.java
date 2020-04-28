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
	private Socket socket;  // o socket devolvido por accept
	private DataBaseServer dataBaseServer;
	private ReceiveCasesList receiveCasesList;

	public ClientHandler(Socket socket, DataBaseServer dataBaseServer, ReceiveCasesList receiveCasesList){
		this.socket=socket;
		this.dataBaseServer = dataBaseServer;
		this.receiveCasesList = receiveCasesList;
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
					this.dataBaseServer.setReportedCases(name, Integer.parseInt(msg));
					System.out.println("Debug1");
					double avgProportion = this.dataBaseServer.avgProportion();
					String msgSend = new String("Average Proportion Atual: " + avgProportion);
					this.receiveCasesList.putCases(msgSend);
					System.out.println("casos: " + msg + " name: " + name);
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
			System.out.println("debug");
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
	private Thread clientHandlerThread;	// thread com a comunicacao com o client
	private ReceiveCasesList receiveCasesList;   // Lista de mensagens a enviar

	public SendProportionHandler(Socket socket, Thread clientHandlerThread, ReceiveCasesList receiveCasesList){
		this.socket = socket;
		this.clientHandlerThread = clientHandlerThread;
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
				if(!this.clientHandlerThread.isAlive()){
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
				ClientHandler client = new ClientHandler(socket, dataBaseServer, receiveCasesList);
				Thread clientHandlerThread = new Thread(client);
				SendProportionHandler sendProportionHandler = new SendProportionHandler(socket, clientHandlerThread, receiveCasesList);
				Thread sendProportionThread = new Thread(sendProportionHandler);

				//inicio
				clientHandlerThread.start();
				sendProportionThread.start();

				receiveCasesList.incrementThreads();
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