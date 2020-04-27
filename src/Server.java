import java.util.Calendar; 
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.StringTokenizer;



class ClientData{
	private String password;
	private int id;
	private Calendar registerDate;
	private ReentrantLock lock;

	public ClientData(String password, int id){
		this.password=password;
		this.id=id;
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

}

class ClientHandler implements Runnable{
	private Socket socket;  // o socket devolvido por accept
	private DataBaseServer dataBaseServer;

	public ClientHandler(Socket socket, DataBaseServer dataBaseServer){
		this.socket=socket;
		this.dataBaseServer = dataBaseServer;
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
				if(msg == null){
					conected = false;
				}
				else{
					System.out.println("receive msg: " + msg);
					out.println(msg.toUpperCase());
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






class Server{
	public static void main(String[] args){

		DataBaseServer dataBaseServer = new DataBaseServer("Server");

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

				ClientHandler client = new ClientHandler(socket, dataBaseServer);
				Thread t = new Thread(client);
				t.start();
				
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